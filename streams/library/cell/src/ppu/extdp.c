/*-----------------------------------------------------------------------------
 * extdp.c
 *
 * Implementation of data_parallel/dp and data_parallel_shared/dps library
 * operations.
 *---------------------------------------------------------------------------*/

#include "spulibint.h"

typedef struct _EXT_DP_DATA {
  GENERIC_COMPLETE_CB *cb;
  uint32_t tag;
  void *spu_ops[NUM_SPU];
  uint32_t busy_spu;
} EXT_DP_DATA;

static void ext_dp_wait_spu(uint32_t tag);

/*-----------------------------------------------------------------------------
 * ext_data_parallel
 *---------------------------------------------------------------------------*/

void *
ext_data_parallel(uint32_t num_spu, EXT_SPU_LAYOUT *l, EXT_SPU_RATES *r,
                  uint32_t iters, GENERIC_COMPLETE_CB *cb, uint32_t tag)
{
  EXT_DP_DATA *d;

  pcheck((num_spu != 0) && (num_spu <= NUM_SPU));

  d = (EXT_DP_DATA *)malloc(sizeof(*d));
  check(d != NULL);
  d->cb = cb;
  d->tag = tag;
  d->busy_spu = num_spu;

  for (uint32_t i = 0; i < num_spu; i++) {
    d->spu_ops[i] = ext_ppu_spu_ppu(&l[i], r, iters, &ext_dp_wait_spu,
                                    (uintptr_t)d);
  }

  return d;
}

/*-----------------------------------------------------------------------------
 * ext_dp_wait_spu
 *
 * Collects completions of individual SPU operations.
 *---------------------------------------------------------------------------*/

static void
ext_dp_wait_spu(uint32_t tag)
{
  EXT_DP_DATA *d = (EXT_DP_DATA *)tag;
  
  if (--d->busy_spu == 0) {
    GENERIC_COMPLETE_CB *cb = d->cb;
    uint32_t cb_tag = d->tag;

    free(d);
    (*cb)(cb_tag);
  }
}

/*-----------------------------------------------------------------------------
 * ext_dp_notify_input
 *---------------------------------------------------------------------------*/

void
ext_dp_notify_input(void *op, uint32_t index)
{
  EXT_DP_DATA *d = (EXT_DP_DATA *)op;
  ext_spu_notify_input(d->spu_ops[index]);
}

/*-----------------------------------------------------------------------------
 * ext_dp_notify_output
 *---------------------------------------------------------------------------*/

void
ext_dp_notify_output(void *op, uint32_t index)
{
  EXT_DP_DATA *d = (EXT_DP_DATA *)op;
  ext_spu_notify_output(d->spu_ops[index]);
}

typedef struct _EXT_DPS_DATA {
  GENERIC_COMPLETE_CB *cb;
  uint32_t tag;
  EXT_SPU_DATA *spu_ops[NUM_SPU];
  uint32_t num_spu;
  struct {
    uint32_t count;
    bool_t waiting;
    uint32_t index;
  } in, out;
  uint32_t busy_spu;
} EXT_DPS_DATA;

static void ext_dps_wait_spu(uint32_t tag);
static void ext_dps_handle_dt(void *data, uint32_t msg);

/*-----------------------------------------------------------------------------
 * ext_data_parallel_shared
 *---------------------------------------------------------------------------*/

void *
ext_data_parallel_shared(uint32_t num_spu, EXT_SPU_LAYOUT *l,
                         void *remote_in_buf_data, void *remote_out_buf_data,
                         EXT_SPU_RATES *r, uint32_t iters,
                         GENERIC_COMPLETE_CB *cb, uint32_t tag)
{
  EXT_DPS_DATA *d;
  EXT_SPU_INT_PARAMS ip;
  bool_t same_layout;
  uint32_t spu_iters[NUM_SPU];

  pcheck((num_spu <= NUM_SPU) && (iters != 0));

  d = (EXT_DPS_DATA *)malloc(sizeof(*d));
  check(d != NULL);
  d->cb = cb;
  d->tag = tag;

  if ((same_layout = (num_spu == 0))) {
    num_spu = (iters >= NUM_SPU ? NUM_SPU : iters);

    l->remote_in_buf_data = remote_in_buf_data;
    l->remote_out_buf_data = remote_out_buf_data;
  } else {
    pcheck(iters >= num_spu);
  }

#if CHECK_PARAMS
  // Make sure rates don't cause misalignment.
  if (iters > num_spu) {
    pcheck((((r->in_bytes * (num_spu - 1)) & CACHE_MASK) == 0) &&
           (((r->out_bytes * (num_spu - 1)) & CACHE_MASK) == 0));
  }
#endif

  d->num_spu = num_spu;
  d->busy_spu = num_spu;

  // Initialize data transfer state.
  d->in.count = iters;
  d->out.count = iters;
  d->in.waiting = TRUE;
  d->out.waiting = TRUE;
  d->in.index = 0;
  d->out.index = 0;

  // Calculate iterations.
  spu_iters[0] = iters / num_spu;

  for (uint32_t i = 1; i < num_spu; i++) {
    spu_iters[i] = spu_iters[0];
  }

  for (uint32_t rem_iters = iters - spu_iters[0] * num_spu, i = 0;
       i < rem_iters; i++) {
    spu_iters[i]++;
  }

  ip.dt_cb = &ext_dps_handle_dt;
  ip.dt_cb_data = d;

  // Start individual SPU operations.
  for (uint32_t i = 0; i < num_spu; i++) {
    EXT_SPU_LAYOUT *sl = (same_layout ? l : &l[i]);

    if (same_layout) {
      sl->spu_id = i;
    } else {
      sl->remote_in_buf_data = remote_in_buf_data;
      sl->remote_out_buf_data = remote_out_buf_data;
    }

    d->spu_ops[i] = ext_ppu_spu_ppu_internal(sl, r, spu_iters[i], &ip,
                                             &ext_dps_wait_spu, (uintptr_t)d);
  }

  // Start data transfers.
  if (!EXT_ALLOW_SPU_NO_INPUT || (r->in_bytes != 0)) {
    ext_dps_notify_input(d);
  }

  if (!EXT_ALLOW_SPU_NO_OUTPUT || (r->out_bytes != 0)) {
    ext_dps_notify_output(d);
  }

  return d;
}

/*-----------------------------------------------------------------------------
 * ext_dps_wait_spu
 *
 * Collects completions of individual SPU operations.
 *---------------------------------------------------------------------------*/

static void
ext_dps_wait_spu(uint32_t tag)
{
  EXT_DPS_DATA *d = (EXT_DPS_DATA *)tag;
  
  if (--d->busy_spu == 0) {
    GENERIC_COMPLETE_CB *cb = d->cb;
    uint32_t cb_tag = d->tag;

    free(d);
    (*cb)(cb_tag);
  }
}

/*-----------------------------------------------------------------------------
 * ext_dps_handle_dt
 *
 * Processes data transfer completion notifications from individual SPU
 * operations.
 *---------------------------------------------------------------------------*/

static void
ext_dps_handle_dt(void *data, uint32_t msg)
{
  EXT_DPS_DATA *d = (EXT_DPS_DATA *)data;
  EXT_SPU_DATA *spu_op;

  if (msg == EXT_SPU_DONE_DT_IN) {
    if (--d->in.count != 0) {
      // Switch to next SPU.
      if (++d->in.index == d->num_spu) {
        d->in.index = 0;
      }

      // Start transfer if data is available.
      if (ext_spu_in_buf_has_data(spu_op = d->spu_ops[d->in.index])) {
        ext_spu_start_dt_in(spu_op);
      } else {
        d->in.waiting = TRUE;
      }
    }
  } else {
    if (--d->out .count != 0) {
      // Switch to next SPU.
      if (++d->out .index == d->num_spu) {
        d->out .index = 0;
      }

      // Start transfer if space is available.
      if (ext_spu_out_buf_has_space(spu_op = d->spu_ops[d->out .index])) {
        ext_spu_start_dt_out(spu_op);
      } else {
        d->out .waiting = TRUE;
      }
    }
  }
}

/*-----------------------------------------------------------------------------
 * ext_dps_notify_input
 *---------------------------------------------------------------------------*/

void
ext_dps_notify_input(void *op)
{
  EXT_DPS_DATA *d = (EXT_DPS_DATA *)op;
  EXT_SPU_DATA *spu_op;

  if (d->in.waiting &&
      ext_spu_in_buf_has_data(spu_op = d->spu_ops[d->in.index])) {
    d->in.waiting = FALSE;
    ext_spu_start_dt_in(spu_op);
  }
}

/*-----------------------------------------------------------------------------
 * ext_dps_notify_output
 *---------------------------------------------------------------------------*/

void
ext_dps_notify_output(void *op)
{
  EXT_DPS_DATA *d = (EXT_DPS_DATA *)op;
  EXT_SPU_DATA *spu_op;

  if (d->out.waiting &&
      ext_spu_out_buf_has_space(spu_op = d->spu_ops[d->out.index])) {
    d->out.waiting = FALSE;
    ext_spu_start_dt_out(spu_op);
  }
}
