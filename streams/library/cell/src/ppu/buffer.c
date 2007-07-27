/*-----------------------------------------------------------------------------
 * buffer.c
 *
 * Buffer command implementation.
 *---------------------------------------------------------------------------*/

#include "spulibint.h"

/*-----------------------------------------------------------------------------
 * alloc_buffer
 *
 * Allocates memory for and initializes a new buffer.
 *
 * size is in bytes, must be power of 2 and at least 128 bytes.
 * data_offset is initial value of head/tail pointers.
 *
 * Returns pointer to buffer data, NULL on failure.
 *---------------------------------------------------------------------------*/
void *
alloc_buffer(uint32_t size, uint32_t data_offset)
{
  /*
   * Additional memory is allocated to align buffer data on cache line (128
   * bytes).
   *
   * Memory layout is:
   * - Padding as needed.
   * - Pointer to start of allocated memory.
   * - Control block.
   * - Data (128-byte aligned).
   *
   * Assumptions: malloc, sizeof(BUFFER_CB), CACHE_SIZE are all multiples of
   * pointer size.
   */

  void *mem;
  void **mem_ptr;
  BUFFER_CB *buf;
  uintptr_t buf_data;
  uintptr_t page;
  uintptr_t end_page;

  mem = malloc(CACHE_SIZE + sizeof(BUFFER_CB) + size);

  if (mem == NULL) {
    return NULL;
  }

  mem_ptr = (void **)(mem +
    (((CACHE_SIZE - (sizeof(void *) + sizeof(BUFFER_CB))) - (uintptr_t)mem) &
     CACHE_MASK));
  *mem_ptr = mem;

  // Initialize control block.
  buf = (BUFFER_CB *)(mem_ptr + 1);
  buf->mask = size - 1;

  // Initialize head/tail pointers.
  buf->head = data_offset;
  buf->tail = data_offset;
#if CHECK
  // Synchronize debug head/tail pointers.
  buf->ihead = data_offset;
  buf->otail = data_offset;
#endif

  IF_CHECK(buf->cflags = 0);

  // Touch every page (avoids page faults when writing to output buffers).
  buf_data = (uintptr_t)(buf + 1);
  page = ROUND_UP(buf_data, PAGE_SIZE);
  end_page = ROUND_UP(buf_data + size, PAGE_SIZE);

  while (page != end_page) {
    *(uint32_t *)page = 0;
    page += PAGE_SIZE;
  }

  return (void *)buf_data;
}

/*-----------------------------------------------------------------------------
 * align_buffer
 *
 * Adjusts head and tail pointers of an empty buffer.
 *---------------------------------------------------------------------------*/
void
align_buffer(void *buf_data, uint32_t data_offset)
{
  BUFFER_CB *buf = buf_get_cb(buf_data);

  // Validate buffer address and new pointer offset.
  pcheck((((uintptr_t)buf_data & CACHE_MASK) == 0) &&
         (data_offset <= buf->mask));
  // Make sure buffer is empty and not being used.
  check((buf->head == buf->tail) &&
        (buf->front_action == BUFFER_ACTION_NONE) &&
        (buf->back_action == BUFFER_ACTION_NONE));

  // Adjust head/tail pointers.
  buf->head = data_offset;
  buf->tail = data_offset;
#if CHECK
  // Synchronize debug head/tail pointers.
  buf->ihead = data_offset;
  buf->otail = data_offset;
#endif
}

/*-----------------------------------------------------------------------------
 * dealloc_buffer
 *
 * Frees memory used by an empty buffer.
 *---------------------------------------------------------------------------*/
void
dealloc_buffer(void *buf_data)
{
  BUFFER_CB *buf = buf_get_cb(buf_data);

  // Validate buffer address.
  pcheck(((uintptr_t)buf_data & CACHE_MASK) == 0);
  // Make sure buffer is empty and not being used.
  check((buf->head == buf->tail) &&
        (buf->front_action == BUFFER_ACTION_NONE) &&
        (buf->back_action == BUFFER_ACTION_NONE));

  free(*((void **)buf - 1));
}
