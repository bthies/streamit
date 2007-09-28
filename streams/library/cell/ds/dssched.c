#include "ds.h"

/*

filter's schedulable iteration count is computed based on upstream channel's
used bytes and downstream free bytes

when filter is scheduled/extended: upstream used and downstream free are
decremented immediately - this does not affect other filters

running filter increments upstream free and downstream used as iters are
reported to have completed

duplicate splitter:

for filter upstream: needs to decrement downstream free as normal, does not
need to increment direct downstream used but must propagate to duplicates

for filters downstream: needs to decrement upstream used as normal, does not
need to increment direct upstream free but must propagate to parent

for channel at input: used_bytes is not directly used, buffer is for output
only (input_only, head doesn't matter)

for channels at output: free_bytes is not directly used, buffer is for input
only (output_only, tail doesn't matter)

directly connected duplicate splitters should work but really can be eliminated
in the compiler

*/

void
update_filter_done(FILTER *f)
{
  if ((f->incomplete_inputs == 0) && (f->input_avail_iters == 0)) {
    incomplete_filters--;

    for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
      update_down_filter_incomplete(f->outputs[i]);
    }
  }
}

void
update_down_filter_incomplete(CHANNEL *c)
{
  if (c->duplicates == NULL) {
    FILTER *f = c->input.f;

    if (f != NULL) {
      f->incomplete_inputs--;

      if (f->active_count == 0) {
        update_filter_done(f);
      }
    }
  } else {
    for (uint32_t i = 0; i < c->num_duplicates; i++) {
      update_down_filter_incomplete(&c->duplicates[i]);
    }
  }
}

void
update_down_channel_used(CHANNEL *c, uint32_t num_bytes)
{
  if (c->duplicates == NULL) {
    c->used_bytes += num_bytes;

#if CHECK
    if (c->parent != NULL) {
      buf_inc_tail(&c->buf, num_bytes);
    }
#endif

    if (c->input.f != NULL) {
      FILTER *f = c->input.f;

      // this should be fine, replace with tree if necessary
      if (f->desc.num_inputs == 1) {
        uint32_t avail_iters = (uint32_t)max_int32
          (0, (int32_t)c->used_bytes - (int32_t)c->input.peek_extra_bytes) /
          c->input.pop_bytes;

        check(avail_iters >= f->input_avail_iters);
        f->input_avail_iters = avail_iters;
        f->avail_iters = min_uint32(avail_iters, f->output_avail_iters);
      } else {
        // this won't be called unnecessarily (e.g. twice by
        // update_input_channels) as long as no pair of filters is connected by
        // multiple channels
        update_filter_input(f);
      }
    }
  } else {
    for (uint32_t i = 0; i < c->num_duplicates; i++) {
      update_down_channel_used(&c->duplicates[i], num_bytes);
    }
  }
}

void
update_up_channel_free(CHANNEL *c, uint32_t num_bytes)
{
  CHANNEL *parent;

  // go up until we hit a channel that's not a duplicate
  while ((parent = c->parent) != NULL) {
    uint32_t old_free_bytes = c->free_bytes;

    c->free_bytes += num_bytes;

    if ((old_free_bytes != 0) || (--parent->free_waiting != 0)) {
      return;
    }

    // linear array should be fine unless the bottleneck channel is being
    // continuously updated
    num_bytes = parent->duplicates[0].free_bytes;

    for (uint32_t i = 1; i < parent->num_duplicates; i++) {
      num_bytes = min_uint32(num_bytes, parent->duplicates[i].free_bytes);
    }

    for (uint32_t i = 0; i < parent->num_duplicates; i++) {
      parent->duplicates[i].free_bytes -= num_bytes;
      parent->free_waiting += (parent->duplicates[i].free_bytes == 0);
    }

    c = parent;
  }

  c->free_bytes += num_bytes;

#if CHECK
  if (c->duplicates != NULL) {
    buf_inc_head(&c->buf, num_bytes);
  }
#endif

  if (c->output.f != NULL) {
    FILTER *f = c->output.f;

    if (f->desc.num_outputs == 1) {
      uint32_t avail_iters = c->free_bytes / c->output.push_bytes;

      check(avail_iters >= f->output_avail_iters);
      f->output_avail_iters = avail_iters;
      f->avail_iters = min_uint32(f->input_avail_iters, avail_iters);
    } else {
      update_filter_output(f);
    }
  }
}

void
update_filter_input(FILTER *f)
{
  if (f->desc.num_inputs != 0) {
    int32_t min_iters = INT32_MAX;

    for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
      CHANNEL *c = f->inputs[i];
      min_iters = min_int32(min_iters,
        ((int32_t)c->used_bytes - (int32_t)c->input.peek_extra_bytes) /
        (int32_t)c->input.pop_bytes);
    }

    min_iters = max_int32(0, min_iters);
    f->input_avail_iters = min_iters;
    f->avail_iters = min_uint32(min_iters, f->output_avail_iters);
  }
}

void
update_filter_output(FILTER *f)
{
  if (f->desc.num_outputs != 0) {
    uint32_t min_iters = UINT32_MAX;

    for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
      CHANNEL *c = f->outputs[i];
      min_iters = min_uint32(min_iters, c->free_bytes / c->output.push_bytes);
    }

    f->output_avail_iters = min_iters;
    f->avail_iters = min_uint32(f->input_avail_iters, min_iters);
  }
}

void
update_all_filters()
{
  for (uint32_t i = 0; i < num_filters; i++) {
    update_filter_input(&filters[i]);
    update_filter_output(&filters[i]);
  }
}

uint32_t
get_schedule_iters(FILTER *f, bool_t current)
{
  uint32_t min_iters = 3 * f->group_iters;
  uint32_t max_iters = 2 * min_iters;

  if ((f->incomplete_inputs == 0) &&
      (f->avail_iters == f->input_avail_iters)) {
    return min_uint32(f->avail_iters, max_iters);
  } else {
    if (current) {
      min_iters = 2 * f->group_iters;
    }

    if (f->avail_iters <= min_iters) {
      return f->avail_iters;
    }
    if (f->avail_iters >= max_iters) {
      return max_iters;
    }

    return f->avail_iters - f->avail_iters % f->group_iters;
  }
}

uint32_t
get_filter_metric(FILTER *f, bool_t current)
{
  if (f->avail_iters == 0) {
    return 0;
  }

  // TODO: Magic goes here

  return (f->avail_iters / f->group_iters + 1) * (current ? 2 : 1);
}

FILTER_METRIC
find_best_filter(FILTER *reserved)
{
  FILTER_METRIC best = {.filter = NULL, .metric = 0};

  for (uint32_t i = 0; i < num_filters; i++) {
    FILTER *f = &filters[i];

    if ((f != reserved) && ((f->active_count == 0) || f->data_parallel)) {
      uint32_t metric = get_filter_metric(f, FALSE);

      if (metric > best.metric) {
        best.filter = f;
        best.metric = metric;
      }
    }
  }

  return best;
}

void
schedule_free_spu(SPU_STATE *spu, FILTER *reserved)
{
  FILTER_METRIC best;

  check(spu->current == NULL);
  best = find_best_filter(reserved);

  if (best.metric != 0) {
    uint32_t iters = get_schedule_iters(best.filter, FALSE);

    if (iters != 0) {
      schedule_filter(spu, best.filter, iters);
    }
  }
}

void
schedule_all_free_spu(FILTER *reserved)
{
  for (uint32_t i = 0; i < num_spu; i++) {
    SPU_STATE *spu = &spu_state[i];

    if (spu->current == NULL) {
      schedule_free_spu(spu, reserved);
    }
  }
}

uint32_t
schedule_spu_next(SPU_STATE *spu, FILTER *current, DP_INSTANCE *instance)
{
  FILTER *reserved =
    ((current == NULL) || current->data_parallel ? NULL : current);
  FILTER_METRIC best;

  if (num_free_spu != 0) {
    schedule_all_free_spu(reserved);
  }

  if ((current != NULL) && current->data_parallel &&
      (instance->next != NULL)) {
    current = NULL;
  }

  best = find_best_filter(current);

  if (current != NULL) {
    uint32_t extend_metric = get_filter_metric(current, TRUE);

    if (extend_metric >= best.metric) {
      return (extend_metric == 0 ? 0 : get_schedule_iters(current, TRUE));
    }
  }

  if (best.metric != 0) {
    uint32_t iters = get_schedule_iters(best.filter, FALSE);

    if (iters != 0) {
      schedule_filter(spu, best.filter, iters);
    }
  }

  return 0;
}
