/*-----------------------------------------------------------------------------
 * extpsp.c
 *
 * Implementation for ppu_spu_ppu/psp library operation.
 *---------------------------------------------------------------------------*/

#include "spulibint.h"

// Indicators for which type of iteration is currently executing on SPU.
#define PHASE_INIT_0  0   // i
#define PHASE_INIT_1  1   // i r
#define PHASE_STEADY  2   // i r o
                          //  ...
#define PHASE_CLEANUP 3   //   r o
                          //     o

static bool_t ext_psp_handler(EXT_PSP_DATA *d, uint32_t mask);

static void ext_psp_setup_init(EXT_PSP_DATA *d);
static void ext_psp_setup_steady(EXT_PSP_DATA *d, uint32_t slot);
static void ext_psp_setup_cleanup_0(EXT_PSP_DATA *d);
static uint32_t ext_psp_setup_cleanup_1(EXT_PSP_DATA *d);
static uint32_t ext_psp_setup_full(EXT_PSP_DATA *d, uint32_t iters);

/*-----------------------------------------------------------------------------
 * ext_ppu_spu_ppu
 *---------------------------------------------------------------------------*/

void *
ext_ppu_spu_ppu(EXT_PSP_LAYOUT *l, EXT_PSP_RATES *r, uint32_t iters,
                GENERIC_COMPLETE_CB *cb, uint32_t tag)
{
  return ext_ppu_spu_ppu_internal(l, r, iters, NULL, cb, tag);
}

/*-----------------------------------------------------------------------------
 * ext_ppu_spu_ppu_internal
 *
 * Internal implementation has extra parameters (used to implement
 * data_parallel_shared).
 *---------------------------------------------------------------------------*/

EXT_PSP_DATA *
ext_ppu_spu_ppu_internal(EXT_PSP_LAYOUT *l, EXT_PSP_RATES *r, uint32_t iters,
                         EXT_PSP_INT_PARAMS *ip, GENERIC_COMPLETE_CB *cb,
                         uint32_t tag)
{
  EXT_PSP_DATA *d;
  SPU_INFO *spu = &spu_info[l->spu_id];

#if !EXT_ALLOW_PSP_NO_INPUT
  pcheck(r->in_bytes != 0);
#endif
#if !EXT_ALLOW_PSP_NO_OUTPUT
  pcheck(r->out_bytes != 0);
#endif
  // If no data is transferred, why are you calling this anyway?
  pcheck((r->in_bytes != 0) || (r->out_bytes != 0));
  pcheck(iters != 0);

  d = (EXT_PSP_DATA *)spu_new_ext_op(spu, (0x3f << l->cmd_id),
                                     &ext_psp_handler, cb, tag, sizeof(*d));
  d->l = *l;
  d->r = *r;

  if (ip == NULL) {
    d->ip.dt_cb = NULL;
  } else {
    d->ip = *ip;
  }

  // Allocate command groups.
  for (uint32_t i = 0; i < 2; i++) {
    d->slots[i].g = spu_new_int_group(spu);
    d->slots[i].da = l->da + i * 256;
  }

  // Initialize data transfer state.
  d->in.count = iters;
  d->out.count = iters;
  d->in.waiting = TRUE;
  d->out.waiting = TRUE;
  d->in.slot = 0;
  d->out.slot = 0;
  d->in.cmd_bit = (1 << l->cmd_id);           // 0
  d->out.cmd_bit = (4 << l->cmd_id);          // 2
  d->in.flip_cmd_bit = (9 << l->cmd_id);      // 0 and 3
  d->out.flip_cmd_bit = (0x24 << l->cmd_id);  // 2 and 5

  d->cur_slot = 0;
  d->completed_mask = 0;

  if (iters <= 2) {
    // Issue all commands to SPU and just wait for all of them.
    d->waiting_mask = ext_psp_setup_full(d, iters);
    d->phase = PHASE_CLEANUP;

    spu_issue_int_group(d->slots[0].g, d->slots[0].da);
  } else {
    // At least one steady state iteration.

    d->steady_iters = iters - 2;

    // Set up init 0/1 groups in slot 1 and slot 0 steady state group, issue
    // init slot.
    ext_psp_setup_init(d);
    ext_psp_setup_steady(d, 0);

    spu_issue_int_group(d->slots[1].g, d->slots[1].da);

    if (EXT_ALLOW_PSP_NO_INPUT && (r->in_bytes == 0)) {
      // No input (must have output) - only init command is run (ID 4), can
      // issue slot 0 steady state group.

      d->flip_waiting_mask = (0x36 << l->cmd_id);     //   1,2 <->   4,5

      d->phase = PHASE_INIT_1;
      d->waiting_mask = (0x10 << l->cmd_id);
      d->cur_slot = 1;

      spu_issue_int_group(d->slots[0].g, d->slots[0].da);
    } else {
      if (EXT_ALLOW_PSP_NO_OUTPUT && (r->out_bytes == 0)) {
        // No output, must have input.
        d->flip_waiting_mask = (0x1b << l->cmd_id);   // 0,1   <-> 3,4
      } else {
        // Input and output.
        d->flip_waiting_mask = (0x3f << l->cmd_id);   // 0,1,2 <-> 3,4,5
      }

      d->phase = PHASE_INIT_0;
      d->waiting_mask = (1 << l->cmd_id);   // init 0's dt_in, ID 0
    }
  }

  // Start data transfers.
  if (d->ip.dt_cb == NULL) {
    if (!EXT_ALLOW_PSP_NO_INPUT || (r->in_bytes != 0)) {
      ext_psp_notify_input(d);
    }

    if (!EXT_ALLOW_PSP_NO_OUTPUT || (r->out_bytes != 0)) {
      ext_psp_notify_output(d);
    }
  } else {
    d->in.waiting = FALSE;
    d->out.waiting = FALSE;
  }

  return d;
}

/*-----------------------------------------------------------------------------
 * ext_psp_notify_input
 *---------------------------------------------------------------------------*/

void
ext_psp_notify_input(void *op)
{
  EXT_PSP_DATA *d = (EXT_PSP_DATA *)op;

  pcheck(d->r.in_bytes != 0);

  if (d->in.waiting && ext_psp_in_buf_has_data(d)) {
    d->in.waiting = FALSE;
    ext_psp_start_dt_in(d);
  }
}

/*-----------------------------------------------------------------------------
 * ext_psp_notify_output
 *---------------------------------------------------------------------------*/

void
ext_psp_notify_output(void *op)
{
  EXT_PSP_DATA *d = (EXT_PSP_DATA *)op;

  pcheck(d->r.out_bytes != 0);

  if (d->out.waiting && ext_psp_out_buf_has_space(d)) {
    d->out.waiting = FALSE;
    ext_psp_start_dt_out(d);
  }
}

/*-----------------------------------------------------------------------------
 * ext_psp_handler
 *
 * Processes command completions for this operation.
 *---------------------------------------------------------------------------*/

static bool_t
ext_psp_handler(EXT_PSP_DATA *d, uint32_t mask)
{
  // Check for completion of data transfer into SPU.
  if ((mask & d->in.cmd_bit) != 0) {
    assert((d->r.in_bytes != 0) && !d->in.waiting);

    d->in.slot ^= 1;
    d->in.cmd_bit ^= d->in.flip_cmd_bit;

    if (d->ip.dt_cb != NULL) {
      // Stop transferring and report.
      (*d->ip.dt_cb)(d->ip.dt_cb_data, EXT_PSP_DONE_DT_IN);
    } else if (--d->in.count != 0) {
      // Start next transfer if data is available.
      if (ext_psp_in_buf_has_data(d)) {
        ext_psp_start_dt_in(d);
      } else {
        d->in.waiting = TRUE;
      }
    }
  }

  // Check for completion of data transfer out of SPU.
  if ((mask & d->out.cmd_bit) != 0) {
    assert((d->r.out_bytes != 0) && !d->out.waiting);

    d->out.slot ^= 1;
    d->out.cmd_bit ^= d->out.flip_cmd_bit;

    if (d->ip.dt_cb != NULL) {
      // Stop transferring and report.
      (*d->ip.dt_cb)(d->ip.dt_cb_data, EXT_PSP_DONE_DT_OUT);
    } else if (--d->out.count != 0) {
      // Start next transfer if space is available.
      if (ext_psp_out_buf_has_space(d)) {
        ext_psp_start_dt_out(d);
      } else {
        d->out.waiting = TRUE;
      }
    }
  }

  d->completed_mask |= mask;

  // Check for completed groups.
  while ((d->completed_mask & d->waiting_mask) == d->waiting_mask) {
    d->completed_mask &= ~d->waiting_mask;

    switch (d->phase) {
    case PHASE_INIT_0:
      // We only get here if filter takes input. Current slot is 0 - init 1
      // group is queued on SPU, slot 1 is occupied on SPU but free on PPU.
      // Slot 0 steady state group will be issued.

      assert(d->r.in_bytes != 0);
      d->phase = PHASE_INIT_1;
      // Wait on init 1 group (IDs 4 and 5).
      d->waiting_mask = (0x18 << d->l.cmd_id);
      break;

    case PHASE_INIT_1:
      // Current slot is 1 - slot 0 steady state group is queued on SPU, slot 1
      // is free.

      if (d->steady_iters == 1) {
        // Set up and issue cleanup 0 group in slot 1.
        ext_psp_setup_cleanup_0(d);

#if EXT_ALLOW_PSP_NO_OUTPUT
        // If no output, we are done when the queued/last steady state group
        // (IDs 0 and 1) and the cleanup run command (ID 4) finish.
        if (d->r.out_bytes == 0) {
          d->phase = PHASE_CLEANUP;
          d->waiting_mask = (0x13 << d->l.cmd_id);
          break;
        }
#endif
      } else {
        // Set up and issue slot 1 steady state group.
        ext_psp_setup_steady(d, 1);
      }

      d->phase = PHASE_STEADY;
#if (EXT_ALLOW_PSP_NO_INPUT || EXT_ALLOW_PSP_NO_OUTPUT)
      d->waiting_mask = d->flip_waiting_mask & (7 << d->l.cmd_id);
#else
      d->waiting_mask = (7 << d->l.cmd_id);
#endif
      break;

    case PHASE_STEADY:
      d->steady_iters--;

      if (d->steady_iters == 0) {
        // We only get here if filter has output. Cleanup 0 group is queued on
        // SPU, set up and issue cleanup 1 group, wait on all cleanup IDs.

        assert(d->r.out_bytes != 0);
        d->waiting_mask = ext_psp_setup_cleanup_1(d);
        d->phase = PHASE_CLEANUP;
      } else {
        // Steady state group in other slot is queued on SPU.

        d->waiting_mask ^= d->flip_waiting_mask;

        if (d->steady_iters == 1) {
          // Set up and issue cleanup 0 group.
          ext_psp_setup_cleanup_0(d);

#if EXT_ALLOW_PSP_NO_OUTPUT
          // If no output, we are done when the queued/last steady state group
          // and the cleanup run command (ID 2 or 4) finish.
          if (d->r.out_bytes == 0) {
            d->phase = PHASE_CLEANUP;
            d->waiting_mask |= (0x12 << d->l.cmd_id);
          }
#endif
        }
        // else steady state group in current slot will be issued again.
      }

      break;

    case PHASE_CLEANUP:
      // All done - free command groups and finish.

      spu_free_int_group(d->slots[0].g);
      spu_free_int_group(d->slots[1].g);
      return TRUE;

    default:
      unreached();
    }

    // Issue group in current slot and flip slots.
    spu_issue_int_group(d->slots[d->cur_slot].g, d->slots[d->cur_slot].da);
    d->cur_slot ^= 1;
  }

  return FALSE;
}

/*-----------------------------------------------------------------------------
 * ext_psp_setup_init
 *
 * Sets up initialization commands in slot 1 (uses IDs from both slots unless
 * filter takes no input).
 *---------------------------------------------------------------------------*/

static void
ext_psp_setup_init(EXT_PSP_DATA *d)
{
  SPU_CMD_GROUP *g = d->slots[1].g;
  uint32_t cmd_id = d->l.cmd_id;

  spu_clear_group(g);

  if (!EXT_ALLOW_PSP_NO_INPUT || (d->r.in_bytes != 0)) {
    spu_dt_in_back(g,
                   d->l.spu_in_buf_data, d->l.ppu_in_buf_data,
                   buf_get_cb(d->l.ppu_in_buf_data)->mask + 1, d->r.in_bytes,
                   cmd_id + 0,
                   0);

    spu_dt_in_back(g,
                   d->l.spu_in_buf_data, d->l.ppu_in_buf_data,
                   buf_get_cb(d->l.ppu_in_buf_data)->mask + 1, d->r.in_bytes,
                   cmd_id + 3,
                   1,
                   cmd_id + 0);
  }

  spu_filter_run(g,
                 d->l.filt, d->r.run_iters,
                 cmd_id + 4,
                 1,
                 cmd_id + 0);
}

/*-----------------------------------------------------------------------------
 * ext_psp_setup_steady
 *
 * Sets up commands for a steady state iteration in the specified slot.
 *---------------------------------------------------------------------------*/

static void
ext_psp_setup_steady(EXT_PSP_DATA *d, uint32_t slot)
{
  SPU_CMD_GROUP *g = d->slots[slot].g;
  uint32_t cmd_id = d->l.cmd_id + slot * 3;
  uint32_t dep_id = d->l.cmd_id + (slot ^ 1) * 3;

  spu_clear_group(g);

  if (!EXT_ALLOW_PSP_NO_INPUT || (d->r.in_bytes != 0)) {
    spu_dt_in_back(g,
                   d->l.spu_in_buf_data, d->l.ppu_in_buf_data,
                   buf_get_cb(d->l.ppu_in_buf_data)->mask + 1, d->r.in_bytes,
                   cmd_id + 0,
                   2,
                   dep_id + 0,
                   dep_id + 1);
  }

  spu_filter_run(g,
                 d->l.filt, d->r.run_iters,
                 cmd_id + 1,
                 3,
                 dep_id + 0,
                 dep_id + 1,
                 dep_id + 2);

  if (!EXT_ALLOW_PSP_NO_OUTPUT || (d->r.out_bytes != 0)) {
    spu_dt_out_front_ppu(g,
                         d->l.spu_out_buf_data, d->l.ppu_out_buf_data,
                         buf_get_cb(d->l.ppu_out_buf_data)->mask + 1,
                         d->r.out_bytes,
                         cmd_id + 2,
                         2,
                         dep_id + 1,
                         dep_id + 2);
  }
}

/*-----------------------------------------------------------------------------
 * ext_psp_setup_cleanup_0
 *
 * Sets up commands for the cleanup 0 iteration in the current slot.
 *---------------------------------------------------------------------------*/

static void
ext_psp_setup_cleanup_0(EXT_PSP_DATA *d)
{
  uint32_t slot = d->cur_slot;
  SPU_CMD_GROUP *g = d->slots[slot].g;
  uint32_t cmd_id = d->l.cmd_id + slot * 3;
  uint32_t dep_id = d->l.cmd_id + (slot ^ 1) * 3;

  spu_clear_group(g);
  spu_filter_run(g,
                 d->l.filt, d->r.run_iters,
                 cmd_id + 1,
                 3,
                 dep_id + 0,
                 dep_id + 1,
                 dep_id + 2);

  if (!EXT_ALLOW_PSP_NO_OUTPUT || (d->r.out_bytes != 0)) {
    spu_dt_out_front_ppu(g,
                         d->l.spu_out_buf_data, d->l.ppu_out_buf_data,
                         buf_get_cb(d->l.ppu_out_buf_data)->mask + 1,
                         d->r.out_bytes,
                         cmd_id + 2,
                         2,
                         dep_id + 1,
                         dep_id + 2);
  }
}

/*-----------------------------------------------------------------------------
 * ext_psp_setup_cleanup_1
 *
 * Sets up commands for the cleanup 1 iteration in the current slot. Returns
 * bitmap of all cleanup command IDs.
 *
 * This cannot be called if filter has no output.
 *---------------------------------------------------------------------------*/

static uint32_t
ext_psp_setup_cleanup_1(EXT_PSP_DATA *d)
{
  uint32_t slot = d->cur_slot;
  SPU_CMD_GROUP *g = d->slots[slot].g;
  uint32_t cmd_id = d->l.cmd_id + slot * 3;
  uint32_t dep_id = d->l.cmd_id + (slot ^ 1) * 3;

  assert(d->r.out_bytes != 0);
  spu_clear_group(g);
  spu_dt_out_front_ppu(g,
                       d->l.spu_out_buf_data, d->l.ppu_out_buf_data,
                       buf_get_cb(d->l.ppu_out_buf_data)->mask + 1,
                       d->r.out_bytes,
                       cmd_id + 2,
                       2,
                       dep_id + 1,
                       dep_id + 2);

  return ((6 << dep_id) | (4 << cmd_id));
}

/*-----------------------------------------------------------------------------
 * ext_psp_setup_full
 *
 * Sets up commands for 1 or 2 full iterations in slot 0 (uses both slots on
 * SPU) and returns bitmap of IDs used.
 *---------------------------------------------------------------------------*/

static uint32_t
ext_psp_setup_full(EXT_PSP_DATA *d, uint32_t iters)
{
  SPU_CMD_GROUP *g = d->slots[0].g;
  uint32_t cmd_id = d->l.cmd_id;
  uint32_t mask = 0;

  assert((iters == 1) || (iters == 2));
  spu_clear_group(g);

  if (!EXT_ALLOW_PSP_NO_INPUT || (d->r.in_bytes != 0)) {
    spu_dt_in_back(g,
                   d->l.spu_in_buf_data, d->l.ppu_in_buf_data,
                   buf_get_cb(d->l.ppu_in_buf_data)->mask + 1, d->r.in_bytes,
                   cmd_id + 0,
                   0);
    mask |= 0x11;     // 0,4
  }

  spu_filter_run(g,
                 d->l.filt, d->r.run_iters,
                 cmd_id + 4,
                 1,
                 cmd_id + 0);

  if (!EXT_ALLOW_PSP_NO_OUTPUT || (d->r.out_bytes != 0)) {
    spu_dt_out_front_ppu(g,
                         d->l.spu_out_buf_data, d->l.ppu_out_buf_data,
                         buf_get_cb(d->l.ppu_out_buf_data)->mask + 1,
                         d->r.out_bytes,
                         cmd_id + 2,
                         1,
                         cmd_id + 4);
    mask |= 0x14;     // 4,2
  }

  if (iters == 2) {
    if (!EXT_ALLOW_PSP_NO_INPUT || (d->r.in_bytes != 0)) {
      spu_dt_in_back(g,
                     d->l.spu_in_buf_data, d->l.ppu_in_buf_data,
                     buf_get_cb(d->l.ppu_in_buf_data)->mask + 1, d->r.in_bytes,
                     cmd_id + 3,
                     1,
                     cmd_id + 0);
      mask |= 0xa;    // 3,1
    }

    spu_filter_run(g,
                   d->l.filt, d->r.run_iters,
                   cmd_id + 1,
                   2,
                   cmd_id + 3,
                   cmd_id + 4);

    if (!EXT_ALLOW_PSP_NO_OUTPUT || (d->r.out_bytes != 0)) {
      spu_dt_out_front_ppu(g,
                           d->l.spu_out_buf_data, d->l.ppu_out_buf_data,
                           buf_get_cb(d->l.ppu_out_buf_data)->mask + 1,
                           d->r.out_bytes,
                           cmd_id + 5,
                           2,
                           cmd_id + 1,
                           cmd_id + 2);
      mask |= 0x22;   // 1,5
    }
  }

  return (mask << cmd_id);
}
