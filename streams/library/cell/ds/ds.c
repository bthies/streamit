#include "ds.h"
#include <stdarg.h>

#define DS_MIN_GROUP_ITERS 5

uint32_t num_filters;
uint32_t num_channels;
uint32_t num_spu = NUM_SPU;

uint32_t incomplete_filters;
uint32_t num_free_spu;

SPU_STATE spu_state[NUM_SPU];

void *channel_data;

DP_INSTANCE dp_instance_data[NUM_SPU * 4];
DP_INSTANCE *next_dp_instance;

static void init_channel_duplicates(CHANNEL *c, CHANNEL *source);
static void init_update_filter_done(FILTER *f);
static void init_update_down_filter_incomplete(CHANNEL *c);

static void calc_filter_buffering(FILTER *f);
static uint32_t calc_input_buffering(FILTER *f, uint32_t iters, bool_t actual);
static uint32_t calc_output_buffering(FILTER *f, uint32_t iters,
                                      bool_t actual);
static uint32_t calc_max_iters(FILTER *f, bool_t input, bool_t output,
                               uint32_t max_size);

void
ds_init()
{
  uint32_t total_channel_size = 0;
  void *next_channel_data;

  // Duplicate channels
  for (uint32_t i = 0; i < num_channels; i++) {
    channels[i].parent = NULL;
  }

  for (uint32_t i = 0; i < num_channels; i++) {
    CHANNEL *c = &channels[i];

    for (uint32_t j = 0; j < c->num_duplicates; j++) {
      c->duplicates[j].parent = c;
    }
  }

  // Alloc channel buffers
  for (uint32_t i = 0; i < num_channels; i++) {
    if (channels[i].parent == NULL) {
      channels[i].buf_size = ROUND_UP(channels[i].buf_size, CACHE_SIZE);
      total_channel_size += channels[i].buf_size;
    }
  }

  channel_data = malloc_buffer_data(total_channel_size);
  if (channel_data == NULL) {
    perror("Unable to allocate channel buffers");
    exit(1);
  }

  next_channel_data = channel_data;

  for (uint32_t i = 0; i < num_channels; i++) {
    CHANNEL *c = &channels[i];

    if (c->parent == NULL) {
      init_buffer(&c->buf, next_channel_data, c->buf_size, !c->non_circular,
                  0);
      next_channel_data += c->buf_size;
      c->used_bytes = 0;
      c->free_bytes = c->buf_size - (c->non_circular ? 0 : QWORD_MASK);

      init_channel_duplicates(c, c);
    }
  }

  // Library init
  if (!spulib_init()) {
    errprintf("Unable to initialize SPUs\n");
    exit(1);
  }
}

static void
init_channel_duplicates(CHANNEL *c, CHANNEL *source)
{
  c->source = source;

  if (c->duplicates != NULL) {
    c->free_waiting = c->num_duplicates;

    for (uint32_t i = 0; i < c->num_duplicates; i++) {
      CHANNEL *d = &c->duplicates[i];

      duplicate_buffer(&d->buf, &source->buf);
      d->used_bytes = 0;
      d->free_bytes = 0;

      init_channel_duplicates(d, source);
    }
  }
}

void
ds_init_2()
{
  // Init filter state
  for (uint32_t i = 0; i < num_filters; i++) {
    FILTER *f = &filters[i];

    f->incomplete_inputs = f->desc.num_inputs;

    for (uint32_t j = 0; j < f->desc.num_inputs; j++) {
      if (f->inputs[j].source->output.f == NULL) {
        f->incomplete_inputs--;
      }
    }

    // Buffer sizing
    f->exclusive = (f->desc.num_inputs + f->desc.num_outputs > 7) ||
      (spu_cmd_get_filt_cb_size(f->desc.num_inputs, f->desc.num_outputs,
                                f->desc.state_size) >
       DS_SPU_FILTER_SLOT_SIZE);
    calc_filter_buffering(f);

    if (f->desc.num_outputs == 0) {
      f->output_avail_iters = UINT32_MAX;
    }

    f->visited = FALSE; // for recursive pass
  }

  // need input_avail_iters for next part
  update_all_filters();
  incomplete_filters = num_filters;

  // Push completed filters through stream graph (unlikely)
  for (uint32_t i = 0; i < num_filters; i++) {
    init_update_filter_done(&filters[i]);
  }
}

static void
init_update_filter_done(FILTER *f)
{
  if (!f->visited &&
      (f->incomplete_inputs == 0) && (f->input_avail_iters == 0)) {
    f->visited = TRUE;
    incomplete_filters--;

    for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
      init_update_down_filter_incomplete(&f->outputs[i]);
    }
  }
}

static void
init_update_down_filter_incomplete(CHANNEL *c)
{
  if (c->duplicates == NULL) {
    FILTER *f = c->input.f;

    if (f != NULL) {
      f->incomplete_inputs--;
      init_update_filter_done(f);
    }
  } else {
    for (uint32_t i = 0; i < c->num_duplicates; i++) {
      init_update_down_filter_incomplete(&c->duplicates[i]);
    }
  }
}

void
init_update_down_channel_used(CHANNEL *c, uint32_t num_bytes)
{
  if (c->duplicates == NULL) {
    c->used_bytes += num_bytes;

#if CHECK
    if (c->parent != NULL) {
      buf_inc_tail(&c->buf, num_bytes);
    }
#endif
  } else {
    for (uint32_t i = 0; i < c->num_duplicates; i++) {
      init_update_down_channel_used(&c->duplicates[i], num_bytes);
    }
  }
}

void
init_update_up_channel_free(CHANNEL *c, uint32_t num_bytes)
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
}

static void
calc_filter_buffering(FILTER *f)
{
  uint32_t input_iters = 0;
  uint32_t output_iters = 0;

  for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
    CHANNEL *c = &f->inputs[i];
    c->input.spu_buf_size = c->input.peek_extra_bytes +
      ((((c->buf.head + c->input.peek_extra_bytes) | c->input.pop_bytes) &
        QWORD_MASK) == 0 ?
       CHECK : QWORD_MASK + QWORD_MASK);
  }

  for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
    CHANNEL *c = &f->outputs[i];
    c->output.spu_buf_size =
      (((c->buf.tail | c->output.push_bytes) & QWORD_MASK) == 0 ?
       0 : QWORD_MASK);
  }

  if (!f->exclusive) {
    input_iters = calc_max_iters(f, TRUE, FALSE, 32768);

    if (input_iters < DS_MIN_GROUP_ITERS) {
      f->exclusive = TRUE;
    }
  }

  if (!f->exclusive) {
    output_iters = calc_max_iters(f, FALSE, TRUE, 32768);

    if (output_iters < DS_MIN_GROUP_ITERS) {
      f->exclusive = TRUE;
    }
  }

  if (!f->exclusive) {
    f->group_iters = min_uint32(input_iters, output_iters);
  } else {
    f->group_iters = calc_max_iters(f, TRUE, TRUE, 32768 * 3);
    check(f->group_iters != 0);
  }

  calc_input_buffering(f, f->group_iters, TRUE);
  calc_output_buffering(f, f->group_iters, TRUE);
  f->loop_iters = max_uint32(f->group_iters / DS_MIN_GROUP_ITERS, 1);

#if PRINT_BUFFERING
  printf("Filter %2d [%-20.20s]: %-1.1s g: %5d l: %5d b: %5d\n",
         f - filters, f->name, (f->exclusive ? "e" : ""), f->group_iters,
         f->loop_iters, f->output_buffered_iters);

  if (f->desc.num_inputs != 0) {
    printf("  I:");
    for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
      uint32_t buf_size = f->inputs[i].input.spu_buf_size;
      if (buf_size >= 1024) {
        printf(" %2dk", buf_size >> 10);
      } else {
        printf(" %3d", buf_size);
      }
    }
    printf("\n");
  }

  if (f->desc.num_outputs != 0) {
    printf("  O:");
    for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
      uint32_t buf_size = f->outputs[i].output.spu_buf_size;
      if (buf_size >= 1024) {
        printf(" %2dk", buf_size >> 10);
      } else {
        printf(" %3d", buf_size);
      }
    }
    printf("\n");
  }
#endif
}

static uint32_t
calc_input_buffering(FILTER *f, uint32_t iters, bool_t actual)
{
  uint32_t total = 0;

  for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
    FILTER_INPUT_TAPE *input = &f->inputs[i].input;
    uint32_t pop_bytes = input->pop_bytes * iters;
    uint32_t buf_size =
      next_power_2(input->spu_buf_size + pop_bytes * 2 +
                   (pop_bytes >= CACHE_SIZE ? 0 : CACHE_MASK));

    total += buf_size;

    if (actual) {
      input->spu_buf_size = buf_size;
    }
  }

  return total;
}

static uint32_t
calc_output_buffering(FILTER *f, uint32_t iters, bool_t actual)
{
  uint32_t total = 0;
  uint32_t buffering_iters = 0;

  for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
    FILTER_OUTPUT_TAPE *output = &f->outputs[i].output;
    uint32_t push_bytes = output->push_bytes * iters;
    uint32_t max_buffered = output->spu_buf_size +
      (push_bytes >= CACHE_SIZE + QWORD_MASK ? 0 : CACHE_MASK);
    uint32_t buf_size = next_power_2(push_bytes * 2 + max_buffered + CHECK);

    total += buf_size;

    if (actual) {
      output->spu_buf_size = buf_size;
      buffering_iters = max_uint32(buffering_iters,
        (max_buffered + (output->push_bytes - 1)) / output->push_bytes);
    }
  }

  if (actual) {
    f->output_buffered_iters = buffering_iters;
  }

  return total;
}

static uint32_t
calc_max_iters(FILTER *f, bool_t inputs, bool_t outputs, uint32_t max_size)
{
  uint32_t i;
  uint32_t j;

  if ((!outputs && (f->desc.num_inputs == 0)) ||
      (!inputs && (f->desc.num_outputs == 0))) {
    return UINT32_MAX;
  }

  j = 1;

  while ((inputs ? calc_input_buffering(f, j, FALSE) : 0) +
         (outputs ? calc_output_buffering(f, j, FALSE) : 0) <= max_size) {
    j = j << 1;
  }

  if (j == 1) {
    return 0;
  }

  i = j >> 1;
  j--;

  while (i != j) {
    uint32_t k = (i + j + 1) >> 1;

    if ((inputs ? calc_input_buffering(f, k, FALSE) : 0) +
        (outputs ? calc_output_buffering(f, k, FALSE) : 0) <= max_size) {
      i = k;
    } else {
      j = k - 1;
    }
  }

  return i;
}

#if CHECK

void
check_equal(uint32_t value, uint32_t expected, char *var_format, ...)
{
  if (value != expected) {
    char var[40];
    va_list var_args;

    va_start(var_args, var_format);
    vsnprintf(var, sizeof(var), var_format, var_args);
    va_end(var_args);

    printf("%s: Expected %d, got %d\n", var, expected, value);
  }
}

#endif
