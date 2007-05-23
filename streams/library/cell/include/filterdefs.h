/*-----------------------------------------------------------------------------
 * filterdefs.h
 *
 * Basic definitions for filter code.
 *---------------------------------------------------------------------------*/

#ifndef _SPULIB_FILTER_DEFS_H_
#define _SPULIB_FILTER_DEFS_H_

#include "../src/defs.h"
#include "../src/buffer.h"

static INLINE void
buf_advance_head(void *buf_data, uint32_t num_bytes)
{
  BUFFER_CB *buf = buf_get_cb(buf_data);
  check((((buf->tail - buf->head) & buf->mask) >= num_bytes) &&
        ((buf->mask + 1 - buf->head) >= num_bytes));
  buf->head = (buf->head + num_bytes) & buf->mask;
}

static INLINE void
buf_advance_tail(void *buf_data, uint32_t num_bytes)
{
  BUFFER_CB *buf = buf_get_cb(buf_data);
  check((((buf->head - buf->tail - 1) & buf->mask) >= num_bytes) &&
        ((buf->mask + 1 - buf->tail) >= num_bytes));
  buf->tail = (buf->tail + num_bytes) & buf->mask;
}

#endif
