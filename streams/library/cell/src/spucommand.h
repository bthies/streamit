/*-----------------------------------------------------------------------------
 * spucommand.h
 *
 * SPU command definitions.
 *---------------------------------------------------------------------------*/

#ifndef _SPULIB_SPUCOMMAND_H_
#define _SPULIB_SPUCOMMAND_H_

#undef C_FILE
#define C_FILE spulib_spucommand_h

#ifdef __SPU__  // SPU
#define SPU_PREFIX(name) name
#else           // PPU
#define SPU_PREFIX(name) spu_##name
#endif

// Maximum number of pending commands per SPU. IDs range from 0 to (this - 1).
#define SPU_MAX_COMMANDS 32

// Command types.
// Filter commands.
#define SPU_CMD_LOAD_DATA             0
#define SPU_CMD_FILTER_LOAD           1
#define SPU_CMD_FILTER_UNLOAD         2
#define SPU_CMD_FILTER_ATTACH_INPUT   3
#define SPU_CMD_FILTER_ATTACH_OUTPUT  4
#define SPU_CMD_FILTER_RUN            5
// Buffer commands.
#define SPU_CMD_BUFFER_ALLOC          6
#define SPU_CMD_BUFFER_ALIGN          7
// Data transfer commands.
#define SPU_CMD_DT_IN_FRONT           8
#define SPU_CMD_DT_IN_BACK            9
#define SPU_CMD_DT_OUT_FRONT         10
#define SPU_CMD_DT_OUT_BACK          11
#define SPU_CMD_DT_OUT_FRONT_PPU     12
#define SPU_CMD_DT_OUT_BACK_PPU      13
// Number of command types.
#define SPU_NUM_CMD_TYPES            14

/*
 * All command structures (except the SPU's internal worker commands) are
 * 16-byte aligned and padded. Code that dynamically allocates/places
 * structures with required alignments must ensure they are met.
 *
 * (*) and (**) indicate fields for use by SPU code. (**) fields must be
 * initialized by PPU code.
 */

// Common header for all commands. Each command can have up to 7 backward
// dependencies. Once added to the dependency graph, each command must not
// accumulate more than 7 forward dependencies.
//
// 12 bytes in size (not padded), 16-byte aligned
typedef struct _SPU_CMD_HEADER {
  uint8_t type;               // Type of command
  uint8_t id;                 // ID of command
  uint8_t num_back_deps;      // Number of backward deps
  uint8_t num_forward_deps;   // (**) Number of forward deps - initialize to 0
  // IDs of dependencies. These are backward deps when command is issued,
  // become filled in with forward deps once added to dependency graph.
  uint8_t deps[7];
  uint8_t next;               // (*) ID of next command in run queue/wait list
} SPU_CMD_HEADER QWORD_ALIGNED;

C_ASSERT(sizeof(SPU_CMD_HEADER) == 12);

// Maximum number of backward/forward deps a command can have.
#define SPU_CMD_MAX_DEPS arraysize(((SPU_CMD_HEADER *)0)->deps)

/*-----------------------------------------------------------------------------
 * Filter commands.
 *---------------------------------------------------------------------------*/

// load_data command
//
// Copies data into local store. Addresses must be 16-byte aligned and size
// must be 16-byte padded.
typedef struct _SPU_LOAD_DATA_CMD {
  SPU_CMD_HEADER header;
// 12
  LS_ADDRESS dest_lsa;    // LS address to copy data to
// 16
  MEM_ADDRESS src_addr;   // Address to copy data from
  uint32_t num_bytes;     // Size of data
// 24
  uint8_t state;          // (**)
  SPU_DMA_TAG tag;        // (*) Tag used for transferring data
  uint8_t _padding[6];
} QWORD_ALIGNED SPU_LOAD_DATA_CMD;

C_ASSERT(sizeof(SPU_LOAD_DATA_CMD) == 32);

// Info about SPU filters needed by SPU library code.
//
// 20 bytes in size (not padded), 16-byte aligned
typedef struct _SPU_INT_FILTER_DESC {
  LS_ADDRESS work_func;     // Entry point of work function
  LS_ADDRESS param;         // Pointer parameter passed to work function
  uint32_t state_size;      // Size of filter state (must be 16-byte padded)
  MEM_ADDRESS state_addr;   // Address to copy state from
// 16
  uint8_t num_inputs;       // Number of input tapes
  uint8_t num_outputs;      // Number of output tapes
  uint8_t _padding[2];
} SPU_INT_FILTER_DESC QWORD_ALIGNED;

C_ASSERT(sizeof(SPU_INT_FILTER_DESC) == 20);

// Properly padded version of SPU_INT_FILTER_DESC for use by scheduler code.
//
// 16-byte aligned and padded
typedef struct _SPU_FILTER_DESC {
  LS_ADDRESS work_func;     // Entry point of work function
  LS_ADDRESS param;         // Pointer parameter passed to work function
  uint32_t state_size;      // Size of filter state (must be 16-byte padded)
  MEM_ADDRESS state_addr;   // Address to copy state from
// 16
  uint8_t num_inputs;       // Number of input tapes
  uint8_t num_outputs;      // Number of output tapes
} QWORD_ALIGNED SPU_FILTER_DESC;

#if CHECK
#define SPU_FILTER_CB_SIZE 48
#else
#define SPU_FILTER_CB_SIZE 32
#endif

/*-----------------------------------------------------------------------------
 * spu_cmd_get_filt_cb_size
 *
 * Returns the size of the SPU FILTER_CB structure for a SPU filter with the
 * specified properties.
 *---------------------------------------------------------------------------*/
static INLINE uint32_t
SPU_PREFIX(cmd_get_filt_cb_size)(uint8_t num_inputs, uint8_t num_outputs,
                                 uint32_t state_size)
{
  pcheck((state_size & QWORD_MASK) == 0);
  return ROUND_UP(SPU_FILTER_CB_SIZE +
                    (num_inputs + num_outputs) * sizeof(LS_ADDRESS),
                  QWORD_SIZE) +
         state_size;
}

// filter_load command
//
// Loads a filter.
typedef struct _SPU_FILTER_LOAD_CMD {
  SPU_CMD_HEADER header;
// 12
  LS_ADDRESS filt;           // LS address of filter CB
// 16
  SPU_INT_FILTER_DESC desc;  // Filter description
// 36
  uint8_t state;             // (**)
  SPU_DMA_TAG tag;           // (*) Tag used for copying filter state
  uint8_t _padding0[2];
// 40
  LS_ADDRESS state_lsa;      // (*)
  uint32_t _padding1;
} QWORD_ALIGNED SPU_FILTER_LOAD_CMD;

C_ASSERT(sizeof(SPU_FILTER_LOAD_CMD) == 48);

// filter_unload command
//
// Unloads a filter.
typedef struct _SPU_FILTER_UNLOAD_CMD {
  SPU_CMD_HEADER header;
// 12
  LS_ADDRESS filt;        // LS address of filter CB
// 16
  uint8_t state;          // (**)
  SPU_DMA_TAG tag;        // (*) Tag used for writing back filter state
  uint8_t _padding[14];
} QWORD_ALIGNED SPU_FILTER_UNLOAD_CMD;

C_ASSERT(sizeof(SPU_FILTER_UNLOAD_CMD) == 32);

// filter_attach_input/filter_attach_output commands
//
// Attaches an input/output tape of a filter to a buffer.
typedef struct _SPU_FILTER_ATTACH_CMD {
  SPU_CMD_HEADER header;
// 12
  LS_ADDRESS filt;        // LS address of filter CB
// 16
  uint8_t tape_id;        // Index of tape
  uint8_t _padding0[3];
// 20
  LS_ADDRESS buf_data;    // LS address of buffer data
  uint32_t _padding1[2];
} QWORD_ALIGNED SPU_FILTER_ATTACH_CMD;

C_ASSERT(sizeof(SPU_FILTER_ATTACH_CMD) == 32);

typedef SPU_FILTER_ATTACH_CMD SPU_FILTER_ATTACH_INPUT_CMD;
typedef SPU_FILTER_ATTACH_CMD SPU_FILTER_ATTACH_OUTPUT_CMD;

// filter_run command
//
// Runs a filter for a specified number of iterations.
typedef struct _SPU_FILTER_RUN_CMD {
  SPU_CMD_HEADER header;
// 12
  LS_ADDRESS filt;        // LS address of filter CB
// 16
  uint32_t iters;         // Number of iterations to run for
#if CHECK
  uint8_t state;          // (**)
  uint8_t _padding[11];
#else
  uint8_t _padding[12];
#endif
} QWORD_ALIGNED SPU_FILTER_RUN_CMD;

C_ASSERT(sizeof(SPU_FILTER_RUN_CMD) == 32);

/*-----------------------------------------------------------------------------
 * Buffer commands.
 *---------------------------------------------------------------------------*/

// buffer_alloc command
//
// Allocates a buffer. Data address must be 128-byte aligned and size must be
// power of 2 and at least 128 bytes.
typedef struct _SPU_BUFFER_ALLOC_CMD {
  SPU_CMD_HEADER header;
// 12
  LS_ADDRESS buf_data;    // LS address of buffer data
// 16
  uint32_t mask;          // Mask for buffer size (size in bytes - 1) 
  uint32_t data_offset;   // Initial value of head/tail pointers
// 24
  uint32_t _padding[2];
} QWORD_ALIGNED SPU_BUFFER_ALLOC_CMD;

C_ASSERT(sizeof(SPU_BUFFER_ALLOC_CMD) == 32);

// buffer_align command
//
// Adjusts head and tail pointers of an empty buffer.
typedef struct _SPU_BUFFER_ALIGN_CMD {
  SPU_CMD_HEADER header;
// 12
  LS_ADDRESS buf_data;    // LS address of buffer data
// 16
  uint32_t data_offset;   // New value of head/tail pointers
  uint32_t _padding1[3];
} QWORD_ALIGNED SPU_BUFFER_ALIGN_CMD;

C_ASSERT(sizeof(SPU_BUFFER_ALIGN_CMD) == 32);

/*-----------------------------------------------------------------------------
 * Data transfer commands.
 *---------------------------------------------------------------------------*/

// 8 bytes in size, 16-byte aligned
typedef union _OUT_DTCB {
  struct {
    uint32_t head;
    uint32_t tail;
  };
  uint64_t data;
} OUT_DTCB QWORD_ALIGNED;

// dt_out_front/dt_out_back commands
typedef struct _SPU_DT_OUT_CMD {
  SPU_CMD_HEADER header;
// 12
  LS_ADDRESS buf_data;        // LS address of buffer data (source)
// 16
  MEM_ADDRESS dest_buf_data;  // Address of destination buffer data
  uint32_t num_bytes;         // Number of bytes to transfer
// 24
  uint8_t state;              // (**)
  SPU_DMA_TAG tag;            // (*) Tag for writing control block
  uint8_t _padding0[6];
// 32
  OUT_DTCB out_dtcb;          // (*) Control block for destination buffer
// 40
  uint32_t _padding1[2];
} QWORD_ALIGNED SPU_DT_OUT_CMD;

C_ASSERT(sizeof(SPU_DT_OUT_CMD) == 48);

// dt_in_front/dt_in_back commands
typedef struct _SPU_DT_IN_CMD {
  SPU_CMD_HEADER header;
// 12
  LS_ADDRESS buf_data;            // LS address of buffer data (destination)
// 16
  MEM_ADDRESS src_buf_data;       // Address of source buffer data
  uint32_t src_buf_mask;          // Mask for source buffer (size - 1)
  uint32_t num_bytes;             // Number of bytes to transfer
// 28
  uint8_t state;                  // (**)
  SPU_DMA_TAG tag;                // (*) Tag for copying data and writing ack
  uint8_t _padding0[2];
// 32
  uint32_t _padding1[2];
// 40
  union {
    uint32_t out_ack;             // (*) Acknowledgement to source buffer
    uint32_t src_head;            // (* dt_in_back only)
    uint32_t src_tail;            // (* dt_in_front only)
  };
// 44
  uint32_t copy_bytes;            // (*)
// 48
  vec16_uint8_t ua_data;          // (*)
} QWORD_ALIGNED SPU_DT_IN_CMD;

C_ASSERT(sizeof(SPU_DT_IN_CMD) == 64);

// dt_out_front_ppu/dt_out_back_ppu commands
typedef struct _SPU_DT_OUT_PPU_CMD {
  SPU_CMD_HEADER header;
// 12
  LS_ADDRESS buf_data;        // LS address of buffer data (source)
// 16
  MEM_ADDRESS dest_buf_data;  // Address of destination buffer data
  uint32_t dest_buf_mask;     // Mask for destination buffer (size - 1)
  uint32_t num_bytes;         // Number of bytes to transfer
// 28
  uint8_t state;              // (**)
  SPU_DMA_TAG tag;            // (*) Tag for copying data
  uint8_t _padding0[2];
// 32
  uint32_t dest_tail;         // (*)
  uint32_t copy_bytes;        // (*)
  uint32_t _padding1[2];
} QWORD_ALIGNED SPU_DT_OUT_PPU_CMD;

C_ASSERT(sizeof(SPU_DT_OUT_PPU_CMD) == 48);

typedef SPU_DT_IN_CMD       SPU_DT_IN_FRONT_CMD;
typedef SPU_DT_IN_CMD       SPU_DT_IN_BACK_CMD;
typedef SPU_DT_OUT_CMD      SPU_DT_OUT_FRONT_CMD;
typedef SPU_DT_OUT_CMD      SPU_DT_OUT_BACK_CMD;
typedef SPU_DT_OUT_PPU_CMD  SPU_DT_OUT_FRONT_PPU_CMD;
typedef SPU_DT_OUT_PPU_CMD  SPU_DT_OUT_BACK_PPU_CMD;

// Size of each command structure.
extern
#ifdef __SPU__  // SPU
uint16_t
#else           // PPU
uint32_t
#endif
spu_cmd_size[SPU_NUM_CMD_TYPES];

/*-----------------------------------------------------------------------------
 * spu_cmd_get_size
 *
 * Returns the size of the specified command structure. Currently no commands
 * are variable-size.
 *---------------------------------------------------------------------------*/
static INLINE uint32_t
SPU_PREFIX(cmd_get_size)(SPU_CMD_HEADER *cmd)
{
  return spu_cmd_size[cmd->type];
}

// Initial parameters to SPU.
//
// 16-byte aligned and padded
typedef struct _SPU_PARAMS {
  uint8_t id;
  // Address of PPU's command group table for this SPU.
  MEM_ADDRESS cmd_group_table;
} QWORD_ALIGNED SPU_PARAMS;

/*-----------------------------------------------------------------------------
 * Request is a 32-bit mailbox message that notifies SPU of a new group of
 * commands.
 *
 * Packed fields in request (LSB first):
 * - LS address to copy command data to, without lowest 4 bits. Address must be
 *   16-byte aligned.
 *   256 KB -> 18 bits, - 4 = 14 bits
 * - Entry in PPU command group table to copy command data from.
 *   32 entries -> 5 bits.
 * - Size of command group, without lowest 4 bits. Size must be 16-byte padded.
 *   2 KB -> 11 bits, - 4, +1 (MSB) = 8 bits
 *---------------------------------------------------------------------------*/

// Maximum number of command groups available for use by scheduler code.
#define SPU_MAX_CMD_GROUPS      32
// ID of first internal command group.
#define SPU_INT_CMD_GROUP       SPU_MAX_CMD_GROUPS
// Maximum number of command groups (including internal).
#define SPU_INT_MAX_CMD_GROUPS  64
// Maximum size of a command group. This is the size of each PPU command group
// table entry.
#define SPU_CMD_GROUP_MAX_SIZE  2048
#define SPU_CMD_GROUP_SHIFT     11

// LS address field in packed request.
#define SPU_CMD_REQ_LSA_SHIFT   0
#define SPU_CMD_REQ_LSA_BITS    14
// Table entry field in packed request.
#define SPU_CMD_REQ_ENTRY_SHIFT 14
#define SPU_CMD_REQ_ENTRY_BITS  6
// Size field in packed request.
#define SPU_CMD_REQ_SIZE_SHIFT  20
#define SPU_CMD_REQ_SIZE_BITS   8

#define SPU_CMD_REQ_LSA_MASK    ((1 << SPU_CMD_REQ_LSA_BITS) - 1)
#define SPU_CMD_REQ_ENTRY_MASK  ((1 << SPU_CMD_REQ_ENTRY_BITS) - 1)
#define SPU_CMD_REQ_SIZE_MASK   ((1 << SPU_CMD_REQ_SIZE_BITS) - 1)

#ifdef __SPU__  // SPU

/*-----------------------------------------------------------------------------
 * cmd_get_req_lsa
 *
 * Returns data LS address specified by a request.
 *---------------------------------------------------------------------------*/
static INLINE void *
cmd_get_req_lsa(uint32_t req)
{
  return (void *)((req & SPU_CMD_REQ_LSA_MASK) << QWORD_SHIFT);
}

/*-----------------------------------------------------------------------------
 * cmd_get_req_entry
 *
 * Returns table entry specified by a request.
 *---------------------------------------------------------------------------*/
static INLINE uint32_t
cmd_get_req_entry(uint32_t req)
{
  return (req >> SPU_CMD_REQ_ENTRY_SHIFT) & SPU_CMD_REQ_ENTRY_MASK;
}

/*-----------------------------------------------------------------------------
 * cmd_get_req_size
 *
 * Returns data size specified by a request.
 *---------------------------------------------------------------------------*/
static INLINE uint32_t
cmd_get_req_size(uint32_t req)
{
  return (req >> SPU_CMD_REQ_SIZE_SHIFT) << QWORD_SHIFT;
}

#else           // PPU

/*-----------------------------------------------------------------------------
 * spu_cmd_compose_request
 *
 * Packs fields into a request message.
 *---------------------------------------------------------------------------*/
static INLINE uint32_t
spu_cmd_compose_req(LS_ADDRESS lsa, uint32_t entry, uint32_t size)
{
  pcheck((lsa & QWORD_MASK) == 0);
  assert((entry < SPU_INT_MAX_CMD_GROUPS) &&
         (size <= SPU_CMD_GROUP_MAX_SIZE) && ((size & QWORD_MASK) == 0));
  return (lsa >> QWORD_SHIFT) | (entry << SPU_CMD_REQ_ENTRY_SHIFT) |
    (size << (SPU_CMD_REQ_SIZE_SHIFT - QWORD_SHIFT));
}

#endif

#ifdef __SPU__

#define MAX_COMMANDS              SPU_MAX_COMMANDS

// Command types.
// Filter commands.
#define CMD_LOAD_DATA             SPU_CMD_LOAD_DATA
#define CMD_FILTER_LOAD           SPU_CMD_FILTER_LOAD
#define CMD_FILTER_UNLOAD         SPU_CMD_FILTER_UNLOAD
#define CMD_FILTER_ATTACH_INPUT   SPU_CMD_FILTER_ATTACH_INPUT
#define CMD_FILTER_ATTACH_OUTPUT  SPU_CMD_FILTER_ATTACH_OUTPUT
#define CMD_FILTER_RUN            SPU_CMD_FILTER_RUN
// Buffer commands.
#define CMD_BUFFER_ALLOC          SPU_CMD_BUFFER_ALLOC
#define CMD_BUFFER_ALIGN          SPU_CMD_BUFFER_ALIGN
// Data transfer commands.
#define CMD_DT_IN_FRONT           SPU_CMD_DT_IN_FRONT
#define CMD_DT_IN_BACK            SPU_CMD_DT_IN_BACK
#define CMD_DT_OUT_FRONT          SPU_CMD_DT_OUT_FRONT
#define CMD_DT_OUT_BACK           SPU_CMD_DT_OUT_BACK
#define CMD_DT_OUT_FRONT_PPU      SPU_CMD_DT_OUT_FRONT_PPU
#define CMD_DT_OUT_BACK_PPU       SPU_CMD_DT_OUT_BACK_PPU
// Number of command types.
#define NUM_CMD_TYPES             SPU_NUM_CMD_TYPES

// Command structures.
// Command header.
#define CMD_HEADER                SPU_CMD_HEADER
#define CMD_MAX_DEPS              SPU_CMD_MAX_DEPS
// Filter commands.
#define LOAD_DATA_CMD             SPU_LOAD_DATA_CMD
#define FILTER_DESC               SPU_INT_FILTER_DESC
#define FILTER_LOAD_CMD           SPU_FILTER_LOAD_CMD
#define FILTER_UNLOAD_CMD         SPU_FILTER_UNLOAD_CMD
#define FILTER_ATTACH_INPUT_CMD   SPU_FILTER_ATTACH_INPUT_CMD
#define FILTER_ATTACH_OUTPUT_CMD  SPU_FILTER_ATTACH_OUTPUT_CMD
#define FILTER_RUN_CMD            SPU_FILTER_RUN_CMD
// Buffer commands.
#define BUFFER_ALLOC_CMD          SPU_BUFFER_ALLOC_CMD
#define BUFFER_ALIGN_CMD          SPU_BUFFER_ALIGN_CMD
// Data transfer commands.
#define DT_IN_FRONT_CMD           SPU_DT_IN_FRONT_CMD
#define DT_IN_BACK_CMD            SPU_DT_IN_BACK_CMD
#define DT_OUT_FRONT_CMD          SPU_DT_OUT_FRONT_CMD
#define DT_OUT_BACK_CMD           SPU_DT_OUT_BACK_CMD
#define DT_OUT_FRONT_PPU_CMD      SPU_DT_OUT_FRONT_PPU_CMD
#define DT_OUT_BACK_PPU_CMD       SPU_DT_OUT_BACK_PPU_CMD

#endif

#undef SPU_PREFIX

#endif
