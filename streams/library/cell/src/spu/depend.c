/*-----------------------------------------------------------------------------
 * depend.c
 *
 * Implementation of command and dependency tracking.
 *---------------------------------------------------------------------------*/

#include "defs.h"
#include <spu_mfcio.h>
#include "depend.h"
#include "dma.h"
#include "stats.h"

// File definition for C_ASSERT.
#undef C_FILE
#define C_FILE depend_c

// Types and IDs for internal worker and request handler commands.
#define CMD_WORKER          NUM_CMD_TYPES
#define CMD_REQ_HANDLER     (NUM_CMD_TYPES + 1)

#define CMD_WORKER_ID       MAX_COMMANDS
#define CMD_REQ_HANDLER_ID  (MAX_COMMANDS + 1)

// Parameters for worker command. Has no state.
//
// 16-byte aligned, not padded
typedef struct _WORKER_CMD {
  DECLARE_CMD_HEADER
} PACKED WORKER_CMD QWORD_ALIGNED;

C_ASSERT(sizeof(WORKER_CMD) == 12);

// Parameters for request handler command.
//
// 16-byte aligned, not padded
typedef struct _REQ_HANDLER_CMD {
  DECLARE_CMD_HEADER
// 12
  uint8_t state;
  DMA_TAG tag;          // Tag used for transferring request data
  uint8_t _padding[2];
// 16
  void *req_lsa;        // LS address of request data
  uint32_t req_size;    // Size of request data
} PACKED REQ_HANDLER_CMD QWORD_ALIGNED;

C_ASSERT(sizeof(REQ_HANDLER_CMD) == 24);

CMD_HEADER *commands[MAX_COMMANDS + 2];

uint8_t dep_cur_id;
uint8_t *dep_cur_ptr;
uint8_t *dep_next_ptr;

#if DEBUG
bool_t dep_dequeued;
#endif

// General parameters to SPU.
SPU_PARAMS dep_params;

// Bitmap of IDs of commands that have completed.
static uint32_t dep_completed_mask;
// Data for worker command.
static WORKER_CMD dep_worker;
// Data for request handler command.
static REQ_HANDLER_CMD dep_req;

static void run_worker();
static void run_req_handler();

/*-----------------------------------------------------------------------------
 * dep_get_req_addr
 *
 * Returns the address of an entry in the PPU's command group table.
 *---------------------------------------------------------------------------*/
static INLINE MEM_ADDRESS
dep_get_cmd_group_addr(uint32_t entry)
{
  return dep_params.cmd_group_table + (entry << SPU_CMD_GROUP_SHIFT);
}

/*-----------------------------------------------------------------------------
 * dep_complete_command
 *
 * Indicates that all processing for the current command has completed.
 *
 * Command handler must return afterwards.
 *---------------------------------------------------------------------------*/
void
dep_complete_command()
{
  CMD_HEADER *cmd = commands[dep_cur_id];
  uint8_t next_id = *dep_next_ptr;

  // Make sure this isn't being called where it shouldn't.
  assert((dep_cur_id < MAX_COMMANDS) && !dep_dequeued);
  IF_DEBUG(dep_dequeued = TRUE);

  // Update dependencies.
  for (uint8_t i = 0; i < cmd->num_forward_deps; i++) {
    uint8_t dep_id = cmd->deps[i];
    CMD_HEADER *dep = commands[dep_id];

    assert((dep != NULL) && (dep->num_back_deps != 0));
    dep->num_back_deps--;

    // Add commands that have no more dependencies to front of queue.
    if (dep->num_back_deps == 0) {
      dep->next = next_id;
      next_id = dep_id;

#if STATS_ENABLE
      if (UNLIKELY(dep->type == CMD_FILTER_RUN)) {
        stats_start_filter_run();
      }
#endif
    }
  }

  // Dequeue command and remove from dependency graph.
  *dep_cur_ptr = next_id;
  dep_next_ptr = dep_cur_ptr;
  commands[dep_cur_id] = NULL;

  // Mark ID as completed and notify PPU if possible.
  dep_completed_mask |= (1 << dep_cur_id);
  if (spu_stat_out_mbox() != 0) {
    spu_write_out_mbox(dep_completed_mask);
    dep_completed_mask = 0;
  }
}

/*-----------------------------------------------------------------------------
 * dep_add_command
 *
 * Adds the specified command to the dependency graph. Commands with all
 * dependencies completed are immediately added to the front of the run queue.
 *
 * This function modifies dep_next_ptr and should only be called by
 * run_req_handler.
 *---------------------------------------------------------------------------*/
static INLINE void
dep_add_command(CMD_HEADER *cmd)
{
  uint8_t cmd_id = cmd->id;
  uint8_t num_back_deps;

  // Validate command header.
  assert((cmd->type < NUM_CMD_TYPES) && (cmd_id < MAX_COMMANDS) &&
         ((cmd->num_back_deps <= CMD_MAX_DEPS) ||
          ((cmd->num_back_deps <= CMD_MAX_DEPS_LARGE) &&
           ((CMD_LARGE_HEADER_TYPES & (1 << cmd->type)) != 0))) &&
         (cmd->num_forward_deps == 0));
  // New command must not have same ID as a pending command or a completed
  // command not yet reported to PPU.
  assert((commands[cmd_id] == NULL) &&
         ((dep_completed_mask & (1 << cmd_id)) == 0));

  // Add command to dependency graph. Backward dependencies are changed to
  // forward dependencies.
  commands[cmd_id] = cmd;

  num_back_deps = cmd->num_back_deps;

  for (uint8_t i = 0; i < num_back_deps; i++) {
    CMD_HEADER *dep;

    pcheck(cmd->deps[i] < MAX_COMMANDS);
    dep = commands[cmd->deps[i]];

    if (dep == NULL) {
      // Command IDs that don't exist are considered to have completed.
      cmd->num_back_deps--;
    } else {
      check((dep->num_forward_deps < CMD_MAX_DEPS) ||
            ((dep->num_forward_deps < CMD_MAX_DEPS_LARGE) &&
             ((CMD_LARGE_HEADER_TYPES & (1 << dep->type)) != 0)));
      dep->deps[dep->num_forward_deps++] = cmd_id;
    }
  }

  // Add command to front of queue if no more dependencies.
  if (cmd->num_back_deps == 0) {
    cmd->next = *dep_next_ptr;
    *dep_next_ptr = cmd_id;
    dep_next_ptr = &cmd->next;

#if STATS_ENABLE
    if (UNLIKELY(cmd->type == CMD_FILTER_RUN)) {
      stats_start_filter_run();
    }
#endif
  }
}

/*-----------------------------------------------------------------------------
 * run_worker
 *
 * Command handler for the worker command (CMD_WORKER). Processes DMA
 * completions and notifies PPU of completed commands.
 *---------------------------------------------------------------------------*/
static void
run_worker()
{
  // Notify PPU of completed command IDs if necessary.
  if ((dep_completed_mask != 0) && (spu_stat_out_mbox() != 0)) {
    spu_write_out_mbox(dep_completed_mask);
    dep_completed_mask = 0;
  }

  // Check for DMA completions.
  if (dma_active_tags != 0) {
    uint32_t completed_tags;

    // Read completed tags.
    mfc_write_tag_mask(dma_active_tags);
    completed_tags = mfc_read_tag_status_immediate();

    if (completed_tags != 0) {
      uint8_t next_id = *dep_next_ptr;

      // Mark tags as completed.
      dma_active_tags &= ~completed_tags;

      // Queue goes from:
      //   worker -> ...
      // to:
      //   worker -> waiting_avail_list -> waiting_complete(s) -> ...

      // Add commands that are waiting for these tags to front of queue.
      while (completed_tags != 0) {
        DMA_TAG tag = count_ls_zeros(completed_tags);
        uint8_t waiting_id = dma_waiting[tag];

        if (waiting_id != INVALID_COMMAND_ID) {
          dma_waiting[tag] = INVALID_COMMAND_ID;
          commands[waiting_id]->next = next_id;
          next_id = waiting_id;
        }

        completed_tags &= ~(1 << tag);
      }

      // Add commands that are waiting for MFC slots to front of queue.
      if (dma_waiting_avail != INVALID_COMMAND_ID) {
        // Enqueue waiting list.
        *dma_waiting_avail_ptr = next_id;
        next_id = dma_waiting_avail;

        // Clear waiting list.
        dma_waiting_avail = INVALID_COMMAND_ID;
        dma_waiting_avail_ptr = &dma_waiting_avail;
      }

      // Fix up next pointer.
      *dep_next_ptr = next_id;
    }
  }
}

/*-----------------------------------------------------------------------------
 * run_req_handler
 *
 * Command handler for the request handler command (CMD_REQ_HANDLER). Polls for
 * new commands from the PPU and adds them to the dependency graph.
 *---------------------------------------------------------------------------*/
static void
run_req_handler()
{
  switch (dep_req.state) {
  state_0:
  case 0:
    // Wait for request.

    if (spu_stat_in_mbox() == 0) {
      return;
    }

    stats_receive_command();
    dep_req.state = 1;

  case 1: {
    // Got request. Start DMA for request data.

    uint32_t req;

    // Wait for MFC slot.
    if (!dma_query_avail(1)) {
      dma_wait_avail();
      return;
    }

    // Read packed request data.
    req = spu_read_in_mbox();

    // Save LS and size for later.
    dep_req.req_lsa = cmd_get_req_lsa(req);
    dep_req.req_size = cmd_get_req_size(req);

    // Start DMA for request data and wait for it to complete.
    dma_get(dep_req.tag, dep_req.req_lsa,
            dep_get_cmd_group_addr(cmd_get_req_entry(req)), dep_req.req_size);

    dma_wait_complete(dep_req.tag);
    dep_req.state = 2;
    return;
  }

  case 2: {
    // Copied in request data. Add each new command to dependency graph.

    CMD_HEADER *cmd = (CMD_HEADER *)dep_req.req_lsa;
    uint32_t bytes_left = dep_req.req_size;

    while (bytes_left != 0) {
      uint32_t cmd_size = cmd_get_size(cmd);
      assert(bytes_left >= cmd_size);

      dep_add_command(cmd);
      cmd = (CMD_HEADER *)((void *)cmd + cmd_size);
      bytes_left -= cmd_size;
    }

    // Fix up next pointer (was changed by dep_add_command).
    dep_next_ptr = &dep_req.header.next;

    // Reset.
    dep_req.state = 0;
    goto state_0;
  }

  default:
    unreached();
  }
}

/*-----------------------------------------------------------------------------
 * dep_init
 *
 * Initializes the SPU and dependency system.
 *---------------------------------------------------------------------------*/
void
dep_init()
{
  MEM_ADDRESS param_addr;

  // Read in library parameters. Address of SPU_PARAMS structure is sent in
  // mailbox message.
  param_addr = spu_read_in_mbox();
  mfc_get(&dep_params, param_addr, sizeof(dep_params), 0, 0, 0);

  // Set up headers for worker and request handler.
  dep_worker.header.type = CMD_WORKER;
  dep_worker.header.id = CMD_WORKER_ID;
  commands[CMD_WORKER_ID] = &dep_worker.header;

  dep_req.header.type = CMD_REQ_HANDLER;
  dep_req.header.id = CMD_REQ_HANDLER_ID;
  commands[CMD_REQ_HANDLER_ID] = &dep_req.header;

  dep_worker.header.next = CMD_REQ_HANDLER_ID;
  dep_req.header.next = CMD_WORKER_ID;

  // Worker runs first - this doesn't really matter.
  dep_next_ptr = &dep_req.header.next;

  // Initialize state for request handler (worker has no state).
  dep_req.state = 0;
  dep_req.tag = dma_reserve_tag();
  assert(dep_req.tag != INVALID_TAG);

  // Wait for params to be copied in.
  mfc_write_tag_mask(1 << 0);
  mfc_read_tag_status_all();

  // Signal PPU.
  spu_write_out_mbox(0);
}

void run_load_data(LOAD_DATA_CMD *cmd);
void run_call_func(CALL_FUNC_CMD *cmd);
void run_filter_load(FILTER_LOAD_CMD *cmd);
void run_filter_unload(FILTER_UNLOAD_CMD *cmd);
void run_filter_attach_input(FILTER_ATTACH_INPUT_CMD *cmd);
void run_filter_attach_output(FILTER_ATTACH_OUTPUT_CMD *cmd);
void run_filter_run(FILTER_RUN_CMD *cmd);
void run_buffer_alloc(BUFFER_ALLOC_CMD *cmd);
void run_buffer_align(BUFFER_ALIGN_CMD *cmd);
// void run_dt_in_front(DT_IN_FRONT_CMD *cmd);
void run_dt_in_back(DT_IN_BACK_CMD *cmd);
void run_dt_out_front(DT_OUT_FRONT_CMD *cmd);
// void run_dt_out_back(DT_OUT_BACK_CMD *cmd);
void run_dt_out_front_ppu(DT_OUT_FRONT_PPU_CMD *cmd);
// void run_dt_out_back_ppu(DT_OUT_BACK_PPU_CMD *cmd);
#if STATS_ENABLE
void run_stats_print(STATS_PRINT_CMD *cmd);
#endif

/*-----------------------------------------------------------------------------
 * dep_execute
 *
 * Starts the round-robin scheduler. Does not return.
 *---------------------------------------------------------------------------*/
void
dep_execute()
{
  while (TRUE) {
    CMD_HEADER *cmd;

    // Get current command and update next pointer.
    dep_cur_ptr = dep_next_ptr;
    dep_cur_id = *dep_cur_ptr;
    cmd = commands[dep_cur_id];
    assert(cmd != NULL);
    dep_next_ptr = &cmd->next;

    IF_DEBUG(dep_dequeued = FALSE);

    // Dispatch on command type.
    switch (cmd->type) {
    case CMD_WORKER:
      run_worker();
      break;

    case CMD_REQ_HANDLER:
      run_req_handler();
      break;

    case CMD_NULL:
      dep_complete_command();
      break;

    case CMD_LOAD_DATA:
      run_load_data((LOAD_DATA_CMD *)cmd);
      break;

    case CMD_CALL_FUNC:
      run_call_func((CALL_FUNC_CMD *)cmd);
      break;

    case CMD_FILTER_LOAD:
      run_filter_load((FILTER_LOAD_CMD *)cmd);
      break;

    case CMD_FILTER_UNLOAD:
      run_filter_unload((FILTER_UNLOAD_CMD *)cmd);
      break;

    case CMD_FILTER_ATTACH_INPUT:
      run_filter_attach_input((FILTER_ATTACH_INPUT_CMD *)cmd);
      break;

    case CMD_FILTER_ATTACH_OUTPUT:
      run_filter_attach_output((FILTER_ATTACH_OUTPUT_CMD *)cmd);
      break;

    case CMD_FILTER_RUN:
      run_filter_run((FILTER_RUN_CMD *)cmd);
      break;

    case CMD_BUFFER_ALLOC:
      run_buffer_alloc((BUFFER_ALLOC_CMD *)cmd);
      break;

    case CMD_BUFFER_ALIGN:
      run_buffer_align((BUFFER_ALIGN_CMD *)cmd);
      break;

      // case CMD_DT_IN_FRONT:
      // run_dt_in_front((DT_IN_FRONT_CMD *)cmd);
      // break;

    case CMD_DT_IN_BACK:
      run_dt_in_back((DT_IN_BACK_CMD *)cmd);
      break;

    case CMD_DT_OUT_FRONT:
      run_dt_out_front((DT_OUT_FRONT_CMD *)cmd);
      break;

      // case CMD_DT_OUT_BACK:
      // run_dt_out_back((DT_OUT_BACK_CMD *)cmd);
      // break;

    case CMD_DT_OUT_FRONT_PPU:
      run_dt_out_front_ppu((DT_OUT_FRONT_PPU_CMD *)cmd);
      break;

      // case CMD_DT_OUT_BACK_PPU:
      // run_dt_out_back_ppu((DT_OUT_BACK_PPU_CMD *)cmd);
      // break;

#if STATS_ENABLE
    case CMD_STATS_PRINT:
      run_stats_print((STATS_PRINT_CMD *)cmd);
      break;
#endif

    default:
      unreached();
    }
  }
}
