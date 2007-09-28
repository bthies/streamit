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
buf_advance_head(BUFFER_CB *buf, uint32_t num_bytes)
{
  check(((buf->tail - buf->head) & buf->mask) >= num_bytes);
  buf->head = (buf->head + num_bytes) & buf->mask;
}

static INLINE void
buf_advance_tail(BUFFER_CB *buf, uint32_t num_bytes)
{
  check(((buf->head - buf->tail - 1) & buf->mask) >= num_bytes);
  buf->tail = (buf->tail + num_bytes) & buf->mask;
}

#endif
