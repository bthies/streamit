/*-----------------------------------------------------------------------------
 * dma.h
 *
 * Functions for managing DMA transfers.
 *
 * Commands reserve a tag, call functions with it, and then release it when
 * done. Commands must not use tags they have not reserved.
 *---------------------------------------------------------------------------*/

#ifndef _DMA_H_
#define _DMA_H_

#include <spu_mfcio.h>
#include "depend.h"

// Number of tags available. Tags range from 0 to (this - 1).
#define DMA_NUM_TAGS  32
#define INVALID_TAG   255

// Maximum number of bytes in a DMA transfer.
#define MAX_DMA_SIZE  (16 * 1024)

// Bitmap of tags that are unused.
extern uint32_t dma_free_tags;
// Bitmap of tags that have pending DMA transfers.
extern uint32_t dma_active_tags;

// IDs of commands that are waiting for tags to complete.
extern uint8_t dma_waiting[DMA_NUM_TAGS];

// ID of first command in list of commands that are waiting for MFC slots to
// become available.
extern uint8_t dma_waiting_avail;
// Pointer to .next field of last command in waiting list.
extern uint8_t *dma_waiting_avail_ptr;

// Checks if a tag has been reserved.
#define dma_assert_tag_valid(tag) \
  assert(((tag) < DMA_NUM_TAGS) && ((dma_free_tags & (1 << (tag))) == 0) && \
         (dma_waiting[tag] == INVALID_COMMAND_ID))

/*-----------------------------------------------------------------------------
 * dma_reserve_tag
 *
 * Reserves a tag for use by a command.
 *
 * If DMA_ALLOW_RESERVE_FAIL is 1, returns INVALID_TAG if all tags are already
 * reserved. Otherwise, caller must ensure a tag is available.
 *---------------------------------------------------------------------------*/
static INLINE DMA_TAG
dma_reserve_tag()
{
  DMA_TAG free_tag;

#if DMA_ALLOW_RESERVE_FAIL
  if (dma_free_tags == 0) {
    return INVALID_TAG;
  }
#else
  check(dma_free_tags != 0);
#endif

  free_tag = count_ls_zeros(dma_free_tags);
  dma_free_tags &= ~(1 << free_tag);
  return free_tag;
}

/*-----------------------------------------------------------------------------
 * dma_query_complete
 *
 * Returns whether all DMA transfers using the specified tag have completed.
 *---------------------------------------------------------------------------*/
static INLINE bool_t
dma_query_complete(DMA_TAG tag)
{
  dma_assert_tag_valid(tag);
  return (dma_active_tags & (1 << tag)) == 0;
}

/*-----------------------------------------------------------------------------
 * dma_release_tag
 *
 * Releases a tag previously reserved by a command.
 *---------------------------------------------------------------------------*/
static INLINE void
dma_release_tag(DMA_TAG tag)
{
  // There must be no pending transfers using the tag.
  assert(dma_query_complete(tag));
  dma_free_tags |= (1 << tag);
}

/*-----------------------------------------------------------------------------
 * dma_query_avail
 *
 * Returns whether the specified number of MFC slots is currently available.
 *---------------------------------------------------------------------------*/
static INLINE bool_t
dma_query_avail(uint8_t num_slots)
{
  return LIKELY(mfc_stat_cmd_queue() >= num_slots);
}

/*-----------------------------------------------------------------------------
 * dma_get
 *
 * Starts a DMA transfer into local store.
 *---------------------------------------------------------------------------*/
static INLINE void
dma_get(DMA_TAG tag, void *dest_lsa, MEM_ADDRESS src_addr, uint32_t num_bytes)
{
  dma_assert_tag_valid(tag);
  assert(dma_query_avail(1));

  // Mark tag as active.
  dma_active_tags |= (1 << tag);
  mfc_get(dest_lsa, src_addr, num_bytes, tag, 0, 0);
}

/*-----------------------------------------------------------------------------
 * dma_put
 *
 * Starts a DMA transfer out of local store.
 *---------------------------------------------------------------------------*/
static INLINE void
dma_put(DMA_TAG tag, MEM_ADDRESS dest_addr, void *src_lsa, uint32_t num_bytes)
{
  dma_assert_tag_valid(tag);
  assert(dma_query_avail(1));

  // Mark tag as active.
  dma_active_tags |= (1 << tag);
  mfc_put(src_lsa, dest_addr, num_bytes, tag, 0, 0);
}

// Suspends the current command until the specified tag has completed. Command
// handler must return afterwards.
void dma_wait_complete(DMA_TAG tag);
// Suspends the current command until an additional MFC slot becomes available.
// Command handler must return afterwards.
void dma_wait_avail();

#endif
