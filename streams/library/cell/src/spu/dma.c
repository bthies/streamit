/*-----------------------------------------------------------------------------
 * dma.c
 *
 * Functions for managing DMA transfers.
 *---------------------------------------------------------------------------*/

#include "defs.h"
#include "depend.h"
#include "dma.h"

uint32_t dma_free_tags = 0xffffffff;
uint32_t dma_active_tags = 0;

uint8_t dma_waiting[DMA_NUM_TAGS] = {
  INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID,
  INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID,
  INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID,
  INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID,
  INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID,
  INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID,
  INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID,
  INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID, INVALID_COMMAND_ID
};

uint8_t dma_waiting_avail = INVALID_COMMAND_ID;
uint8_t *dma_waiting_avail_ptr = &dma_waiting_avail;

/*-----------------------------------------------------------------------------
 * dma_wait_complete
 *
 * Suspends the current command until the specified tag has completed.
 *
 * Command handler must return afterwards.
 *---------------------------------------------------------------------------*/
void
dma_wait_complete(DMA_TAG tag)
{
  // Don't do anything if tag has already completed. Otherwise, dequeue command
  // and put ID in waiting array.
  if (!dma_query_complete(tag)) {
    assert(!dep_dequeued);
    IF_DEBUG(dep_dequeued = TRUE);

    *dep_cur_ptr = *dep_next_ptr;
    dep_next_ptr = dep_cur_ptr;
    dma_waiting[tag] = dep_cur_id;
  }
}

/*-----------------------------------------------------------------------------
 * dma_wait_avail
 *
 * Suspends the current command until some tag has completed. When the command
 * is queued again, additional MFC slot(s) should (but are not guaranteed to)
 * be available.
 *
 * Command handler must return afterwards.
 *---------------------------------------------------------------------------*/
void
dma_wait_avail()
{
  // Dequeue command.
  assert(!dep_dequeued);
  IF_DEBUG(dep_dequeued = TRUE);
  *dep_cur_ptr = *dep_next_ptr;

  // Add command to end of waiting list.
  *dep_next_ptr = INVALID_COMMAND_ID;
  *dma_waiting_avail_ptr = dep_cur_id;
  dma_waiting_avail_ptr = dep_next_ptr;

  // Fix up next pointer.
  dep_next_ptr = dep_cur_ptr;
}
