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
 * size is in bytes, must be multiple of 128 bytes.
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
  void *buf_data;

  pcheck((size >= CACHE_SIZE) && (data_offset < size));

  mem = malloc(CACHE_SIZE + sizeof(BUFFER_CB) + size);

  if (mem == NULL) {
    return NULL;
  }

  mem_ptr = (void **)(mem +
    (((CACHE_SIZE - (sizeof(void *) + sizeof(BUFFER_CB))) - (uintptr_t)mem) &
     CACHE_MASK));
  *mem_ptr = mem;

  buf = (BUFFER_CB *)(mem_ptr + 1);
  buf_data = buf + 1;
  touch_pages(buf_data, size);
  init_buffer(buf, buf_data, size, data_offset);

  return buf_data;
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

/*-----------------------------------------------------------------------------
 * init_buffer
 *---------------------------------------------------------------------------*/
void
init_buffer(BUFFER_CB *buf, void *buf_data, uint32_t size,
            uint32_t data_offset)
{
  pcheck((size >= CACHE_SIZE) && (data_offset < size));

  if (buf_data == NULL) {
    buf_data = malloc_aligned(size);
    check(buf_data != NULL);
  } else {
    pcheck(((uintptr_t)buf_data & CACHE_MASK) == 0);
  }

  buf->data = buf_data;
  buf->mask = (size == (1U << count_ls_zeros(size)) ? size - 1 : 0x7fffffff);
  buf->head = data_offset;
  buf->tail = data_offset;

#if CHECK
  buf->ihead = data_offset;
  buf->otail = data_offset;
  buf->cflags = 0;
#endif
}

/*-----------------------------------------------------------------------------
 * duplicate_buffer
 *---------------------------------------------------------------------------*/
void
duplicate_buffer(BUFFER_CB *dest, BUFFER_CB *src)
{
  dest->data = src->data;
  dest->mask = src->mask;
  dest->head = src->head;
  dest->tail = src->tail;

#if CHECK
  dest->ihead = dest->head;
  dest->otail = dest->tail;
  dest->cflags = 0;
#endif
}

/*-----------------------------------------------------------------------------
 * malloc_aligned
 *
 * Allocates memory aligned on a cache line (128 bytes).
 *---------------------------------------------------------------------------*/
void *
malloc_aligned(uint32_t size)
{
  /*
   * Additional memory is allocated to align data.
   *
   * Memory layout is:
   * - Padding as needed.
   * - Pointer to start of allocated memory.
   * - Data (128-byte aligned).
   *
   * Assumptions: malloc, CACHE_SIZE are all multiples of pointer size.
   */

  void *mem;
  void *data;

  mem = malloc(CACHE_SIZE + size);

  if (mem == NULL) {
    return NULL;
  }

  data = mem + (CACHE_SIZE - ((uintptr_t)mem & CACHE_MASK));
  *((void **)data - 1) = mem;
  touch_pages(data, size);

  return data;
}

/*-----------------------------------------------------------------------------
 * free_aligned
 *---------------------------------------------------------------------------*/
void
free_aligned(void *data)
{
  free(*((void **)data - 1));
}

/*-----------------------------------------------------------------------------
 * touch_pages
 *
 * Touches every page in a block of memory (avoids page faults when writing to
 * output buffers).
 *---------------------------------------------------------------------------*/
void
touch_pages(void *data, uint32_t size)
{
  uintptr_t page;
  uintptr_t end_page;

  *(uint32_t *)data = 0;

  page = ROUND_UP((uintptr_t)data, PAGE_SIZE);
  end_page = ROUND_UP((uintptr_t)data + size, PAGE_SIZE);

  while (page != end_page) {
    *(uint32_t *)page = 0;
    page += PAGE_SIZE;
  }
}
