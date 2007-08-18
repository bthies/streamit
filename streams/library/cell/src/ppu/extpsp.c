#include "spulibint.h"

#define PHASE_ALLOC   0
#define PHASE_LOAD    1
#define PHASE_INIT_A  2
#define PHASE_INIT_B  3
#define PHASE_STEADY  4
#define PHASE_CLEANUP 5

typedef struct _ACTIVE_INPUT_TAPE {
  SPU_ADDRESS spu_buf_data;
  BUFFER_CB *channel_buf;
  BUFFER_CB channel_buf_instance;
} ACTIVE_INPUT_TAPE;

typedef struct _ACTIVE_OUTPUT_TAPE {
  SPU_ADDRESS spu_buf_data;
  BUFFER_CB *channel_buf;
  uint32_t ppu_dt_bytes;
  BUFFER_CB channel_buf_instance;
} ACTIVE_OUTPUT_TAPE;

typedef struct _ACTIVE_FILTER {
  uint32_t spu_id;
  EXT_PSP_EX_PARAMS *f;
  ACTIVE_INPUT_TAPE inputs[EXT_PSP_MAX_TAPES - 1];
  ACTIVE_OUTPUT_TAPE outputs[EXT_PSP_MAX_TAPES - 1];
  SPU_ADDRESS filt_cb;
  uint32_t cmd_id_start;
  uint32_t group_num_cmd;
  bool_t load_filter;
  bool_t unload_filter;
  struct {
    SPU_CMD_GROUP *g;
    SPU_ADDRESS cmd_data;
  } groups[2];
  uint32_t groups_left;
  uint32_t last_group_iters;
  uint32_t current_group;
  uint32_t cmd_mask;
  uint32_t phase;
  uint32_t waiting_mask;
  uint32_t completed_mask;
} ACTIVE_FILTER;

static void setup_load(ACTIVE_FILTER *af, EXT_PSP_EX_LAYOUT *l);
static void setup_init_a(ACTIVE_FILTER *af);
static void setup_init_b(ACTIVE_FILTER *af);
static void setup_steady(ACTIVE_FILTER *af, uint32_t group);
static void setup_cleanup_a(ACTIVE_FILTER *af, uint32_t group);
static void setup_cleanup_b(ACTIVE_FILTER *af, uint32_t group);

static void adjust_input(ACTIVE_FILTER *af, uint32_t group);

static bool_t ext_psp_ex_handler(void *data, uint32_t mask);
static void start_input(ACTIVE_FILTER *af);
static void start_output(ACTIVE_FILTER *af);
static void finish_output(ACTIVE_FILTER *af);
static void done_filter(ACTIVE_FILTER *af);

static INLINE void
issue_group(ACTIVE_FILTER *af, uint32_t group)
{
  spu_issue_int_group(af->groups[group].g, af->groups[group].cmd_data);
}

static INLINE uint32_t
get_mask(uint32_t n)
{
  return (1 << n) - 1;
}

static INLINE uint32_t
get_alloc_mask(ACTIVE_FILTER *af)
{
  return
    get_mask(af->f->num_inputs) << (1 + af->group_num_cmd + af->cmd_id_start);
}

static INLINE uint32_t
get_load_mask(ACTIVE_FILTER *af)
{
  return ((get_mask(af->group_num_cmd) ^ !af->load_filter) |
          (get_mask(af->f->num_outputs) <<
           (1 + af->f->num_inputs + af->group_num_cmd)))
    << af->cmd_id_start;
}

static INLINE uint32_t
get_init_a_mask(ACTIVE_FILTER *af)
{
  return
    get_mask(af->f->num_inputs) << (1 + af->group_num_cmd + af->cmd_id_start);
}

static INLINE uint32_t
get_init_b_mask(ACTIVE_FILTER *af)
{
  return get_mask(1 + af->f->num_inputs) << af->cmd_id_start;
}

static INLINE uint32_t
get_steady_0_mask(ACTIVE_FILTER *af)
{
  return get_mask(af->group_num_cmd) << af->cmd_id_start;
}

static INLINE uint32_t
get_cleanup_a_mask(ACTIVE_FILTER *af, uint32_t group)
{
  return (1 | (get_mask(af->f->num_outputs) << (1 + af->f->num_inputs))) <<
    (af->cmd_id_start + (group * af->group_num_cmd));
}

static INLINE uint32_t
get_cleanup_b_mask(ACTIVE_FILTER *af, uint32_t group)
{
  EXT_PSP_EX_PARAMS *f = af->f;
  uint32_t mask = get_mask(f->num_outputs) << (1 + f->num_inputs);

  if (af->unload_filter || (CHECK && af->load_filter)) {
    mask |= 1;
  }

  return mask << (af->cmd_id_start + (group * af->group_num_cmd));
}

static INLINE uint32_t
get_cleanup_mask(ACTIVE_FILTER *af, uint32_t group)
{
  return get_cleanup_a_mask(af, group ^ 1) | get_cleanup_b_mask(af, group);
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
  SPU_CMD_HEADER *run_cmd;

  run_cmd = spu_filter_run(g,
                           af->filt_cb, iters,
                           cmd_id,
                           0);
  run_cmd->num_back_deps = af->group_num_cmd;

  for (uint32_t i = 0; i < af->group_num_cmd; i++) {
    run_cmd->deps[i] = dep_start + i;
  }
}

static INLINE void
setup_input(SPU_CMD_GROUP *g, ACTIVE_FILTER *af, uint32_t iters,
            uint32_t cmd_id_start, uint32_t input_dep_start, uint32_t run_dep)
{
  EXT_PSP_EX_PARAMS *f = af->f;

  for (uint32_t i = 0; i < f->num_inputs; i++) {
    spu_dt_in_back_ppu_ex(g,
                          af->inputs[i].spu_buf_data,
                          af->inputs[i].channel_buf,
                          iters * f->inputs[i].pop_bytes,
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
  EXT_PSP_EX_PARAMS *f = af->f;

  for (uint32_t i = 0; i < f->num_outputs; i++) {
    spu_dt_out_front_ppu_ex(g,
                            af->outputs[i].spu_buf_data,
                            af->outputs[i].channel_buf,
                            iters * f->outputs[i].push_bytes,
                            f->data_parallel,
                            cmd_id_start + i,
                            2,
                            output_dep_start + i,
                            run_dep);
  }
}

void
ext_ppu_spu_ppu_ex(EXT_PSP_EX_LAYOUT *l, EXT_PSP_EX_PARAMS *f,
                   BUFFER_CB *ppu_in_buf, BUFFER_CB *ppu_out_buf,
                   uint32_t iters, GENERIC_COMPLETE_CB *cb, uint32_t tag)
{
  SPU_INFO *spu = &spu_info[l->spu_id];
  ACTIVE_FILTER *af;
  SPU_ADDRESS input_buf_start;
  SPU_ADDRESS output_buf_start;
  uint32_t group_num_cmd;
  uint32_t cmd_mask;

  if (iters == 0) {
    (*cb)(tag);
    return;
  }

  group_num_cmd = 1 + f->num_inputs + f->num_outputs;
  pcheck((f->num_inputs + f->num_outputs <= EXT_PSP_MAX_TAPES) &&
         (l->cmd_id_start <= SPU_MAX_COMMANDS - group_num_cmd * 2) &&
         ((l->cmd_data_start & CACHE_MASK) == 0));

  cmd_mask = get_mask(group_num_cmd * 2) << l->cmd_id_start;
  af = spu_new_ext_op(spu, cmd_mask, &ext_psp_ex_handler, cb, tag,
                      sizeof(*af));

  af->spu_id = l->spu_id;
  af->f = f;
  af->group_num_cmd = group_num_cmd;
  af->cmd_mask = cmd_mask;
  af->load_filter = l->load_filter;
  af->unload_filter = af->load_filter &&
    (SPU_STATS_ENABLE || ((l->desc->state_size != 0) && !f->data_parallel));

  af->groups_left = (iters - 1) / f->group_iters + 1;
  af->last_group_iters = (iters - 1) % f->group_iters + 1;

  input_buf_start = l->in_buf_start + CACHE_SIZE;
  output_buf_start = l->out_buf_start + CACHE_SIZE;
  af->filt_cb = l->filt_cb;
  af->cmd_id_start = l->cmd_id_start;

  // Input bookkeeping
  for (uint32_t i = 0; i < f->num_inputs; i++) {
    ACTIVE_INPUT_TAPE *ait = &af->inputs[i];

    ait->spu_buf_data = input_buf_start;
    input_buf_start += f->inputs[i].spu_buf_size + CACHE_SIZE;

    if (f->data_parallel) {
      duplicate_buffer(&ait->channel_buf_instance, &ppu_in_buf[i]);
      ait->channel_buf = &ait->channel_buf_instance;

      buf_inc_head(&ppu_in_buf[i], iters * f->inputs[i].pop_bytes);
    } else {
      ait->channel_buf = &ppu_in_buf[i];
    }
  }

  // Output bookkeeping
  for (uint32_t i = 0; i < f->num_outputs; i++) {
    ACTIVE_OUTPUT_TAPE *aot = &af->outputs[i];

    aot->spu_buf_data = output_buf_start;
    output_buf_start += f->outputs[i].spu_buf_size + CACHE_SIZE;

    if (f->data_parallel) {
      duplicate_buffer(&aot->channel_buf_instance, &ppu_out_buf[i]);
      aot->channel_buf = &aot->channel_buf_instance;

      buf_inc_tail(&ppu_out_buf[i], iters * f->outputs[i].push_bytes);
    } else {
      aot->channel_buf = &ppu_out_buf[i];
    }
  }

  for (uint32_t i = 0; i < 2; i++) {
    af->groups[i].g = spu_new_int_group(spu);
    af->groups[i].cmd_data = l->cmd_data_start + i * (af->group_num_cmd * 64);
  }

  setup_load(af, l);
  issue_group(af, 0);
  setup_init_a(af);

  af->phase = PHASE_ALLOC;
  af->waiting_mask = get_alloc_mask(af);
  af->completed_mask = 0;
}

static void
setup_load(ACTIVE_FILTER *af, EXT_PSP_EX_LAYOUT *l)
{
  EXT_PSP_EX_PARAMS *f = af->f;
  SPU_CMD_GROUP *g = af->groups[0].g;
  uint32_t load_id = af->cmd_id_start + 0 + 0;
  uint32_t in_attach_id_start = load_id + 1;
  uint32_t in_alloc_id_start = in_attach_id_start + af->group_num_cmd;
  uint32_t out_alloc_id_start = in_attach_id_start + f->num_inputs;
  uint32_t out_attach_id_start = out_alloc_id_start + af->group_num_cmd;

  spu_clear_group(g);

  if (l->load_filter) {
    spu_filter_load(g,
                    af->filt_cb, l->desc,
                    load_id,
                    0);
  }

  for (uint32_t i = 0; i < f->num_inputs; i++) {
    spu_buffer_alloc(g,
                     af->inputs[i].spu_buf_data, f->inputs[i].spu_buf_size,
                     af->inputs[i].channel_buf->head & QWORD_MASK,
                     in_alloc_id_start + i,
                     0);
    spu_filter_attach_input(g,
                            af->filt_cb, i, af->inputs[i].spu_buf_data,
                            in_attach_id_start + i,
                            2,
                            load_id,
                            in_alloc_id_start + i);
  }

  for (uint32_t i = 0; i < f->num_outputs; i++) {
    spu_buffer_alloc(g,
                     af->outputs[i].spu_buf_data, f->outputs[i].spu_buf_size,
                     af->outputs[i].channel_buf->tail & QWORD_MASK,
                     out_alloc_id_start + i,
                     0);
    spu_filter_attach_output(g,
                             af->filt_cb, i, af->outputs[i].spu_buf_data,
                             out_attach_id_start + i,
                             2,
                             load_id,
                             out_alloc_id_start + i);
  }
}

static void
setup_init_a(ACTIVE_FILTER *af)
{
  EXT_PSP_EX_PARAMS *f = af->f;
  SPU_CMD_GROUP *g = af->groups[1].g;
  uint32_t input_iters =
    (af->groups_left == 1 ? af->last_group_iters : f->group_iters);
  uint32_t input_id_start = af->cmd_id_start + af->group_num_cmd + 1;

  spu_clear_group(g);

  for (uint32_t i = 0; i < f->num_inputs; i++) {
    spu_dt_in_back_ppu_ex(g,
                          af->inputs[i].spu_buf_data,
                          af->inputs[i].channel_buf,
                          f->inputs[i].peek_extra_bytes +
                            input_iters * f->inputs[i].pop_bytes,
                          input_id_start + i,
                          0);
  }
}

static void
setup_init_b(ACTIVE_FILTER *af)
{
  EXT_PSP_EX_PARAMS *f = af->f;
  SPU_CMD_GROUP *g = af->groups[0].g;
  uint32_t id_start = af->cmd_id_start + 0;
  uint32_t dep_start = af->cmd_id_start + af->group_num_cmd;
  uint32_t input_id_start = id_start + 1;
  uint32_t input_dep_start = dep_start + 1;

  spu_clear_group(g);
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
  EXT_PSP_EX_PARAMS *f = af->f;
  SPU_CMD_GROUP *g = af->groups[group].g;
  uint32_t group_offset = group * af->group_num_cmd;
  uint32_t id_start = af->cmd_id_start + group_offset;
  uint32_t dep_start = af->cmd_id_start + (af->group_num_cmd - group_offset);
  uint32_t input_id_start = id_start + 1;
  uint32_t input_dep_start = dep_start + 1;

  if (group == 1) {
    spu_clear_group(g);
    setup_run(g, af, f->group_iters, id_start + 0, dep_start);
    setup_input(g, af,
                (af->groups_left == 3 ? af->last_group_iters : f->group_iters),
                input_id_start, input_dep_start, dep_start + 0);
  }

  setup_output(g, af, f->group_iters, input_id_start + f->num_inputs,
               input_dep_start + f->num_inputs, dep_start + 0);
}

static void
setup_cleanup_a(ACTIVE_FILTER *af, uint32_t group)
{
  EXT_PSP_EX_PARAMS *f = af->f;
  SPU_CMD_GROUP *g = af->groups[group].g;
  uint32_t group_offset = group * af->group_num_cmd;
  uint32_t id_start = af->cmd_id_start + group_offset;
  uint32_t dep_start = af->cmd_id_start + (af->group_num_cmd - group_offset);

  spu_clear_group(g);
  setup_run(g, af, af->last_group_iters, id_start + 0, dep_start);
  setup_output(g, af, f->group_iters, id_start + 1 + f->num_inputs,
               dep_start + 1 + f->num_inputs, dep_start + 0);
}

static void
setup_cleanup_b(ACTIVE_FILTER *af, uint32_t group)
{
  EXT_PSP_EX_PARAMS *f = af->f;
  SPU_CMD_GROUP *g = af->groups[group].g;
  uint32_t group_offset = group * af->group_num_cmd;
  uint32_t id_start = af->cmd_id_start + group_offset;
  uint32_t dep_start = af->cmd_id_start + (af->group_num_cmd - group_offset);

  spu_clear_group(g);

  if (af->unload_filter) {
    spu_filter_unload(g,
                      af->filt_cb,
                      id_start + 0,
                      1,
                      dep_start + 0);
  } else {
#if CHECK
    if (af->load_filter) {
      spu_filter_detach_all(g,
                            af->filt_cb,
                            id_start + 0,
                            1,
                            dep_start + 0);
    }
#endif
  }

  setup_output(g, af, af->last_group_iters, id_start + 1 + f->num_inputs,
               dep_start + 1 + f->num_inputs, dep_start + 0);
}

static void
adjust_input(ACTIVE_FILTER *af, uint32_t group)
{
  EXT_PSP_EX_PARAMS *f = af->f;
  SPU_DT_IN_BACK_CMD *cmd;

  cmd = (SPU_DT_IN_BACK_CMD *)((void *)spu_first_command(af->groups[group].g) +
                               sizeof(SPU_FILTER_RUN_CMD));

  for (uint32_t i = 0; i < f->num_inputs; i++) {
    cmd->num_bytes = af->last_group_iters * f->inputs[i].pop_bytes;
    cmd++;
  }
}

static bool_t
ext_psp_ex_handler(void *data, uint32_t mask)
{
  ACTIVE_FILTER *af = (ACTIVE_FILTER *)data;
  EXT_PSP_EX_PARAMS *f = af->f;

  af->completed_mask |= mask;

  while ((af->completed_mask & af->waiting_mask) == af->waiting_mask) {
    af->completed_mask &= ~af->waiting_mask;

    switch (af->phase) {
    case PHASE_ALLOC:
      start_input(af);

      issue_group(af, 1); // init_a
      setup_init_b(af);

      af->phase = PHASE_LOAD;
      af->waiting_mask = get_load_mask(af);
      break;

    case PHASE_LOAD:
      start_output(af);

      issue_group(af, 0); // init_b

      af->phase = PHASE_INIT_A;
      af->waiting_mask = get_init_a_mask(af);
      break;

    case PHASE_INIT_A:
      if (af->groups_left == 1) {
        setup_cleanup_b(af, 1); // issues cleanup_b

        af->phase = PHASE_CLEANUP;
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

      af->phase = PHASE_STEADY;
      af->waiting_mask = get_steady_0_mask(af);

    case PHASE_STEADY:
      af->groups_left--;
      af->waiting_mask ^= af->cmd_mask;

      switch (af->groups_left) {
      case 2:
        if (af->last_group_iters != f->group_iters) {
          adjust_input(af, af->current_group); // issues modified steady
        }

        break;
        
      case 1:
        setup_cleanup_a(af, af->current_group); // issues cleanup_a
        break;

      case 0:
        setup_cleanup_b(af, af->current_group); // issues cleanup_b

        af->phase = PHASE_CLEANUP;
        af->waiting_mask = get_cleanup_mask(af, af->current_group);
        break;
      }

      issue_group(af, af->current_group);
      af->current_group ^= 1;
      break;

    case PHASE_CLEANUP:
      finish_output(af);
      done_filter(af);
      return TRUE;

    default:
      unreached();
    }
  }

  return FALSE;
}

static void
start_input(ACTIVE_FILTER *af)
{
  EXT_PSP_EX_PARAMS *f = af->f;
  uint32_t iters;

  iters = (af->groups_left - 1) * f->group_iters + af->last_group_iters;

  for (uint32_t i = 0; i < f->num_inputs; i++) {
    dt_out_front_ex(af->inputs[i].channel_buf, af->spu_id,
                    af->inputs[i].spu_buf_data,
                    f->inputs[i].peek_extra_bytes +
                      iters * f->inputs[i].pop_bytes);
  }
}

static void
start_output(ACTIVE_FILTER *af)
{
  EXT_PSP_EX_PARAMS *f = af->f;
  uint32_t iters;
  uint32_t output_id_start;

  iters = (af->groups_left - 1) * f->group_iters + af->last_group_iters;
  output_id_start = af->cmd_id_start + af->group_num_cmd + (1 + f->num_inputs);

  for (uint32_t i = 0; i < f->num_outputs; i++) {
    uint32_t dt_bytes = iters * f->outputs[i].push_bytes;
    dt_in_back_ex(af->outputs[i].channel_buf, af->spu_id,
                  af->outputs[i].spu_buf_data, dt_bytes, f->data_parallel,
                  output_id_start + i);
    af->outputs[i].ppu_dt_bytes = dt_bytes;
  }
}

static void
finish_output(ACTIVE_FILTER *af)
{
  if (af->f->data_parallel) {
    finish_dt_in_back_ex(af->outputs[0].channel_buf,
                         af->outputs[0].ppu_dt_bytes);
  }
}

static void
done_filter(ACTIVE_FILTER *af)
{
  EXT_PSP_EX_PARAMS *f = af->f;

  if (!f->data_parallel) {
    for (uint32_t i = 0; i < f->num_inputs; i++) {
      buf_inc_head(af->inputs[i].channel_buf, -f->inputs[i].peek_extra_bytes);
    }
  }

  spu_free_int_group(af->groups[0].g);
  spu_free_int_group(af->groups[1].g);
}
