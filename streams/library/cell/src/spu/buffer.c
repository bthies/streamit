/*-----------------------------------------------------------------------------
 * buffer.c
 *
 * Buffer command implementation (SPU).
 *---------------------------------------------------------------------------*/

#include "defs.h"
#include "depend.h"
#include "buffer.h"

/*-----------------------------------------------------------------------------
 * run_buffer_alloc
 *
 * Command handler.
 *---------------------------------------------------------------------------*/
void
run_buffer_alloc(BUFFER_ALLOC_CMD *cmd)
{
  BUFFER_CB *buf = buf_get_cb(cmd->buf_data);

  // Validate buffer data address and initial pointer offset (mask seems too
  // hard).
  pcheck((((uintptr_t)cmd->buf_data & CACHE_MASK) == 0) &&
         (cmd->data_offset <= cmd->mask));

  buf->mask = cmd->mask;

  // Initialize head/tail pointers.
  buf->head = cmd->data_offset;
  buf->tail = cmd->data_offset;
#if CHECK
  // Synchronize debug head/tail pointers.
  buf->ihead = cmd->data_offset;
  buf->otail = cmd->data_offset;
#endif

  IF_CHECK(buf->cflags = 0);

  buf->buffered_bytes = VEC_SPLAT_U32(0);
  buf->front_in_dtcb.data = VEC_SPLAT_U32(0);
  buf->back_in_dtcb.data = VEC_SPLAT_U32(0);

  // Done.
  dep_complete_command();
}

/*-----------------------------------------------------------------------------
 * run_buffer_align
 *
 * Command handler.
 *---------------------------------------------------------------------------*/
void
run_buffer_align(BUFFER_ALIGN_CMD *cmd)
{
  BUFFER_CB *buf = buf_get_cb(cmd->buf_data);

  // Validate buffer address and new pointer offset.
  pcheck((((uintptr_t)cmd->buf_data & CACHE_MASK) == 0) &&
         (cmd->data_offset <= buf->mask));
  // Make sure buffer is empty and not being used (okay if buffer is attached).
  check((buf->head == buf->tail) &&
        (buf->front_action == BUFFER_ACTION_NONE) &&
        (buf->back_action == BUFFER_ACTION_NONE));

  // Adjust head/tail pointers.
  buf->head = cmd->data_offset;
  buf->tail = cmd->data_offset;
#if CHECK
  // Synchronize debug head/tail pointers.
  buf->ihead = cmd->data_offset;
  buf->otail = cmd->data_offset;
#endif

  // Done.
  dep_complete_command();
}
