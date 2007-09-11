/*-----------------------------------------------------------------------------
 * ext.h
 *
 * Public interface for library-handled operations. Automatically included by
 * spulib.h.
 *---------------------------------------------------------------------------*/

#ifndef _SPULIB_EXT_H_
#define _SPULIB_EXT_H_

// Resource configuration for operations.
typedef struct _EXT_SPU_LAYOUT {
  uint32_t spu_id;                // ID of SPU

  // Operation requires following resources on SPU, which must be available for
  // duration:
  // - 6 contiguous command IDs starting from <cmd_id>. Scheduler cannot issue
  //   commands with these IDs while operation is in progress.
  // - 512 bytes starting at data address <da> (must be 128-byte aligned).
  uint32_t cmd_id;
  SPU_ADDRESS da;

  // TRUE indicates remote input buffer is on PPU, FALSE on SPU
  bool_t remote_in_buf_ppu;
  // Address of remote input buffer
  union {
    BUFFER_CB *ppu_in_buf;
    void *remote_spu_in_buf_data;
    void *remote_in_buf_ptr;
  };
  // Size of remote input buffer (only needed if buffer is on SPU)
  uint32_t remote_spu_in_buf_size;

  // D-address of input buffer on local SPU
  SPU_ADDRESS local_in_buf_data;
  // D-address of filter control block on local SPU
  SPU_ADDRESS filt;
  SPU_ADDRESS local_out_buf_data;

  bool_t remote_out_buf_ppu;
  union {
    BUFFER_CB *ppu_out_buf;
    void *remote_spu_out_buf_data;
    void *remote_out_buf_ptr;
  };
  uint32_t remote_spu_out_buf_size;
} EXT_SPU_LAYOUT;

// Rate info for an "iteration" of an operation.
typedef struct _EXT_SPU_RATES {
  uint32_t in_bytes;    // Number of bytes consumed
  uint32_t run_iters;   // Number of iterations work function is run
  uint32_t loop_iters;
  uint32_t out_bytes;   // Number of bytes produced
} EXT_SPU_RATES;

// Starts a generic SPU input-run-output operation.
//
// Data from input buffer is processed on SPU and written to output buffer.
// Remote buffers are optional and can be on PPU or another SPU.
//
// <r> specifies the unit of double-buffering.
void *ext_spu(EXT_SPU_LAYOUT *l, EXT_SPU_RATES *r, uint32_t iters,
              GENERIC_COMPLETE_CB *cb, uint32_t tag);
// Notifies operation that additional data/space is available in the PPU
// input/output buffer.
void ext_spu_notify_input(void *d);
void ext_spu_notify_output(void *d);

#define EXT_PSP_MAX_TAPES 15

typedef struct _EXT_PSP_LAYOUT {
  uint32_t spu_id;
  SPU_FILTER_DESC *desc;
  SPU_ADDRESS filt_cb;
  SPU_ADDRESS in_buf_start;
  SPU_ADDRESS out_buf_start;
  SPU_ADDRESS cmd_data_start;  // Needs 128 bytes + 128 per tape
  uint32_t cmd_id_start;       // Needs 2 + 2 per tape
  bool_t load_filter;
} EXT_PSP_LAYOUT;

typedef struct _EXT_PSP_INPUT_TAPE {
  uint32_t pop_bytes;
  uint32_t peek_extra_bytes;
  uint32_t spu_buf_size;
} EXT_PSP_INPUT_TAPE;

typedef struct _EXT_PSP_OUTPUT_TAPE {
  uint32_t push_bytes;
  uint32_t spu_buf_size;
} EXT_PSP_OUTPUT_TAPE;

typedef struct _EXT_PSP_PARAMS {
  EXT_PSP_INPUT_TAPE inputs[EXT_PSP_MAX_TAPES - 1];
  EXT_PSP_OUTPUT_TAPE outputs[EXT_PSP_MAX_TAPES - 1];
  uint32_t num_inputs;
  uint32_t num_outputs;
  bool_t data_parallel;
  uint32_t group_iters;
  uint32_t loop_iters;
} EXT_PSP_PARAMS;

void ext_ppu_spu_ppu(EXT_PSP_LAYOUT *l, EXT_PSP_PARAMS *f,
                     BUFFER_CB **ppu_in_buf, BUFFER_CB **ppu_out_buf,
                     uint32_t iters, GENERIC_COMPLETE_CB *cb, uint32_t tag);

// For compatibility.
typedef EXT_PSP_LAYOUT EXT_PSP_EX_LAYOUT;
typedef EXT_PSP_PARAMS EXT_PSP_EX_PARAMS;

void ext_ppu_spu_ppu_ex(EXT_PSP_EX_LAYOUT *l, EXT_PSP_EX_PARAMS *f,
                        BUFFER_CB **ppu_in_buf, BUFFER_CB **ppu_out_buf,
                        uint32_t iters, GENERIC_COMPLETE_CB *cb, uint32_t tag);

#endif
