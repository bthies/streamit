/*-----------------------------------------------------------------------------
 * buffer.h
 *
 * Buffer structure definitions (SPU and PPU).
 *
 * Buffer addresses point to the start of buffer data and must be 128-byte
 * aligned. Each buffer has a control block (BUFFER_CB structure) located
 * immediately before that (obtain with buf_get_cb).
 *
 * Buffer sizes must be powers of 2 and at least 128 bytes.
 *---------------------------------------------------------------------------*/

#ifndef _SPULIB_BUFFER_H_
#define _SPULIB_BUFFER_H_

// 16 bytes in size, 16-byte aligned
typedef union _SPU_IN_DTCB {
  struct {
    uint32_t head;
    uint32_t tail;
    uint32_t _unused[2];
  };
  vec4_uint32_t data;
} QWORD_ALIGNED SPU_IN_DTCB;

// 16 bytes in size, 16-byte aligned
typedef struct _PPU_IN_DTCB {
  vec16_uint8_t ua_data;
} QWORD_ALIGNED PPU_IN_DTCB;

#ifdef __SPU__  // SPU
#define IN_DTCB SPU_IN_DTCB
#else           // PPU
#define IN_DTCB PPU_IN_DTCB
#endif

// 16-byte aligned and padded
typedef struct _BUFFER_CB {
#if CHECK
// +0
  uint32_t ihead;
  uint32_t otail;
// +8
  union {
    struct {
      struct {
        uint8_t front_attached  : 1;
        uint8_t back_attached   : 1;
      };
      uint8_t front_action;
      uint8_t back_action;
      uint8_t dt_active;
    };
    uint32_t cflags;
  };
// +12
  uint32_t _c_padding;
#endif
// +0
  uint32_t mask;
  uint32_t head;
  uint32_t tail;
#ifndef __SPU__ // PPU
  void *data;
#else
  uint32_t _padding0;
#endif
// +16
  union {
    struct {
      uint32_t in_back_buffered_bytes;
      uint32_t out_front_buffered_bytes;
      uint32_t _padding1[2];
    };
    vec4_uint32_t buffered_bytes;
  };
  // These are only used in PPU buffers but SPU dt_out_*_ppu commands write to
  // them.
// -64
  // IN_DTCB front_in_dtcb_2;
// -48
  IN_DTCB back_in_dtcb_2;
// -32
  union {
    struct {
// -32
      IN_DTCB front_in_dtcb;
// -16
      IN_DTCB back_in_dtcb;
    };
    struct {
// -32
      uint32_t _f_padding0[2];
// -24
      uint32_t front_in_ack;
      uint32_t _f_padding1;
// -16
      uint32_t _b_padding0[2];
// -8
      uint32_t back_in_ack;
      uint32_t _b_padding1;
    };
  };
} QWORD_ALIGNED BUFFER_CB;

#define BUFFER_ACTION_NONE  0
#define BUFFER_ACTION_IN    1
#define BUFFER_ACTION_OUT   2
#define BUFFER_ACTION_RUN   3

/*-----------------------------------------------------------------------------
 * buf_get_cb
 *
 * Returns control block of the buffer with data at the specified address.
 *---------------------------------------------------------------------------*/
static INLINE BUFFER_CB *
buf_get_cb(void *buf_data)
{
  return (BUFFER_CB *)buf_data - 1;
}

#ifdef __SPU__

/*-----------------------------------------------------------------------------
 * buf_get_data
 *
 * Returns data address of the buffer with the specified control block.
 *---------------------------------------------------------------------------*/
static INLINE void *
buf_get_data(BUFFER_CB *buf)
{
  return (void *)(buf + 1);
}

#endif

/*-----------------------------------------------------------------------------
 * buf_get_dt_field_addr/buf_cb_get_dt_field_addr
 *
 * Returns the address of a data transfer field for a buffer. field is one of
 * front/back_in_dtcb, front/back_in_ack.
 *---------------------------------------------------------------------------*/
#define buf_get_dt_field_addr(buf_data, field) \
  ((buf_data) - (sizeof(BUFFER_CB) - offsetof(BUFFER_CB, field)))

#ifdef __SPU__
#define buf_cb_get_dt_field_addr(buf, field) \
  ((buf) + offsetof(BUFFER_CB, field))
#endif

/*-----------------------------------------------------------------------------
 * buf_bytes_used
 *
 * Returns number of bytes of data contained in the buffer with the specified
 * control block.
 *---------------------------------------------------------------------------*/
static INLINE uint32_t
buf_bytes_used(BUFFER_CB *buf)
{
  return (buf->tail - buf->head) & buf->mask;
}

/*-----------------------------------------------------------------------------
 * buf_set_head
 *---------------------------------------------------------------------------*/
static INLINE void
buf_set_head(BUFFER_CB *buf, uint32_t new_head)
{
  buf->head = new_head;
  IF_CHECK(buf->ihead = new_head);
}

/*-----------------------------------------------------------------------------
 * buf_set_tail
 *---------------------------------------------------------------------------*/
static INLINE void
buf_set_tail(BUFFER_CB *buf, uint32_t new_tail)
{
  buf->tail = new_tail;
  IF_CHECK(buf->otail = new_tail);
}

/*-----------------------------------------------------------------------------
 * buf_inc_head
 *---------------------------------------------------------------------------*/
static INLINE void
buf_inc_head(BUFFER_CB *buf, uint32_t num_bytes)
{
  buf->head = (buf->head + num_bytes) & buf->mask;
  IF_CHECK(buf->ihead = buf->head);
}

/*-----------------------------------------------------------------------------
 * buf_inc_tail
 *---------------------------------------------------------------------------*/
static INLINE void
buf_inc_tail(BUFFER_CB *buf, uint32_t num_bytes)
{
  buf->tail = (buf->tail + num_bytes) & buf->mask;
  IF_CHECK(buf->otail = buf->tail);
}

#endif
