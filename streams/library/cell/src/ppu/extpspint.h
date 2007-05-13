/*-----------------------------------------------------------------------------
 * extpspint.h
 *
 * Internal declarations for ppu_spu_ppu/psp library operation. Automatically
 * included by spulibint.h.
 *---------------------------------------------------------------------------*/

#ifndef _EXT_PSP_INT_H_
#define _EXT_PSP_INT_H_

// Callback after each data transfer completes.
typedef void EXT_PSP_DT_CB(void *data, uint32_t msg);

// Notification types for callback.
#define EXT_PSP_DONE_DT_IN  0
#define EXT_PSP_DONE_DT_OUT 1

// Optional internal parameters (used to implement data_parallel_shared).
typedef struct _EXT_PSP_INT_PARAMS {
  // Callback to run after each data transfer completes. When this is
  // specified, transfers are never *started* by psp (completions are still
  // processed).
  EXT_PSP_DT_CB *dt_cb;
  void *dt_cb_data;
} EXT_PSP_INT_PARAMS;

// Internal state.
typedef struct _EXT_PSP_DATA {
  EXT_PSP_LAYOUT l;
  EXT_PSP_RATES r;
  EXT_PSP_INT_PARAMS ip;
  struct {
    SPU_CMD_GROUP *g;
    SPU_ADDRESS da;
  } slots[2];
  struct {
    uint32_t count;
    bool_t waiting;
    uint32_t slot;
    uint32_t cmd_bit;
    uint32_t flip_cmd_bit;
  } in, out;
  uint32_t steady_iters;
  uint32_t phase;
  uint32_t cur_slot;
  uint32_t waiting_mask;
  uint32_t flip_waiting_mask;
  uint32_t completed_mask;
} EXT_PSP_DATA;

EXT_PSP_DATA *ext_ppu_spu_ppu_internal(EXT_PSP_LAYOUT *l, EXT_PSP_RATES *r,
                                       uint32_t iters, EXT_PSP_INT_PARAMS *ip,
                                       GENERIC_COMPLETE_CB *cb, uint32_t tag);

/*-----------------------------------------------------------------------------
 * ext_psp_in_buf_has_data
 *
 * Returns whether the PPU input buffer contains enough data to start another
 * transfer into the SPU.
 *---------------------------------------------------------------------------*/

static INLINE bool_t
ext_psp_in_buf_has_data(EXT_PSP_DATA *d)
{
  return buf_bytes_used(buf_get_cb(d->l.ppu_in_buf_data)) >= d->r.in_bytes;
}

/*-----------------------------------------------------------------------------
 * ext_psp_out_buf_has_space
 *
 * Returns whether the PPU output buffer has enough space to start another
 * transfer out of the SPU.
 *---------------------------------------------------------------------------*/

static INLINE bool_t
ext_psp_out_buf_has_space(EXT_PSP_DATA *d)
{
  BUFFER_CB *buf = buf_get_cb(d->l.ppu_out_buf_data);
  return ((buf->head - (buf->head & QWORD_MASK ? : 1) - buf->tail) &
          buf->mask) >= d->r.out_bytes;
}

/*-----------------------------------------------------------------------------
 * ext_psp_start_dt_in
 *
 * Starts a transfer into the SPU. Caller must make sure this is called at the
 * correct time.
 *---------------------------------------------------------------------------*/

static INLINE void
ext_psp_start_dt_in(EXT_PSP_DATA *d)
{
  dt_out_front(d->l.ppu_in_buf_data, d->l.spu_id, d->l.spu_in_buf_data,
               d->r.in_bytes, d->l.cmd_id + d->in.slot * 3 + 0, 0);
}

/*-----------------------------------------------------------------------------
 * ext_psp_start_dt_out
 *
 * Starts a transfer out of the SPU. Caller must make sure this is called at
 * the correct time.
 *---------------------------------------------------------------------------*/

static INLINE void
ext_psp_start_dt_out(EXT_PSP_DATA *d)
{
  dt_in_back(d->l.ppu_out_buf_data, d->l.spu_id, d->l.spu_out_buf_data,
             d->r.out_bytes, d->l.cmd_id + d->out.slot * 3 + 2, 0);
}

#endif
