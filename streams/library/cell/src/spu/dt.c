/*-----------------------------------------------------------------------------
 * dt.c
 *
 * SPU data transfer implementation.
 *---------------------------------------------------------------------------*/

#include "defs.h"
#include "depend.h"
#include "buffer.h"
#include "dma.h"

/*-----------------------------------------------------------------------------
 * "Normal"/dataflow direction.
 *---------------------------------------------------------------------------*/

/*-----------------------------------------------------------------------------
 * run_dt_out_front
 *
 * Command handler.
 *---------------------------------------------------------------------------*/
void
run_dt_out_front(DT_OUT_FRONT_CMD *cmd)
{
  BUFFER_CB *buf = buf_get_cb(cmd->buf_data);

  switch (cmd->state) {
  case 0:
    // Initialization.

#if CHECK
    // Validate buffer alignment.
    pcheck((((uintptr_t)cmd->buf_data & CACHE_MASK) == 0) &&
           ((cmd->dest_buf_data & CACHE_MASK) == 0) &&
           (cmd->num_bytes != 0));

    // Debug head pointer should be synchronized.
    assert(buf->ihead == buf->head);
    // Make sure front of buffer is free. out-out is disallowed.
    check((buf->front_action == BUFFER_ACTION_NONE) &&
          (buf->back_action != BUFFER_ACTION_OUT));
    buf->front_action = BUFFER_ACTION_OUT;
    // Make sure enough data is available.
    check(((buf->tail - buf->head) & buf->mask) >= cmd->num_bytes);
    buf->dt_active++;
#endif

    // Reserve tag for writing control block.
    cmd->tag = dma_reserve_tag();
    check(cmd->tag != INVALID_TAG);

    // Initialize control block with source buffer's head/tail pointers for the
    // transfer.
    cmd->out_dtcb.head = buf->head;
    cmd->out_dtcb.tail = (buf->head + cmd->num_bytes) & buf->mask;
    // Reset front_in_ack area for destination processor to write
    // acknowledgement to. At this point the entire front_in area can be safely
    // written to.
    buf->front_in_ack = buf->head;
    // Advance inner head pointer to mark off data to be transferred.
    IF_CHECK(buf->ihead = cmd->out_dtcb.tail);

    cmd->state = 1;

  case 1:
    // Write control block to destination processor.

    // Wait for MFC slot.
    while (!dma_query_avail(1)) {
      dma_wait_avail();
      return;
    }

    dma_put(cmd->tag,
            buf_get_dt_field_addr(cmd->dest_buf_data, back_in_dtcb),
            &cmd->out_dtcb,
            sizeof(cmd->out_dtcb));

    dma_wait_complete(cmd->tag);
    cmd->state = 2;
    return;

  case 2:
    dma_release_tag(cmd->tag);
    cmd->state = 3;

  case 3: {
    // Wait until destination processor acknowledges that all data has been
    // copied.

    uint32_t ack;

    ack = volatile_read(buf->front_in_ack);
    if (ack != cmd->out_dtcb.tail) {
      return;
    }

    // Update outer head pointer.
    buf->head = ack;
#if CHECK
    buf->dt_active--;
    buf->front_action = BUFFER_ACTION_NONE;
#endif

    dep_complete_command();
    return;
  }

  default:
    unreached();
  }
}

/*-----------------------------------------------------------------------------
 * run_dt_in_back
 *
 * Command handler.
 *---------------------------------------------------------------------------*/
void
run_dt_in_back(DT_IN_BACK_CMD *cmd)
{
  BUFFER_CB *buf = buf_get_cb(cmd->buf_data);
  uint32_t buf_tail;

  switch (cmd->state) {
  case 0:
    // Initialization. Buffer's back_in_dtcb should have been reset by a
    // previous command.

#if CHECK
    // Validate buffer alignment.
    pcheck((((uintptr_t)cmd->buf_data & CACHE_MASK) == 0) &&
           ((cmd->src_buf_data & CACHE_MASK) == 0) &&
           (cmd->num_bytes != 0));

#if !DT_ALLOW_UNALIGNED
    // Make sure transfer size is multiple of qwords.
    pcheck((cmd->num_bytes & QWORD_MASK) == 0);
#endif

    // Debug tail pointer should be synchronized.
    assert(buf->otail == buf->tail);
    // Make sure back of buffer is free. *-in is allowed.
    check(buf->back_action == BUFFER_ACTION_NONE);
#endif

    if (buf->in_back_buffered_bytes >= cmd->num_bytes) {
      buf->in_back_buffered_bytes -= cmd->num_bytes;
      dep_complete_command();
      return;
    }

    IF_CHECK(buf->back_action = BUFFER_ACTION_IN);

    cmd->tag = dma_reserve_tag();
    check(cmd->tag != INVALID_TAG);

    cmd->state = 1;

  case 1: {
    // Wait for source processor to write control block.

    IN_DTCB in_dtcb;
    uint32_t dt_bytes;
    uint32_t src_bytes;
#if DT_AUTO_ADJUST_POINTERS
    uint32_t data_offset;
#endif

    // Wait for MFC slot in advance.
    if (!dma_query_avail(IF_DT_ALLOW_UNALIGNED(2, 1))) {
      dma_wait_avail();
      return;
    }

    in_dtcb.data = volatile_read(buf->back_in_dtcb.data);
    if (in_dtcb.head == in_dtcb.tail) {
      return;
    }

#if !DT_ALLOW_UNALIGNED
    // Make sure data in source buffer is aligned on qword boundary.
    check((in_dtcb.head & QWORD_MASK) == 0);
#endif

    cmd->num_bytes -= buf->in_back_buffered_bytes;
    dt_bytes = cmd->num_bytes;
    src_bytes = (in_dtcb.tail - in_dtcb.head) & cmd->src_buf_mask;

    // Make sure number of bytes transferring out from source buffer is at
    // least number of bytes transferring in.
    check(src_bytes >= dt_bytes);

    if (dt_bytes < CACHE_SIZE) {
      dt_bytes = CACHE_SIZE;
    }

#if DT_AUTO_ADJUST_POINTERS
    // Automatically adjust head/tail pointers to match data offset in source
    // buffer.
    data_offset = in_dtcb.head & CACHE_MASK;

    if ((buf->tail & CACHE_MASK) != data_offset) {
      // Need to adjust offset - buffer must be empty with no front data
      // transfer in progress. If CHECK is off and this is not the case,
      // everything will get screwed up.

      uint32_t offset_diff;

      check((buf->ihead == buf->tail) && (buf->dt_active == 0));

      offset_diff = data_offset - buf->tail;
      buf->head = (buf->head + offset_diff) & buf->mask;
      buf->tail = data_offset;
#if CHECK
      buf->ihead = data_offset;
      buf->otail = data_offset;
#endif
    }
#else
    check((buf->tail & CACHE_MASK) == (in_dtcb.head & CACHE_MASK));
#endif

#if DT_ALLOW_UNALIGNED
    dt_bytes += (QWORD_SIZE - (buf->tail + dt_bytes)) & QWORD_MASK;
#endif

    if (dt_bytes > src_bytes) {
      dt_bytes = src_bytes;
    }

    assert((dt_bytes == src_bytes) ||
           (((buf->tail + dt_bytes) & QWORD_MASK) == 0));
    buf->in_back_buffered_bytes = dt_bytes - cmd->num_bytes;
    cmd->num_bytes = dt_bytes;

#if CHECK
    // Make sure enough space is available. head and tail must be off by at
    // least 1 and cannot be non-zero offsets in the same qword.
    check(((buf->head - (buf->head & QWORD_MASK ? : 1) - buf->tail) &
             buf->mask) >= dt_bytes);
    // Reserve space for data to be transferred.
    buf->otail = (buf->tail + dt_bytes) & buf->mask;
    buf->dt_active++;
#endif

    cmd->src_head = in_dtcb.head;

#if DT_ALLOW_UNALIGNED
    if ((buf->tail & QWORD_MASK) != 0) {
      // Start DMA for unaligned qword of data.
      uint32_t ua_bytes;

      dma_get(cmd->tag,
              &cmd->ua_data,
              cmd->src_buf_data + ROUND_DOWN(cmd->src_head, QWORD_SIZE),
              QWORD_SIZE);
      cmd->state = 4;

      // Advance to first aligned piece of data.
      ua_bytes = QWORD_SIZE - (cmd->src_head & QWORD_MASK);

      if (ua_bytes > cmd->num_bytes) {
        ua_bytes = cmd->num_bytes;
      }

      cmd->src_head = (cmd->src_head + ua_bytes) & cmd->src_buf_mask;
      cmd->num_bytes -= ua_bytes;
      cmd->ua_bytes = ua_bytes;

      if (cmd->num_bytes == 0) {
        cmd->copy_bytes = 0;
        dma_wait_complete(cmd->tag);
        return;
      } else {
        buf_tail = (buf->tail + ua_bytes) & buf->mask;
        goto state_copy_next;
      }
    }
#endif

    // Start copying aligned data.
    cmd->state = 2;
    buf_tail = buf->tail;
    goto state_copy_next;
  }

  state_dma_complete:
  case 2:
    // Finished copying piece of data.

    // Wait for MFC slot in advance.
    if (!dma_query_avail(1)) {
      dma_wait_avail();
      return;
    }

    buf->tail = buf_tail = (buf->tail + cmd->copy_bytes) & buf->mask;

    if (cmd->num_bytes == 0) {
      // Finished copying data.

      // Update back_in_dtcb in preparation for next dt_in_back.
      buf->back_in_dtcb.head = cmd->src_head;

      if ((cmd->src_buf != 0) && (cmd->src_head == buf->back_in_dtcb.tail)) {
        // Write acknowledgement (new value of head pointer for source buffer).
        dma_put(cmd->tag,
                buf_cb_get_dt_field_addr(cmd->src_buf, front_in_ack),
                &cmd->out_ack,
                sizeof(cmd->out_ack));
        cmd->state = 3;
      } else {
        goto state_done;
      }
    } else {
      // Start DMA for next piece of data.

      uint32_t copy_bytes;
      uint32_t dest_bytes;

    state_copy_next:

      // Bytes to end of source buffer.
      copy_bytes = (cmd->src_buf_mask + 1) - cmd->src_head;

      // Bytes left to copy.
      if (copy_bytes > cmd->num_bytes) {
        copy_bytes = cmd->num_bytes;
      }

      // Bytes to end of destination buffer.
      dest_bytes = (buf->mask + 1) - buf_tail;

      if (copy_bytes > dest_bytes) {
        copy_bytes = dest_bytes;
      }

      // Limit DMA size.
      if (copy_bytes > MAX_DMA_SIZE) {
        copy_bytes = MAX_DMA_SIZE;
      }

      dma_get(cmd->tag,
              cmd->buf_data + buf_tail,
              cmd->src_buf_data + cmd->src_head,
#if DT_ALLOW_UNALIGNED
              ROUND_UP(copy_bytes, QWORD_SIZE)
#else
              copy_bytes
#endif
              );

      // Advance to next piece.
      cmd->src_head = (cmd->src_head + copy_bytes) & cmd->src_buf_mask;
      cmd->num_bytes -= copy_bytes;
      cmd->copy_bytes = copy_bytes;
    }

    dma_wait_complete(cmd->tag);
    return;

  state_done:
  case 3:
    // Finished copying data.

    dma_release_tag(cmd->tag);

#if CHECK
    // Make sure tail pointer was updated correctly and reset flags.
    assert(buf->tail == buf->otail);
    buf->dt_active--;
    buf->back_action = BUFFER_ACTION_NONE;
#endif

    dep_complete_command();
    return;

#if DT_ALLOW_UNALIGNED
  case 4: {
    // Finished copying unaligned qword of data. Write unaligned data to buffer
    // and copy rest of data normally.

    uint32_t ua_bytes;
    vec16_uint8_t *ua_data;

    // Write valid part of unaligned data qword to buffer.
    ua_bytes = QWORD_SIZE - (buf->tail & QWORD_MASK);
    ua_data = (vec16_uint8_t *)
      (cmd->buf_data + ROUND_DOWN(buf->tail, QWORD_SIZE));
    *ua_data = spu_sel(*ua_data, cmd->ua_data, spu_maskb((1 << ua_bytes) - 1));

    // Advance to first aligned piece of data.
    buf->tail = (buf->tail + cmd->ua_bytes) & buf->mask;

    cmd->state = 2;
    goto state_dma_complete;
  }
#endif

  default:
    unreached();
  }
}

/*-----------------------------------------------------------------------------
 * run_dt_out_front_ppu
 *
 * Command handler.
 *---------------------------------------------------------------------------*/
void
run_dt_out_front_ppu(DT_OUT_FRONT_PPU_CMD *cmd)
{
  BUFFER_CB *buf = buf_get_cb(cmd->buf_data);
  uint32_t buf_head;
  uint32_t dest_tail;
#if DT_ALLOW_UNALIGNED
  uint32_t ua_bytes = 0;
#endif

  switch (cmd->state) {
#if CHECK
  case 255:
    // Validate buffer alignment.
    pcheck((((uintptr_t)cmd->buf_data & CACHE_MASK) == 0) &&
           ((cmd->dest_buf_data & CACHE_MASK) == 0) &&
           (cmd->num_bytes != 0));

    // Debug head pointer should be synchronized.
    assert(buf->ihead == buf->head);
    // Make sure front of buffer is free. out-out is disallowed.
    check((buf->front_action == BUFFER_ACTION_NONE) &&
          (buf->back_action != BUFFER_ACTION_OUT));
    buf->front_action = BUFFER_ACTION_OUT;

    cmd->state = 0;
#endif

  case 0: {
    // Wait for PPU to write location of free space in destination buffer.

    IN_DTCB in_dtcb;
    uint32_t dt_bytes;
    uint32_t dest_bytes;

    // Wait for MFC slot in advance.
    if (!dma_query_avail(IF_DT_ALLOW_UNALIGNED(3, 1))) {
      dma_wait_avail();
      return;
    }

    in_dtcb.data = volatile_read(buf->front_in_dtcb.data);
    if (in_dtcb.head == in_dtcb.tail) {
      return;
    }

    // Make sure data in source/destination buffers have same offset within
    // cache line (128 bytes).
    check((buf->head & CACHE_MASK) == (in_dtcb.head & CACHE_MASK));

    cmd->num_bytes += buf->out_front_buffered_bytes;
    dt_bytes = cmd->num_bytes;
    dest_bytes = (in_dtcb.tail - in_dtcb.head) & cmd->dest_buf_mask;

    // Make sure number of bytes transferring into destination buffer is at
    // least number of bytes transferring out.
    check(dest_bytes >= dt_bytes);

    if (dt_bytes != dest_bytes) {
      uint32_t ua_bytes = (buf->head + dt_bytes) & QWORD_MASK;

      if ((dt_bytes <= ua_bytes) || ((dt_bytes -= ua_bytes) < CACHE_SIZE)) {
        buf->out_front_buffered_bytes = cmd->num_bytes;
        IF_CHECK(buf->front_action = BUFFER_ACTION_NONE);
        dep_complete_command();
        return;
      }
    }

    assert((dt_bytes == dest_bytes) ||
           (((buf->head + dt_bytes) & QWORD_MASK) == 0));
    buf->out_front_buffered_bytes = cmd->num_bytes - dt_bytes;
    cmd->num_bytes = dt_bytes;

#if CHECK
    // Make sure enough data is available and mark off data to be transferred.
    check(((buf->tail - buf->head) & buf->mask) >= dt_bytes);
    buf->ihead = (buf->head + dt_bytes) & buf->mask;
    buf->dt_active++;
#endif

    // Reserve tag for copying data.
    cmd->tag = dma_reserve_tag();
    check(cmd->tag != INVALID_TAG);

    cmd->dest_tail = in_dtcb.head;

    cmd->state = 1;

#if DT_ALLOW_UNALIGNED
    if ((dt_bytes == dest_bytes) && cmd->tail_overlaps) {
      uint32_t tail_ua_bytes = in_dtcb.tail & QWORD_MASK;

      if ((tail_ua_bytes != 0) && (dt_bytes >= tail_ua_bytes)) {
        dma_put(cmd->tag,
                buf_cb_get_dt_field_addr(cmd->dest_buf, back_in_dtcb_2),
                cmd->buf_data +
                  ((buf->head + dt_bytes - tail_ua_bytes) & buf->mask),
                QWORD_SIZE);

        if (tail_ua_bytes == dt_bytes) {
          cmd->copy_bytes = tail_ua_bytes;
          dma_wait_complete(cmd->tag);
          cmd->state = 2;
          return;
        } else {
          cmd->tail_ua_bytes = tail_ua_bytes;
          cmd->num_bytes -= tail_ua_bytes;
        }
      }
    }

    if ((buf->head & QWORD_MASK) != 0) {
      // Write unaligned qword of data to destination buffer's control block.

      dma_put(cmd->tag,
              buf_cb_get_dt_field_addr(cmd->dest_buf, back_in_dtcb),
              cmd->buf_data + ROUND_DOWN(buf->head, QWORD_SIZE),
              QWORD_SIZE);

      ua_bytes = QWORD_SIZE - (buf->head & QWORD_MASK);

      if (ua_bytes >= cmd->num_bytes) {
        // No more data left.
        cmd->copy_bytes = cmd->num_bytes;

        dma_wait_complete(cmd->tag);
        cmd->state = 2;
        return;
      } else {
        buf_head = (buf->head + ua_bytes) & buf->mask;
        dest_tail = (cmd->dest_tail + ua_bytes) & cmd->dest_buf_mask;
        cmd->num_bytes -= ua_bytes;

        goto state_copy_next;
      }
    }
#endif

    // Start copying aligned data.
    buf_head = buf->head;
    dest_tail = cmd->dest_tail;
    goto state_copy_next;
  }

  case 1: {
    // Finished copying piece of data, with more left to copy. Start DMA for
    // next piece.

    uint32_t copy_bytes;
    uint32_t dest_bytes;

    // Wait for MFC slot.
    if (!dma_query_avail(1)) {
      dma_wait_avail();
      return;
    }

    // Advance pointers to next piece.
    buf->head = buf_head = (buf->head + cmd->copy_bytes) & buf->mask;
    cmd->dest_tail = dest_tail =
      (cmd->dest_tail + cmd->copy_bytes) & cmd->dest_buf_mask;
    cmd->num_bytes -= cmd->copy_bytes;

  state_copy_next:

    // Bytes to end of source buffer.
    copy_bytes = (buf->mask + 1) - buf_head;

    // Bytes to end of destination buffer.
    dest_bytes = (cmd->dest_buf_mask + 1) - dest_tail;

    if (copy_bytes > dest_bytes) {
      copy_bytes = dest_bytes;
    }

    // Limit DMA size.
    if (copy_bytes > MAX_DMA_SIZE) {
      copy_bytes = MAX_DMA_SIZE;
    }

    // Bytes left to copy.
    if (copy_bytes >= cmd->num_bytes) {
      // No more data left.
      copy_bytes = cmd->num_bytes;
      cmd->state = 2;
    }

    dma_put(cmd->tag,
            cmd->dest_buf_data + dest_tail,
            cmd->buf_data + buf_head,
#if DT_ALLOW_UNALIGNED
            ROUND_UP(copy_bytes, QWORD_SIZE)
#else
            copy_bytes
#endif
            );

    cmd->copy_bytes = copy_bytes + IF_DT_ALLOW_UNALIGNED(ua_bytes, 0);

    dma_wait_complete(cmd->tag);
    return;
  }

  case 2: {
    // Finished copying last piece of data.

    uint32_t copy_bytes;

    copy_bytes = cmd->copy_bytes + cmd->tail_ua_bytes;
    buf->head = (buf->head + copy_bytes) & buf->mask;

    // Update front_in_dtcb before corresponding PPU-side command completes.
    buf->front_in_dtcb.head =
      (cmd->dest_tail + copy_bytes) & cmd->dest_buf_mask;
    assert((buf->front_in_dtcb.head != buf->front_in_dtcb.tail) ||
           (buf->out_front_buffered_bytes == 0));

    dma_release_tag(cmd->tag);

#if CHECK
    // Make sure head pointer was updated correctly and reset flags.
    assert(buf->head == buf->ihead);
    buf->dt_active--;
    buf->front_action = BUFFER_ACTION_NONE;
#endif

    dep_complete_command();
    return;
  }

  default:
    unreached();
  }
}

/*-----------------------------------------------------------------------------
 * "Reverse dataflow" direction.
 *---------------------------------------------------------------------------*/

// TODO: Implement this when it's actually needed (not bloody likely).

/*

void
run_dt_out_back(DT_OUT_BACK_CMD *cmd)
{

}

void
run_dt_in_front(DT_IN_FRONT_CMD *cmd)
{

}

void
run_dt_out_back_ppu(DT_OUT_BACK_PPU_CMD *cmd)
{

}

*/
