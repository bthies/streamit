/*-----------------------------------------------------------------------------
 * spulib.c
 *
 * Implementation for PPU interface.
 *---------------------------------------------------------------------------*/

#include "spulibint.h"
#include <cbe_mfc.h>
#include <errno.h>
#include <sched.h>
#include <signal.h>

SPU_INFO spu_info[NUM_SPU];

static void spu_handle_complete(uint32_t spu_id, uint32_t mask);
static void spu_handle_internal_complete(SPU_INFO *spu, uint32_t mask);

/*-----------------------------------------------------------------------------
 * spu_new_group
 *---------------------------------------------------------------------------*/
SPU_CMD_GROUP *
spu_new_group(uint32_t spu_id, uint32_t gid)
{
  SPU_CMD_GROUP *g;

  pcheck(gid < SPU_MAX_CMD_GROUPS);
  g = &spu_info[spu_id].cmd_groups[gid];
  g->size = 0;
  g->end = &spu_info[spu_id].cmd_group_data[gid];

  return g;
}

/*-----------------------------------------------------------------------------
 * spu_issue_group
 *---------------------------------------------------------------------------*/
void
spu_issue_group(uint32_t spu_id, uint32_t gid, SPU_ADDRESS da)
{
  SPU_INFO *spu = &spu_info[spu_id];
  SPU_CMD_GROUP *g;

  pcheck(gid < SPU_INT_MAX_CMD_GROUPS);
  g = &spu->cmd_groups[gid];
  pcheck(g->size != 0);

#if CHECK
  if (gid < SPU_MAX_CMD_GROUPS) {
    // Command group is from scheduler. Make sure no command IDs in the group
    // are internal, pending, or completed and not yet cleared.

    uint32_t invalid_mask = spu->internal_mask | spu->completed_mask;

    for (SPU_CMD_HEADER *cmd = spu_first_command(g);
         cmd != NULL;
         cmd = spu_next_command(g, cmd)) {
      check(((spu->issued_mask | invalid_mask) & (1 << cmd->id)) == 0);
      spu->issued_mask |= (1 << cmd->id);
    }
  }
#if DEBUG
  else {
    // Command group is internal. Make sure all command IDs in the group are
    // marked internal and none are pending.

    uint32_t invalid_mask = ~spu->internal_mask;

    for (SPU_CMD_HEADER *cmd = spu_first_command(g);
         cmd != NULL;
         cmd = spu_next_command(g, cmd)) {
      assert(((spu->issued_mask | invalid_mask) & (1 << cmd->id)) == 0);
      spu->issued_mask |= (1 << cmd->id);
    }
  }
#endif // DEBUG
#endif // CHECK

  // Make sure SPU inbound mailbox has free entry. I don't think real
  // schedulers would issue too many requests at once. If this ever trips,
  // would have to implement some sort of queue.
  check(_spe_in_mbox_status(spu->control) != 0);
  // Send request to SPU.
  _spe_in_mbox_write(spu->control,
                     spu_cmd_compose_req(spu_lsa(spu_id, da), gid, g->size));
}

/*-----------------------------------------------------------------------------
 * ppu_dt_wait_spu
 *
 * Sets a PPU data transfer command to wait on a SPU command ID. Returns
 * pointer to memory for data transfer parameters.
 *---------------------------------------------------------------------------*/
PPU_DT_PARAMS *
ppu_dt_wait_spu(uint32_t spu_id, uint32_t spu_cmd_id, uint32_t tag)
{
  SPU_INFO *spu = &spu_info[spu_id];

  // Make sure nothing is already waiting on the SPU command ID.
  pcheck(spu_cmd_id < SPU_MAX_COMMANDS);
  check((spu->dt_mask & (1 << spu_cmd_id)) == 0);

  spu->dt_mask |= (1 << spu_cmd_id);
  spu->dt_waiting[spu_cmd_id].tag = tag;
  return &spu->dt_waiting[spu_cmd_id].params;
}

/*-----------------------------------------------------------------------------
 * spu_handle_complete
 *
 * Processes a command completion message from a SPU. Finishes any waiting PPU
 * data transfers and runs all callbacks.
 *---------------------------------------------------------------------------*/
static void
spu_handle_complete(uint32_t spu_id, uint32_t mask)
{
  SPU_INFO *spu = &spu_info[spu_id];
  uint32_t internal_mask;
  uint32_t dt_mask;

  internal_mask = mask & spu->internal_mask;
  dt_mask = mask & spu->dt_mask;

  // Finish PPU data transfer commands that are waiting on the SPU command IDs.
  if (dt_mask != 0) {
    // Clear waiting mask before running PPU callbacks, which may start new
    // data transfers waiting on the completed SPU command IDs.
    spu->dt_mask &= ~dt_mask;

    while (dt_mask != 0) {
      uint32_t spu_cmd_id = count_ls_zeros(dt_mask);
      uint32_t spu_cmd_bit = (1 << spu_cmd_id);

      ppu_finish_dt(&spu->dt_waiting[spu_cmd_id].params);

      // Run PPU callback if command is not internal.
      if (((internal_mask & spu_cmd_bit) != 0) &&
          (spu->ppu_dt_complete_cb != NULL)) {
        (*spu->ppu_dt_complete_cb)(spu->dt_waiting[spu_cmd_id].tag);
      }

      dt_mask &= ~spu_cmd_bit;
    }
  }

  // Clear issued mask before running anything that may issue more.
  IF_CHECK(spu->issued_mask &= ~mask);

  // Run library operation handlers.
  if (internal_mask != 0) {
    spu_handle_internal_complete(spu, internal_mask);
  }

  mask = mask & ~internal_mask;

  // Run SPU callback.
  if (mask != 0) {
    spu->completed_mask |= mask;

    if (spu->spu_complete_cb != NULL) {
      (*spu->spu_complete_cb)(spu_id, mask, spu->completed_mask);
    }
  }
}

/*-----------------------------------------------------------------------------
 * spu_handle_internal_complete
 *
 * Dispatches completed internal command IDs to operation handlers.
 *---------------------------------------------------------------------------*/

static void
spu_handle_internal_complete(SPU_INFO *spu, uint32_t mask)
{
  EXTENDED_OP *op;
  EXTENDED_OP **op_ptr;

  assert(spu->ext_ops != NULL);
  op_ptr = &spu->ext_ops;

  // Check each operation waiting on commands on this SPU and call its handler
  // if any of its command IDs just completed.
  while ((mask != 0) && ((op = *op_ptr) != NULL)) {
    uint32_t op_mask = mask & op->spu_cmd_mask;

    if (op_mask != 0) {
      mask &= ~op_mask;

      // Call handler.
      if (op->handler(op->data, op_mask)) {
        // Operation is done.

        GENERIC_COMPLETE_CB *cb = op->cb;
        uint32_t tag = op->tag;

        // Remove operation from list and mark its command IDs as available to
        // scheduler.
        *op_ptr = op->next;
        spu->internal_mask &= ~op->spu_cmd_mask;
        free(op);

        // Run callback.
        cb(tag);
        continue;
      }
    }

    op_ptr = &op->next;
  }

  // Every internal ID should belong to some operation.
  assert(mask == 0);
}

/*-----------------------------------------------------------------------------
 * spu_new_int_group
 *
 * Returns a new uninitialized internal command group for the specified SPU.
 * Fails if all internal groups are being used.
 *---------------------------------------------------------------------------*/

SPU_CMD_GROUP *
spu_new_int_group(SPU_INFO *spu)
{
  SPU_CMD_GROUP *g;
  check(spu->free_int_group != NULL);
  g = spu->free_int_group;
  spu->free_int_group = (SPU_CMD_GROUP *)g->end;
  return g;
}

/*-----------------------------------------------------------------------------
 * spu_free_int_group
 *
 * Frees an internal command group.
 *---------------------------------------------------------------------------*/

void
spu_free_int_group(SPU_CMD_GROUP *g)
{
  SPU_INFO *spu = &spu_info[g->spu_id];
  g->end = spu->free_int_group;
  spu->free_int_group = g;
}

/*-----------------------------------------------------------------------------
 * spu_new_ext_op
 *
 * Sets up a new library-handled operation that waits on command IDs in the
 * specified bitmap on the specified SPU. Returns pointer to memory for
 * operation-specific data.
 *
 * This must be called before issuing any commands in the operation.
 *---------------------------------------------------------------------------*/

void *
spu_new_ext_op(SPU_INFO *spu, uint32_t spu_cmd_mask,
               EXTENDED_OP_HANDLER *handler, GENERIC_COMPLETE_CB *cb,
               uint32_t tag, uint32_t data_size)
{
  EXTENDED_OP *op;

  // Make sure the specified command IDs are not being used and mark them as
  // internal.
  check(((spu->internal_mask | spu->completed_mask | spu->issued_mask) &
         spu_cmd_mask) == 0);
  spu->internal_mask |= spu_cmd_mask;

  // Allocate and initialize entry for operation.
  op = (EXTENDED_OP *)malloc(sizeof(*op) + data_size);
  check(op != NULL);
  op->spu_cmd_mask = spu_cmd_mask;
  op->handler = (EXTENDED_OP_HANDLER *)handler;
  op->cb = cb;
  op->tag = tag;

  // Add operation to list of operations waiting on commands on this SPU.
  op->next = spu->ext_ops;
  spu->ext_ops = op;

  return op->data;
}

/*-----------------------------------------------------------------------------
 * Misc.
 *---------------------------------------------------------------------------*/

extern spe_program_handle_t spulib_spu;

static spe_gid_t spulib_spe_gid;

static uint32_t spulib_next_poll;

/*-----------------------------------------------------------------------------
 * spulib_init
 *---------------------------------------------------------------------------*/
bool_t
spulib_init()
{
  SPU_PARAMS spu_params[NUM_SPU];

  // Initialize SPU structures.

  for (uint32_t spu_id = 0; spu_id < NUM_SPU; spu_id++) {
    SPU_INFO *spu = &spu_info[spu_id];

    // Set SPU ID.
    spu->spu_id = spu_id;

    spu->data_start = ROUND_UP(spu->data_start, CACHE_SIZE);
    spu->data_size = LS_SIZE - spu->data_start;

    // Set SPU and command group IDs for all command groups.
    for (uint32_t gid = 0; gid < SPU_INT_MAX_CMD_GROUPS; gid++) {
      SPU_CMD_GROUP *g = &spu->cmd_groups[gid];
      g->spu_id = spu_id;
      g->gid = gid;
    }

    // Initialize list of free internal command groups.
    for (uint32_t i = SPU_INT_CMD_GROUP; i < SPU_INT_MAX_CMD_GROUPS - 1; i++) {
      spu->cmd_groups[i].end = &spu->cmd_groups[i + 1];
    }

    spu->cmd_groups[SPU_INT_MAX_CMD_GROUPS - 1].end = NULL;
    spu->free_int_group = &spu->cmd_groups[SPU_INT_CMD_GROUP];
  }

  // Everything else is automatically intialized to 0.

  // Initialize SPUs.

  // Create thread group and threads.
  spulib_spe_gid = spe_create_group(SCHED_OTHER, 0, FALSE);
  if (spulib_spe_gid == NULL) {
    return FALSE;
  }

  for (uint32_t i = 0; i < NUM_SPU; i++) {
    SPU_INFO *spu = &spu_info[i];
    spu->speid = spe_create_thread(spulib_spe_gid, spu->program, NULL, NULL,
                                   -1, SPE_MAP_PS);
    if (spu->speid == NULL) {
      // Kill previously created threads and report failure.
      while (i != 0) {
        i--;
        if (spe_kill(spu->speid, SIGKILL) == 0) {
          while ((spe_wait(spu->speid, NULL, 0) == 0) || (errno == EAGAIN));
        }
      }

      spe_destroy_group(spulib_spe_gid);
      return FALSE;
    }
  }

  // Initialize SPU info and SPU's event loop.
  for (uint32_t i = 0; i < NUM_SPU; i++) {
    SPU_INFO *spu = &spu_info[i];

    spu->data_addr = spe_get_ls(spu->speid) + spu->data_start;
    spu->control = (spe_spu_control_area_t *)
      spe_get_ps_area(spu->speid, SPE_CONTROL_AREA);

    // Set up SPU parameters and send address to SPU.
    spu_params[i].cmd_group_table = spu->cmd_group_data;
    _spe_in_mbox_write(spu->control, (uintptr_t)&spu_params[i]);
  }

  // Wait for all SPUs to signal ready.
  for (uint32_t i = 0; i < NUM_SPU; i++) {
    _spe_out_mbox_read(spu_info[i].control);
  }

  return TRUE;
}

/*-----------------------------------------------------------------------------
 * spulib_poll
 *---------------------------------------------------------------------------*/
bool_t
spulib_poll()
{
  uint32_t i = spulib_next_poll;

  do {
    if (_spe_out_mbox_status(spu_info[i].control) != 0) {
      spu_handle_complete(i, _spe_out_mbox_read(spu_info[i].control));

      spulib_next_poll = (i == NUM_SPU - 1 ? 0 : i + 1);
      return TRUE;
    }

    i = (i == NUM_SPU - 1 ? 0 : i + 1);
  } while (i != spulib_next_poll);

  return FALSE;
}

/*-----------------------------------------------------------------------------
 * spulib_wait
 *---------------------------------------------------------------------------*/

void
spulib_wait(uint32_t spu_id, uint32_t mask)
{
  spulib_poll_while(!spu_check_and_ack_completed(spu_id, mask));
}

/*-----------------------------------------------------------------------------
 * spulib_set_all_spu_complete_cb
 *---------------------------------------------------------------------------*/

void
spulib_set_all_spu_complete_cb(SPU_COMPLETE_CB *cb)
{
  for (uint32_t i = 0; i < NUM_SPU; i++) {
    spu_info[i].spu_complete_cb = cb;
  }
}

/*-----------------------------------------------------------------------------
 * spulib_set_all_ppu_dt_complete_cb
 *---------------------------------------------------------------------------*/

void
spulib_set_all_ppu_dt_complete_cb(GENERIC_COMPLETE_CB *cb)
{
  for (uint32_t i = 0; i < NUM_SPU; i++) {
    spu_info[i].ppu_dt_complete_cb = cb;
  }
}
