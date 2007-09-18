#include "ds.h"

// TODO: builtin_expect num_inputs/outputs == 1?

#define PHASE_WAITING   0
#define PHASE_ALLOC     1
#define PHASE_LOAD      2
#define PHASE_INIT_A    3
#define PHASE_INIT_B    4
#define PHASE_STEADY    5
#define PHASE_CLEANUP_A 6
#define PHASE_CLEANUP_B 7

static void run_init();
#if CHECK
static void run_done_check();
#endif

static void start_filter(ACTIVE_FILTER *af);
static void setup_load(ACTIVE_FILTER *af);
static void setup_init_a(ACTIVE_FILTER *af);
static void setup_init_b(ACTIVE_FILTER *af);
static void setup_steady(ACTIVE_FILTER *af, uint32_t group);
static void setup_cleanup_a(ACTIVE_FILTER *af, uint32_t group);
static void setup_cleanup_b(ACTIVE_FILTER *af, uint32_t group);

static void adjust_input(ACTIVE_FILTER *af, uint32_t group);

static void handle_complete(SPU_STATE *spu, ACTIVE_FILTER *af, bool_t current);
static void start_input(ACTIVE_FILTER *af, bool_t first);
static void start_output(ACTIVE_FILTER *af, uint32_t groups_left);
static void finish_output(ACTIVE_FILTER *af);
static void done_filter(SPU_STATE *spu, ACTIVE_FILTER *af, bool_t current);
static void extend_run(ACTIVE_FILTER *af, uint32_t iters);

static void spu_cb(uint32_t spu_id, uint32_t new_completed,
                   uint32_t all_completed);

static void update_input_channels(ACTIVE_FILTER *af);
static void update_output_channels(ACTIVE_FILTER *af, bool_t buffered);

static void done_dp_instance(FILTER *f, DP_INSTANCE *instance);
static void update_dp_input(FILTER *f, DP_INSTANCE *instance,
                            uint32_t input_iters, bool_t wait);
static void update_dp_output(FILTER *f, DP_INSTANCE *instance,
                             uint32_t output_iters, bool_t wait);

static INLINE void
issue_group(ACTIVE_FILTER *af, uint32_t group)
{
  if (spu_get_group(af->spu_id, af->cmd_group_start + group)->size != 0) {
    spu_issue_group(af->spu_id, af->cmd_group_start + group,
                    af->cmd_data_start + group * af->group_data_size);
  }
}

static INLINE uint32_t
get_mask(uint32_t n)
{
  return (1 << n) - 1;
}

static INLINE uint32_t
get_alloc_mask(ACTIVE_FILTER *af)
{
  return get_mask(af->f->desc.num_inputs) <<
    (1 + af->group_num_cmd + af->cmd_id_start);
}

static INLINE uint32_t
get_load_mask(ACTIVE_FILTER *af)
{
  return get_mask(1 + af->f->desc.num_inputs) << af->cmd_id_start;
}

static INLINE uint32_t
get_init_a_mask(ACTIVE_FILTER *af)
{
  return ((get_mask(af->group_num_cmd) << af->group_num_cmd) |
          (get_mask(af->f->desc.num_outputs) << (1 + af->f->desc.num_inputs)))
    << af->cmd_id_start;
}

static INLINE uint32_t
get_init_b_mask(ACTIVE_FILTER *af)
{
  return get_mask(1 + af->f->desc.num_inputs) << af->cmd_id_start;
}

static INLINE uint32_t
get_steady_0_mask(ACTIVE_FILTER *af)
{
  return get_mask(af->group_num_cmd) << af->cmd_id_start;
}

static INLINE uint32_t
get_cleanup_a_mask(ACTIVE_FILTER *af, uint32_t group)
{
  return
    (1 | (get_mask(af->f->desc.num_outputs) << (1 + af->f->desc.num_inputs)))
    << (af->cmd_id_start + (group * af->group_num_cmd));
}

static INLINE uint32_t
get_cleanup_b_mask(ACTIVE_FILTER *af, uint32_t group)
{
  FILTER *f = af->f;
  uint32_t mask = get_mask(f->desc.num_outputs) << (1 + f->desc.num_inputs);

  if (SPU_STATS_ENABLE || ((f->desc.state_size != 0) && !f->data_parallel)) {
    mask |= 1;
  }

  return mask << (af->cmd_id_start + (group * af->group_num_cmd));
}

static INLINE uint32_t
get_short_cleanup_mask(ACTIVE_FILTER *af)
{
  return (1 << af->cmd_id_start) | get_cleanup_b_mask(af, 1);
}

static INLINE void
setup_run(SPU_CMD_GROUP *g, ACTIVE_FILTER *af, uint32_t iters, uint32_t cmd_id,
          uint32_t dep_start)
{
  FILTER *f = af->f;
  SPU_CMD_HEADER *run_cmd;

  run_cmd = spu_filter_run(g,
                           af->filt_cb, iters, f->loop_iters,
                           cmd_id,
                           0);
  run_cmd->num_back_deps = af->group_num_cmd;

  for (uint32_t i = 0; i < run_cmd->num_back_deps; i++) {
    run_cmd->deps[i] = dep_start + i;
  }
}

static INLINE void
setup_input(SPU_CMD_GROUP *g, ACTIVE_FILTER *af, uint32_t iters,
            uint32_t cmd_id_start, uint32_t input_dep_start, uint32_t run_dep)
{
  FILTER *f = af->f;

  for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
    spu_dt_in_back_ppu(g,
                       af->inputs[i].spu_buf_data, af->inputs[i].channel_buf,
                       iters * f->inputs[i].input.pop_bytes,
                       cmd_id_start + i,
                       2,
                       input_dep_start + i,
                       run_dep);
  }
}

static INLINE void
setup_output(SPU_CMD_GROUP *g, ACTIVE_FILTER *af, uint32_t iters,
             uint32_t cmd_id_start, uint32_t output_dep_start,
             uint32_t run_dep)
{
  FILTER *f = af->f;

  for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
    spu_dt_out_front_ppu_ex(g,
                            af->outputs[i].spu_buf_data,
                            af->outputs[i].channel_buf,
                            iters * f->outputs[i].output.push_bytes,
                            f->data_parallel,
                            cmd_id_start + i,
                            2,
                            output_dep_start + i,
                            run_dep);
  }
}

void
schedule_filter(SPU_STATE *spu, FILTER *f, uint32_t iters)
{
  ACTIVE_FILTER *af;
  SPU_ADDRESS input_buf_start;
  SPU_ADDRESS output_buf_start;

#if PRINT_SCHEDULE
  printf("SPU %d: Scheduling: %2d [%-20.20s] for %5d iters\n",
         spu->spu_id, f - filters, f->name, iters);
#endif

  af = &spu->filters[spu->next_filt];
  spu->next_filt ^= 1;

  check(spu->next == NULL);

  if (spu->current == NULL) {
    spu->current = af;
    num_free_spu--;
  } else {
    check(!f->exclusive);
    spu->next = af;
  }

  af->f = f;

  check((f->active_count == 0) || f->data_parallel);
  f->active_count++;
  check(f->avail_iters >= iters);
  f->avail_iters -= iters;
  f->input_avail_iters -= iters;
  f->output_avail_iters -= iters;

  af->groups_left = (iters - 1) / f->group_iters + 1;
  af->last_group_iters = (iters - 1) % f->group_iters + 1;
  af->done_input_groups = 0;
  af->done_output_groups = 0;
  af->updated = FALSE;

  // New instance of data-parallel filter
  if (f->data_parallel) {
    DP_INSTANCE *instance = new_dp_instance();
    af->instance = instance;

    instance->next = NULL;

    if (f->instances == NULL) {
      instance->prev = NULL;
      f->instances = instance;
    } else {
      instance->prev = f->last_instance;
      f->last_instance->next = instance;
    }

    f->last_instance = instance;

    instance->input_iters = 0;
    instance->output_iters = 0;
    instance->done = FALSE;
  }

  // Input bookkeeping
  input_buf_start = (f->exclusive ? 0 : get_spu_buffer_slot(spu));

  for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
    CHANNEL *c = &f->inputs[i];
    ACTIVE_INPUT_TAPE *ait = &af->inputs[i];
    uint32_t pop_bytes;

    pop_bytes = iters * c->input.pop_bytes;
    safe_dec(c->used_bytes, pop_bytes);

    input_buf_start += CACHE_SIZE;
    ait->spu_buf_data = input_buf_start;
    input_buf_start += c->input.spu_buf_size;

    if (f->data_parallel) {
      duplicate_buffer(&ait->channel_buf_instance, &c->buf);
      ait->channel_buf = &ait->channel_buf_instance;

      buf_inc_head(&c->buf, pop_bytes);
    } else {
      ait->channel_buf = &c->buf;
    }
  }

  // Output bookkeeping
  output_buf_start =
    (f->exclusive ? input_buf_start : get_spu_buffer_slot(spu));

  for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
    CHANNEL *c = &f->outputs[i];
    ACTIVE_OUTPUT_TAPE *aot = &af->outputs[i];
    uint32_t push_bytes;

    push_bytes = iters * c->output.push_bytes;
    safe_dec(c->free_bytes, push_bytes);

    output_buf_start += CACHE_SIZE;
    aot->spu_buf_data = output_buf_start;
    output_buf_start += c->output.spu_buf_size;

    if (f->data_parallel) {
      duplicate_buffer(&aot->channel_buf_instance, &c->buf);
      aot->channel_buf = &aot->channel_buf_instance;

      buf_inc_tail(&c->buf, push_bytes);
    } else {
      aot->channel_buf = &c->buf;
    }
  }

  af->output_buffered_iters = 0;

  if (f->exclusive) {
    af->filt_cb = output_buf_start;
    af->cmd_data_start = af->filt_cb +
      spu_cmd_get_filt_cb_size(f->desc.num_inputs, f->desc.num_outputs,
                               f->desc.state_size);
    af->cmd_id_start = 0;
    af->cmd_group_start = 28;
  } else {
    af->filt_cb = get_spu_filter_slot(spu);
    af->cmd_data_start = get_spu_cmd_data_slot(spu);
    af->cmd_id_start = get_spu_cmd_id_slot(spu);
    af->cmd_group_start = get_spu_cmd_group_slot(spu);
  }

  af->group_num_cmd = 1 + f->desc.num_inputs + f->desc.num_outputs;
  af->group_data_size = af->group_num_cmd * 64;

  setup_load(af);

  if (!f->exclusive || (spu->next == NULL)) {
    start_filter(af);
  } else {
    af->phase = PHASE_WAITING;
    af->waiting_mask = 0;
  }
}

static void
start_filter(ACTIVE_FILTER *af)
{
  issue_group(af, 0);
  setup_init_a(af);

  af->phase = PHASE_ALLOC;
  af->waiting_mask = get_alloc_mask(af);
}

static void
setup_load(ACTIVE_FILTER *af)
{
  FILTER *f = af->f;
  SPU_CMD_GROUP *g = spu_new_group(af->spu_id, af->cmd_group_start + 0);
  uint32_t load_id = af->cmd_id_start + 0 + 0;
  uint32_t attach_id_start = load_id + 1;
  uint32_t alloc_id_start = attach_id_start + af->group_num_cmd;

  spu_filter_load(g,
                  af->filt_cb, &f->desc,
                  load_id,
                  0);

  for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
    spu_buffer_alloc(g,
                     af->inputs[i].spu_buf_data,
                     f->inputs[i].input.spu_buf_size,
                     af->inputs[i].channel_buf->head & CACHE_MASK,
                     alloc_id_start + i,
                     0);
    spu_filter_attach_input(g,
                            af->filt_cb, i, af->inputs[i].spu_buf_data,
                            attach_id_start + i,
                            2,
                            load_id,
                            alloc_id_start + i);
  }
}

static void
setup_init_a(ACTIVE_FILTER *af)
{
  FILTER *f = af->f;
  SPU_CMD_GROUP *g = spu_new_group(af->spu_id, af->cmd_group_start + 1);
  uint32_t input_iters =
    (af->groups_left == 1 ? af->last_group_iters : f->group_iters);
  uint32_t load_id = af->cmd_id_start + 0 + 0;
  uint32_t null_id = load_id + af->group_num_cmd;
  uint32_t input_id_start = null_id + 1;
  uint32_t alloc_id_start = load_id + 1 + f->desc.num_inputs;
  uint32_t attach_id_start = alloc_id_start + af->group_num_cmd;

  spu_null(g,
           null_id,
           0);

  for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
    spu_dt_in_back_ppu(g,
                       af->inputs[i].spu_buf_data, af->inputs[i].channel_buf,
                       f->inputs[i].input.peek_extra_bytes +
                         input_iters * f->inputs[i].input.pop_bytes,
                       input_id_start + i,
                       0);
  }

  for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
    spu_buffer_alloc(g,
                     af->outputs[i].spu_buf_data,
                     f->outputs[i].output.spu_buf_size,
                     af->outputs[i].channel_buf->tail & CACHE_MASK,
                     alloc_id_start + i,
                     1,
                     null_id);
    spu_filter_attach_output(g,
                             af->filt_cb, i, af->outputs[i].spu_buf_data,
                             attach_id_start + i,
                             2,
                             load_id,
                             alloc_id_start + i);
  }
}

static void
setup_init_b(ACTIVE_FILTER *af)
{
  FILTER *f = af->f;
  SPU_CMD_GROUP *g = spu_new_group(af->spu_id, af->cmd_group_start + 0);
  uint32_t id_start = af->cmd_id_start + 0;
  uint32_t dep_start = af->cmd_id_start + af->group_num_cmd;
  uint32_t input_id_start = id_start + 1;
  uint32_t input_dep_start = dep_start + 1;

  setup_run(g, af,
            (af->groups_left == 1 ? af->last_group_iters : f->group_iters),
            id_start + 0, dep_start);

  if (af->groups_left != 1) {
    setup_input(g, af,
                (af->groups_left == 2 ? af->last_group_iters : f->group_iters),
                input_id_start, input_dep_start, dep_start + 0);
  }
}

static void
setup_steady(ACTIVE_FILTER *af, uint32_t group)
{
  FILTER *f = af->f;
  SPU_CMD_GROUP *g;
  uint32_t group_offset = group * af->group_num_cmd;
  uint32_t id_start = af->cmd_id_start + group_offset;
  uint32_t dep_start = af->cmd_id_start + (af->group_num_cmd - group_offset);
  uint32_t input_id_start = id_start + 1;
  uint32_t input_dep_start = dep_start + 1;

  if (group == 0) {
    g = spu_get_group(af->spu_id, af->cmd_group_start + 0);
  } else {
    g = spu_new_group(af->spu_id, af->cmd_group_start + 1);

    setup_run(g, af, f->group_iters, id_start + 0, dep_start);
    setup_input(g, af,
                (af->groups_left == 3 ? af->last_group_iters : f->group_iters),
                input_id_start, input_dep_start, dep_start + 0);
  }

  setup_output(g, af, f->group_iters, input_id_start + f->desc.num_inputs,
               input_dep_start + f->desc.num_inputs, dep_start + 0);
}

static void
setup_cleanup_a(ACTIVE_FILTER *af, uint32_t group)
{
  FILTER *f = af->f;
  SPU_CMD_GROUP *g = spu_new_group(af->spu_id, af->cmd_group_start + group);
  uint32_t group_offset = group * af->group_num_cmd;
  uint32_t id_start = af->cmd_id_start + group_offset;
  uint32_t dep_start = af->cmd_id_start + (af->group_num_cmd - group_offset);

  setup_run(g, af, af->last_group_iters, id_start + 0, dep_start);
  setup_output(g, af, f->group_iters, id_start + 1 + f->desc.num_inputs,
               dep_start + 1 + f->desc.num_inputs, dep_start + 0);
}

static void
setup_cleanup_b(ACTIVE_FILTER *af, uint32_t group)
{
  FILTER *f = af->f;
  SPU_CMD_GROUP *g = spu_new_group(af->spu_id, af->cmd_group_start + group);
  uint32_t group_offset = group * af->group_num_cmd;
  uint32_t id_start = af->cmd_id_start + group_offset;
  uint32_t dep_start = af->cmd_id_start + (af->group_num_cmd - group_offset);

  if (SPU_STATS_ENABLE || ((f->desc.state_size != 0) && !f->data_parallel)) {
    spu_filter_unload(g,
                      af->filt_cb,
                      id_start + 0,
                      1,
                      dep_start + 0);
  }

  setup_output(g, af, af->last_group_iters, id_start + 1 + f->desc.num_inputs,
               dep_start + 1 + f->desc.num_inputs, dep_start + 0);
}

static void
adjust_input(ACTIVE_FILTER *af, uint32_t group)
{
  FILTER *f = af->f;
  SPU_DT_IN_BACK_CMD *cmd;

  cmd = (SPU_DT_IN_BACK_CMD *)
    ((void *)spu_first_command(spu_get_group(af->spu_id,
                                             af->cmd_group_start + group)) +
     sizeof(SPU_FILTER_RUN_CMD));

  for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
    cmd->num_bytes = af->last_group_iters * f->inputs[i].input.pop_bytes;
    cmd++;
  }
}

static void
handle_complete(SPU_STATE *spu, ACTIVE_FILTER *af, bool_t current)
{
  FILTER *f = af->f;

  while ((spu_get_completed(af->spu_id) & af->waiting_mask) ==
         af->waiting_mask) {
    spu_ack_completed(af->spu_id, af->waiting_mask);

    switch (af->phase) {
    case PHASE_WAITING:
      if (!current) {
        return;
      }

      start_filter(af);
      break;

    case PHASE_ALLOC:
      if (!current) {
        SPU_CMD_HEADER *cmd;

        if (!spu->last_run_issued) {
          af->waiting_mask = 0;
          return;
        }

        cmd = spu_first_command(spu_get_group(af->spu_id,
                                              af->cmd_group_start + 1));
        cmd->num_back_deps = 1;
        cmd->deps[0] = spu->last_run_id;

        spu->last_run_issued = FALSE;
      }

      start_input(af, TRUE);

      issue_group(af, 1); // init_a
      setup_init_b(af);

      af->phase = PHASE_LOAD;
      af->waiting_mask = get_load_mask(af);
      break;

    case PHASE_LOAD:
      issue_group(af, 0); // init_b

      af->phase = PHASE_INIT_A;
      af->waiting_mask = get_init_a_mask(af);
      break;

    case PHASE_INIT_A:
      af->done_input_groups++;

      start_output(af, af->groups_left - 2);

      if (af->groups_left == 1) {
        setup_cleanup_b(af, 1); // issues cleanup_b

        af->phase = PHASE_CLEANUP_B;
        af->waiting_mask = get_short_cleanup_mask(af);
      } else {
        if (af->groups_left == 2) {
          setup_cleanup_a(af, 1); // issues cleanup_a
        } else {
          setup_steady(af, 1); // issues steady (1)
        }

        af->phase = PHASE_INIT_B;
        af->waiting_mask = get_init_b_mask(af);
      }

      issue_group(af, 1);
      break;

    case PHASE_INIT_B:
      if (af->groups_left != 2) {
        setup_steady(af, 0);
      }

      af->current_group = 0;
      af->groups_left--;
      af->input_groups_left--;
      af->output_groups_left++;
      af->done_output_groups--;

      af->phase = PHASE_STEADY;
      af->waiting_mask = get_steady_0_mask(af);
      af->cmd_mask =
        af->waiting_mask | (af->waiting_mask << af->group_num_cmd);

    case PHASE_STEADY:
      af->groups_left--;
      af->done_input_groups++;
      af->done_output_groups++;

      if (current && !af->updated) {
        switch (af->groups_left) {
        case 2:
        case 1:
          update_input_channels(af);
          update_output_channels(af, TRUE);
          af->updated = TRUE;

          if (!f->exclusive && (spu->next == NULL)) {
            uint32_t more_iters =
              schedule_spu_next(spu,
                                (af->last_group_iters == f->group_iters ?
                                 f : NULL),
                                af->instance);

            if (more_iters != 0) {
#if PRINT_SCHEDULE
              printf("SPU %d: Extending:  %2d [%-20.20s] for %5d iters\n",
                     af->spu_id, f - filters, f->name, more_iters);
#endif
              check(spu->next == NULL);
              extend_run(af, more_iters);
              af->updated = FALSE;
            }
          }

          break;
        }
      }

      if ((--af->input_groups_left == 0) && (af->groups_left != 0)) {
        start_input(af, FALSE);
      }

      if (--af->output_groups_left == 0) {
        // Downstream filter can be scheduled before this is called but it
        // won't start transferring until after
        finish_output(af);
        start_output(af, af->groups_left);
      }

      af->waiting_mask ^= af->cmd_mask;

      switch (af->groups_left) {
      case 2:
        if (af->last_group_iters != f->group_iters) {
          adjust_input(af, af->current_group); // issues modified steady
        }

        break;

      case 1:
        setup_cleanup_a(af, af->current_group); // issues cleanup_a

        if (current && (spu->next != NULL)) {
          spu->last_run_id =
            af->cmd_id_start + (af->current_group * af->group_num_cmd) + 0;
          spu->last_run_issued = TRUE;
        }

        break;

      case 0:
        setup_cleanup_b(af, af->current_group); // issues cleanup_b

        af->waiting_mask = get_cleanup_a_mask(af, af->current_group ^ 1);

        if (af->output_groups_left == 2) {
          af->phase = PHASE_CLEANUP_B;
          af->waiting_mask |= get_cleanup_b_mask(af, af->current_group);
        } else {
          check(af->output_groups_left == 1);
          af->phase = PHASE_CLEANUP_A;
        }

        af->done_output_groups++;
        break;
      }

      issue_group(af, af->current_group);
      af->current_group ^= 1;
      break;

    case PHASE_CLEANUP_A:
      check(af->groups_left == 0);

      finish_output(af);
      start_output(af, -1);

      af->phase = PHASE_CLEANUP_B;
      af->waiting_mask = get_cleanup_b_mask(af, af->current_group ^ 1);
      break;

    case PHASE_CLEANUP_B:
      finish_output(af);
      af->done_output_groups++;

      af->groups_left = 0;
      update_input_channels(af);
      af->groups_left = -2;
      update_output_channels(af, FALSE);

      done_filter(spu, af, current);
      return;

    default:
      unreached();
    }
  }

  if (current && (spu->next != NULL)) {
    handle_complete(spu, spu->next, FALSE);
  }
}

static void
start_input(ACTIVE_FILTER *af, bool_t first)
{
  FILTER *f = af->f;
  uint32_t iters;

  check(af->groups_left != 0);
  af->input_groups_left = af->groups_left;
  iters = (af->input_groups_left - 1) * f->group_iters + af->last_group_iters;

  for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
    dt_out_front_ex(af->inputs[i].channel_buf, af->spu_id,
                    af->inputs[i].spu_buf_data,
                    (first ? f->inputs[i].input.peek_extra_bytes : 0) +
                      iters * f->inputs[i].input.pop_bytes);
  }
}

static void
start_output(ACTIVE_FILTER *af, uint32_t groups_left)
{
  FILTER *f = af->f;
  uint32_t iters;

  check(groups_left != (uint32_t)-2);
  af->output_groups_left = groups_left + 2;
  af->done_unaligned_output = FALSE;
  iters = (af->output_groups_left - 1) * f->group_iters + af->last_group_iters;

  for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
    dt_in_back_ex(af->outputs[i].channel_buf, af->spu_id,
                  af->outputs[i].spu_buf_data,
                  iters * f->outputs[i].output.push_bytes);
  }
}

static void
finish_output(ACTIVE_FILTER *af)
{
  FILTER *f = af->f;

  if (!af->done_unaligned_output) {
    for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
      finish_dt_in_back_ex_head(af->outputs[i].channel_buf, f->data_parallel);
    }

    af->done_unaligned_output = TRUE;
  }

  if (f->data_parallel) {
    for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
      finish_dt_in_back_ex_tail(af->outputs[i].channel_buf);
    }
  }
}

static void
done_filter(SPU_STATE *spu, ACTIVE_FILTER *af, bool_t current)
{
  FILTER *f = af->f;

  if (f->data_parallel) {
    done_dp_instance(f, af->instance);
  } else {
    for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
      buf_inc_head(&f->inputs[i].buf, -f->inputs[i].input.peek_extra_bytes);
    }
  }

  if (--f->active_count == 0) {
    update_filter_done(f);
  }

#if PRINT_SCHEDULE
  printf("SPU %d: Done:       %2d [%-20.20s]\n",
         af->spu_id, f - filters, f->name);
#endif

  if (current) {
    spu->current = spu->next;
    spu->next = NULL;
    spu->last_run_issued = FALSE;

    if (spu->current != NULL) {
      handle_complete(spu, spu->current, TRUE);
    } else {
      if (num_free_spu++ == 0) {
        schedule_free_spu(spu, NULL);
      } else {
        schedule_all_free_spu(NULL);
      }
    }
  } else {
    spu->next = NULL;
    // Use same slots again for next filter
    spu->next_filt ^= 1;
    spu->next_buf_slot =
      (spu->next_buf_slot == 2 ? 0 : spu->next_buf_slot + 1);
    spu->next_filt_slot ^= 1;
    spu->next_cmd_data_slot ^= 1;
    spu->next_cmd_id_slot ^= 1;
    spu->next_cmd_group_slot ^= 1;

    schedule_spu_next(spu, NULL, NULL);
  }
}

static void
extend_run(ACTIVE_FILTER *af, uint32_t iters)
{
  FILTER *f = af->f;
  uint32_t groups;

  check((af->last_group_iters == f->group_iters) &&
        (!f->data_parallel || (af->instance->next == NULL)));

  check(f->avail_iters >= iters);
  f->avail_iters -= iters;
  f->input_avail_iters -= iters;
  f->output_avail_iters -= iters;

  groups = (iters - 1) / f->group_iters + 1;
  af->groups_left += groups;
  af->last_group_iters = (iters - 1) % f->group_iters + 1;

  for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
    uint32_t pop_bytes = iters * f->inputs[i].input.pop_bytes;

    safe_dec(f->inputs[i].used_bytes, pop_bytes);

    if (f->data_parallel) {
      buf_inc_head(&f->inputs[i].buf, pop_bytes);
      IF_CHECK(buf_set_tail(&af->inputs[i].channel_buf_instance,
                            f->inputs[i].buf.tail));
    }
  }

  for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
    uint32_t push_bytes = iters * f->outputs[i].output.push_bytes;

    safe_dec(f->outputs[i].free_bytes, push_bytes);

    if (f->data_parallel) {
      buf_inc_tail(&f->outputs[i].buf, push_bytes);
      IF_CHECK(buf_set_head(&af->outputs[i].channel_buf_instance,
                            f->outputs[i].buf.head));
    }
  }
}

static void
spu_cb(uint32_t spu_id, uint32_t new_completed, uint32_t all_completed)
{
  SPU_STATE *spu = &spu_state[spu_id];

  UNUSED_PARAM(new_completed);
  UNUSED_PARAM(all_completed);

  check(spu->current != NULL);
  handle_complete(spu, spu->current, TRUE);
}

static void
update_input_channels(ACTIVE_FILTER *af)
{
  FILTER *f = af->f;
  bool_t done = (af->groups_left == 0);
  uint32_t iters;

  if (af->done_input_groups == 0) {
    return;
  }

  iters = (af->done_input_groups - 1) * f->group_iters +
    (done ? af->last_group_iters : f->group_iters);
  af->done_input_groups = 0;

  if (f->data_parallel) {
    update_dp_input(f, af->instance, iters, done);
  } else {
    for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
      update_up_channel_free(&f->inputs[i],
                             iters * f->inputs[i].input.pop_bytes);
    }
  }
}

static void
update_output_channels(ACTIVE_FILTER *af, bool_t buffered)
{
  FILTER *f = af->f;
  bool_t done = (af->groups_left == (uint32_t)-2);
  uint32_t iters;

  check(!done || !buffered);

  if (af->done_output_groups == 0) {
    if (buffered || (af->output_buffered_iters == 0)) {
      return;
    } else {
      iters = af->output_buffered_iters;
      af->output_buffered_iters = 0;
    }
  } else {
    iters = af->output_buffered_iters +
      (af->done_output_groups - 1) * f->group_iters +
      (done ? af->last_group_iters : f->group_iters);
    af->done_output_groups = 0;

    if (buffered) {
      if (iters <= f->output_buffered_iters) {
        af->output_buffered_iters = iters;
        return;
      } else {
        iters -= f->output_buffered_iters;
        af->output_buffered_iters = f->output_buffered_iters;
      }
    } else {
      af->output_buffered_iters = 0;
    }
  }

  if (!af->done_unaligned_output) {
    for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
      finish_dt_in_back_ex_head(af->outputs[i].channel_buf, f->data_parallel);
    }

    af->done_unaligned_output = TRUE;
  }

  if (f->data_parallel) {
    update_dp_output(f, af->instance, iters, done);
  } else {
    for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
      update_down_channel_used(&f->outputs[i],
                               iters * f->outputs[i].output.push_bytes);
    }
  }
}

static void
done_dp_instance(FILTER *f, DP_INSTANCE *instance)
{
  uint32_t input_iters = instance->input_iters;
  uint32_t output_iters = instance->output_iters;
  DP_INSTANCE *next_instance;

  check((input_iters != 0) && (output_iters != 0));
  next_instance = instance->next;

  if (next_instance != NULL) {
    while ((next_instance != NULL) && next_instance->done) {
      DP_INSTANCE *temp;

      input_iters += next_instance->input_iters;
      output_iters += next_instance->output_iters;

      temp = next_instance;
      next_instance = next_instance->next;
      free_dp_instance(temp);
    }

    instance->next = next_instance;

    if (next_instance == NULL) {
      f->last_instance = instance;
    } else {
      next_instance->prev = instance;

      input_iters += next_instance->input_iters;
      output_iters += next_instance->output_iters;
      next_instance->input_iters = 0;
      next_instance->output_iters = 0;
    }
  }

  if (f->instances == instance) {
    for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
      update_up_channel_free(&f->inputs[i],
                             input_iters * f->inputs[i].input.pop_bytes);
    }

    for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
      update_down_channel_used(&f->outputs[i],
                               output_iters * f->outputs[i].output.push_bytes);
    }

    f->instances = next_instance;

    if (next_instance == NULL) {
      f->last_instance = NULL;
    } else {
      next_instance->prev = NULL;
    }

    free_dp_instance(instance);
  } else {
    DP_INSTANCE *prev_instance = instance->prev;

    check(!f->instances->done);

    if (prev_instance->done) {
      while (prev_instance->done) {
        DP_INSTANCE *temp;

        input_iters += prev_instance->input_iters;
        output_iters += prev_instance->output_iters;

        temp = prev_instance;
        prev_instance = prev_instance->prev;
        free_dp_instance(temp);
      }

      instance->prev = prev_instance;
      prev_instance->next = instance;
    }

    instance->input_iters = input_iters;
    instance->output_iters = output_iters;
    instance->done = TRUE;
  }
}

static void
update_dp_input(FILTER *f, DP_INSTANCE *instance, uint32_t input_iters,
                bool_t wait)
{
  input_iters += instance->input_iters;

  if (!wait && (f->instances == instance)) {
    for (uint32_t i = 0; i < f->desc.num_inputs; i++) {
      update_up_channel_free(&f->inputs[i],
                             input_iters * f->inputs[i].input.pop_bytes);
    }

    instance->input_iters = 0;
  } else {
    instance->input_iters = input_iters;
  }
}

static void
update_dp_output(FILTER *f, DP_INSTANCE *instance, uint32_t output_iters,
                 bool_t wait)
{
  output_iters += instance->output_iters;

  if (!wait && (f->instances == instance)) {
    for (uint32_t i = 0; i < f->desc.num_outputs; i++) {
      update_down_channel_used(&f->outputs[i],
                               output_iters * f->outputs[i].output.push_bytes);
    }

    instance->output_iters = 0;
  } else {
    instance->output_iters = output_iters;
  }
}

static void
run_init()
{
  ds_init_2();

  for (uint32_t i = 0; i < num_filters; i++) {
    FILTER *f = &filters[i];

    f->active_count = 0;
    f->instances = NULL;
    f->last_instance = NULL;
  }

  for (uint32_t i = 0; i < arraysize(dp_instance_data) - 1; i++) {
    dp_instance_data[i].next = &dp_instance_data[i + 1];
  }

  next_dp_instance = &dp_instance_data[0];

  num_free_spu = num_spu;

  for (uint32_t i = 0; i < num_spu; i++) {
    SPU_STATE *spu = &spu_state[i];

    spu->spu_id = i;
    spu->filters[0].spu_id = i;
    spu->filters[1].spu_id = i;

    spu->buffer_start = 0;
    spu->filter_start = spu->buffer_start + 3 * DS_SPU_BUFFER_SLOT_SIZE;
    spu->cmd_data_start = spu->filter_start + 2 * DS_SPU_FILTER_SLOT_SIZE;

    // Everything else is already 0/NULL
  }
}

#if CHECK

static void
run_done_check()
{
  check(num_free_spu == num_spu);

  for (uint32_t i = 0; i < num_filters; i++) {
    FILTER *f = &filters[i];

    check((f->incomplete_inputs == 0) && (f->active_count == 0) &&
          (f->avail_iters == 0) && (f->input_avail_iters == 0) &&
          (f->instances == NULL) && (f->last_instance == NULL));
  }

  for (uint32_t i = 0; i < num_channels; i++) {
    CHANNEL *c = &channels[i];
    uint32_t avail_bytes =
      c->source->buf_size - (c->source->non_circular ? 0 : QWORD_MASK);
    uint32_t expected_free;

    if (c->duplicates == NULL) {
      check_equal(c->used_bytes, (c->input.f == NULL ?
                                  avail_bytes - c->source->free_bytes : 0),
                  "Channel %2d: used_bytes", i);
    } else {
      check(c->used_bytes == 0);
    }

    if (c->parent == NULL) {
      expected_free = (c->input.f == NULL ?
                       avail_bytes - c->used_bytes : avail_bytes);
    } else {
      expected_free = 0;
    }

    check_equal(c->free_bytes, expected_free,
                "Channel %2d: free_bytes", i);

    if (c->duplicates != NULL) {
      check_equal(c->free_waiting, c->num_duplicates,
                  "Channel %2d: free_waiting", i);
    }
  }
}

#endif

void
ds_run()
{
  run_init();

  spulib_set_all_spu_complete_cb(&spu_cb);
  schedule_all_free_spu(NULL);
  spulib_poll_while(incomplete_filters != 0);
  spulib_set_all_spu_complete_cb(NULL);

#if SPU_STATS_ENABLE
  for (uint32_t i = 0; i < num_spu; i++) {
    SPU_CMD_GROUP *g = spu_new_group(i, 0);
    spu_stats_print(g,
                    0,
                    0);
    spu_issue_group(i, 0, 0);
    spulib_wait(i, 1);
  }
#endif

  IF_CHECK(run_done_check());
}
