/*-----------------------------------------------------------------------------
 * dt.c
 *
 * PPU data transfer implementation.
 *---------------------------------------------------------------------------*/

#include "spulibint.h"

/*-----------------------------------------------------------------------------
 * "Normal"/dataflow direction.
 *---------------------------------------------------------------------------*/

/*-----------------------------------------------------------------------------
 * dt_out_front
 *---------------------------------------------------------------------------*/
void
dt_out_front(BUFFER_CB *buf, uint32_t dest_spu, SPU_ADDRESS dest_buf_data,
             uint32_t num_bytes, uint32_t spu_cmd_id, uint32_t tag)
{
  OUT_DTCB out_dtcb;
  PPU_DT_PARAMS *cmd;

#if CHECK
  // Validate alignment.
  pcheck(((dest_buf_data & CACHE_MASK) == 0) && (num_bytes != 0));

  // Debug head pointer should be synchronized.
  assert(buf->ihead == buf->head);
  // Make sure front of buffer is free. out-out is disallowed.
  check((buf->front_action == BUFFER_ACTION_NONE) &&
        (buf->back_action != BUFFER_ACTION_OUT));
  buf->front_action = BUFFER_ACTION_OUT;
  // Make sure enough data is available.
  check(((buf->tail - buf->head) & buf->mask) >= num_bytes);
#endif

  // Set up source buffer's head/tail pointers for region to be transferred.
  out_dtcb.head = buf->head;
  out_dtcb.tail = (buf->head + num_bytes) & buf->mask;
  IF_CHECK(buf->ihead = out_dtcb.tail);

  // Set data transfer to wait on the SPU command ID. This must be done before
  // writing transfer info to SPU.
  cmd = ppu_dt_wait_spu(dest_spu, spu_cmd_id, tag);
  cmd->type = PPU_CMD_DT_OUT_FRONT;
  cmd->buf = buf;
  cmd->num_bytes = num_bytes;

  // Write transfer info to destination SPU.
  write64((uint64_t *)buf_get_dt_field_addr(spu_addr(dest_spu, dest_buf_data),
                                            back_in_dtcb),
          out_dtcb.data);
}

/*-----------------------------------------------------------------------------
 * ppu_finish_dt_out_front
 *---------------------------------------------------------------------------*/
static INLINE void
ppu_finish_dt_out_front(PPU_DT_PARAMS *cmd)
{
  BUFFER_CB *buf = cmd->buf;

  buf->head = (buf->head + cmd->num_bytes) & buf->mask;
  IF_CHECK(buf->front_action = BUFFER_ACTION_NONE);
}

/*-----------------------------------------------------------------------------
 * dt_out_front_ex
 *---------------------------------------------------------------------------*/
void
dt_out_front_ex(BUFFER_CB *buf, uint32_t dest_spu, SPU_ADDRESS dest_buf_data,
                uint32_t num_bytes)
{
  OUT_DTCB out_dtcb;

#if CHECK
  // Validate alignment.
  pcheck(((dest_buf_data & CACHE_MASK) == 0) && (num_bytes != 0));

  // Debug head pointer should be synchronized.
  assert(buf->ihead == buf->head);
  // Make sure front of buffer is free. out-out is disallowed.
  check((buf->front_action == BUFFER_ACTION_NONE) &&
        (buf->back_action != BUFFER_ACTION_OUT));
  // Make sure enough data is available.
  check(((buf->tail - buf->head) & buf->mask) >= num_bytes);
#endif

  // Set up source buffer's head/tail pointers for region to be transferred.
  out_dtcb.head = buf->head;
  out_dtcb.tail = (buf->head + num_bytes) & buf->mask;
  buf->head = out_dtcb.tail;
  IF_CHECK(buf->ihead = buf->head);

  // Write transfer info to destination SPU.
  write64((uint64_t *)buf_get_dt_field_addr(spu_addr(dest_spu, dest_buf_data),
                                            back_in_dtcb),
          out_dtcb.data);
}

/*-----------------------------------------------------------------------------
 * dt_in_back
 *---------------------------------------------------------------------------*/
void
dt_in_back(BUFFER_CB *buf, uint32_t src_spu, SPU_ADDRESS src_buf_data,
           uint32_t num_bytes, uint32_t spu_cmd_id, uint32_t tag)
{
  OUT_DTCB out_dtcb;
  PPU_DT_PARAMS *cmd;

#if CHECK
  // Validate buffer alignment.
  pcheck(((src_buf_data & CACHE_MASK) == 0) && (num_bytes != 0));

  // Debug tail pointer should be synchronized.
  assert(buf->otail == buf->tail);

  // Make sure back of buffer is free. *-in is allowed.
  check(buf->back_action == BUFFER_ACTION_NONE);
  buf->back_action = BUFFER_ACTION_IN;

#if !DT_ALLOW_UNALIGNED
  // Make sure data is aligned on qword boundary.
  pcheck((num_bytes & QWORD_MASK) == 0);
  check((buf->tail & QWORD_MASK) == 0);
#endif

  // Make sure enough space is available. head and tail must be off by at least
  // 1 and cannot be non-zero offsets in the same qword.
  check(((buf->head - (buf->head & QWORD_MASK ? : 1) - buf->tail) &
           buf->mask) >= num_bytes);
#endif

  // Set up description of region in destination buffer that source SPU will
  // write to. Data/space in source/destination buffers must have same offset
  // within cache line (128 bytes) - destination buffer does not automatically
  // adjust pointers if empty.
  out_dtcb.head = buf->tail;
  out_dtcb.tail = (buf->tail + num_bytes) & buf->mask;
  // Reserve space for data to be transferred.
  IF_CHECK(buf->otail = out_dtcb.tail);

  // Set data transfer to wait on the SPU command ID. This must be done before
  // writing transfer info to SPU.
  cmd = ppu_dt_wait_spu(src_spu, spu_cmd_id, tag);
  cmd->type = PPU_CMD_DT_IN_BACK;
  cmd->buf = buf;
  cmd->num_bytes = num_bytes;

  // Write transfer info to source SPU.
  write64((uint64_t *)buf_get_dt_field_addr(spu_addr(src_spu, src_buf_data),
                                            front_in_dtcb),
          out_dtcb.data);
}

/*-----------------------------------------------------------------------------
 * ppu_finish_dt_in_back
 *---------------------------------------------------------------------------*/
static INLINE void
ppu_finish_dt_in_back(PPU_DT_PARAMS *cmd)
{
  BUFFER_CB *buf = cmd->buf;
#if DT_ALLOW_UNALIGNED
  uint32_t tail_offset = buf->tail & QWORD_MASK;

  // Write unaligned data to buffer.
  if (tail_offset != 0) {
    vec_stvlx(vec_lvlx(tail_offset, &buf->back_in_dtcb.ua_data),
              buf->tail, (uint8_t *)buf->data);
  }
#endif

  buf->tail = (buf->tail + cmd->num_bytes) & buf->mask;
  IF_CHECK(buf->back_action = BUFFER_ACTION_NONE);
}

/*-----------------------------------------------------------------------------
 * dt_in_back_ex
 *---------------------------------------------------------------------------*/
void
dt_in_back_ex(BUFFER_CB *buf, uint32_t src_spu, SPU_ADDRESS src_buf_data,
              uint32_t num_bytes)
{
  OUT_DTCB out_dtcb;

#if CHECK
  // Validate buffer alignment.
  pcheck(((src_buf_data & CACHE_MASK) == 0) && (num_bytes != 0));

  // Debug tail pointer should be synchronized.
  assert(buf->otail == buf->tail);
  // Make sure back of buffer is free. *-in is allowed.
  check(buf->back_action == BUFFER_ACTION_NONE);

#if !DT_ALLOW_UNALIGNED
  // Make sure data is aligned on qword boundary.
  pcheck((num_bytes & QWORD_MASK) == 0);
  check((buf->tail & QWORD_MASK) == 0);
#endif

  // Make sure enough space is available. head and tail must be off by at least
  // 1 and cannot be non-zero offsets in the same qword.
  check(((buf->head - (buf->head & QWORD_MASK ? : 1) - buf->tail) &
           buf->mask) >= num_bytes);
#endif

  // Set up description of region in destination buffer that source SPU will
  // write to. Data/space in source/destination buffers must have same offset
  // within cache line (128 bytes) - destination buffer does not automatically
  // adjust pointers if empty.
  out_dtcb.head = buf->tail;
  out_dtcb.tail = (buf->tail + num_bytes) & buf->mask;
  // Reserve space for data to be transferred.
  IF_CHECK(buf->otail = out_dtcb.tail);

#if DT_ALLOW_UNALIGNED
  IF_CHECK(buf->back_action = BUFFER_ACTION_IN);

  // Save transfer size.
  buf->in_back_buffered_bytes = num_bytes;
#else
  buf->tail = out_dtcb.tail;
#endif

  // Write transfer info to source SPU.
  write64((uint64_t *)buf_get_dt_field_addr(spu_addr(src_spu, src_buf_data),
                                            front_in_dtcb),
          out_dtcb.data);
}

#if DT_ALLOW_UNALIGNED

/*-----------------------------------------------------------------------------
 * finish_dt_in_back_ex_head
 *---------------------------------------------------------------------------*/
void
finish_dt_in_back_ex_head(BUFFER_CB *buf, bool_t tail_overlaps)
{
  uint32_t num_bytes = buf->in_back_buffered_bytes;
  uint32_t tail_offset = buf->tail & QWORD_MASK;

  if (tail_offset != 0) {
    uint8_t *buf_data = (uint8_t *)buf->data;

    if (tail_overlaps && (QWORD_SIZE - tail_offset > num_bytes)) {
      uint32_t new_tail = buf->tail + num_bytes;
      vec_stvlx(vec_perm(vec_lvrx(new_tail & QWORD_MASK,
                                  &buf->back_in_dtcb.ua_data),
                         vec_lvlx(new_tail, buf_data),
                         vec_lvsr(num_bytes, (uint8_t *)NULL)),
                buf->tail, buf_data);
    } else {
      vec_stvlx(vec_lvlx(tail_offset, &buf->back_in_dtcb.ua_data),
                buf->tail, buf_data);
    }
  }

  buf->tail = (buf->tail + num_bytes) & buf->mask;
  assert(buf->otail == buf->tail);

#if CHECK
  if (!tail_overlaps) {
    buf->back_action = BUFFER_ACTION_NONE;
  }
#endif
}

/*-----------------------------------------------------------------------------
 * finish_dt_in_back_ex_tail
 *---------------------------------------------------------------------------*/
void
finish_dt_in_back_ex_tail(BUFFER_CB *buf)
{
  uint32_t ua_bytes;

  check(buf->back_action == BUFFER_ACTION_IN);
  ua_bytes = buf->tail & QWORD_MASK;

  if ((ua_bytes != 0) && (buf->in_back_buffered_bytes >= ua_bytes)) {
    vec_stvrx(vec_lvrx(ua_bytes, &buf->back_in_dtcb_2.ua_data),
              buf->tail, (uint8_t *)buf->data);
  }

  IF_CHECK(buf->back_action = BUFFER_ACTION_NONE);
}

#endif

/*-----------------------------------------------------------------------------
 * ppu_finish_dt
 *
 * Finishes processing for a PPU data transfer command after the SPU command
 * that handles the other end has completed.
 *---------------------------------------------------------------------------*/
void
ppu_finish_dt(PPU_DT_PARAMS *cmd)
{
  switch (cmd->type) {
    // case PPU_CMD_DT_IN_FRONT:

  case PPU_CMD_DT_IN_BACK:
    ppu_finish_dt_in_back(cmd);
    break;

  case PPU_CMD_DT_OUT_FRONT:
    ppu_finish_dt_out_front(cmd);
    break;

    // case PPU_CMD_DT_OUT_BACK:

  default:
    unreached();
  }
}
