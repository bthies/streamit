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

  // Address of remote input buffer data
  union {
    void *remote_in_buf_data;
    void *ppu_in_buf_data;
  };
  // Size of remote input buffer (only needed if buffer is on SPU)
  uint32_t remote_in_buf_size;
  // TRUE indicates remote input buffer is on PPU, FALSE on SPU
  bool_t remote_in_buf_ppu;
  // D-address of input buffer on local SPU
  union {
    SPU_ADDRESS local_in_buf_data;
    SPU_ADDRESS spu_in_buf_data;
  };
  // D-address of filter control block on local SPU
  SPU_ADDRESS filt;
  union {
    SPU_ADDRESS local_out_buf_data;
    SPU_ADDRESS spu_out_buf_data;
  };
  union {
    void *remote_out_buf_data;
    void *ppu_out_buf_data;
  };
  uint32_t remote_out_buf_size;
  bool_t remote_out_buf_ppu;
} EXT_SPU_LAYOUT;

// Rate info for an "iteration" of an operation.
typedef struct _EXT_SPU_RATES {
  uint32_t in_bytes;    // Number of bytes consumed
  uint32_t run_iters;   // Number of iterations work function is run
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

typedef EXT_SPU_LAYOUT EXT_PSP_LAYOUT;
typedef EXT_SPU_RATES EXT_PSP_RATES;

// Starts a ppu_spu_ppu operation.
//
// Wrapper for ext_spu when remote buffers are always on PPU.
//
// The <remote_[in/out]_buf_[size/ppu]> fields in <l> are ignored and
// overwritten.
void *ext_ppu_spu_ppu(EXT_SPU_LAYOUT *l, EXT_SPU_RATES *r, uint32_t iters,
                      GENERIC_COMPLETE_CB *cb, uint32_t tag);
#define ext_psp_notify_input  ext_spu_notify_input
#define ext_psp_notify_output ext_spu_notify_output

// Starts a data_parallel operation.
//
// Each SPU uses separate PPU input/output buffers. Essentially no alignment
// requirements.
//
// <l> specifies an array of EXT_SPU_LAYOUT structures, one for each SPU. The
// <remote_[in/out]_buf_[size/ppu]> fields are ignored and overwritten.
// 
// <iters> specifies the number of iterations executed by *each* SPU.
void *ext_data_parallel(uint32_t num_spu, EXT_SPU_LAYOUT *l, EXT_SPU_RATES *r,
                        uint32_t iters, GENERIC_COMPLETE_CB *cb, uint32_t tag);
void ext_dp_notify_input(void *op, uint32_t n);
void ext_dp_notify_output(void *op, uint32_t n);

// Starts a data_parallel_shared operation.
//
// The same PPU input/output buffers are shared by all SPUs. Major alignment
// requirements.
//
// <l> specifies an array of EXT_SPU_LAYOUT structures, one for each SPU. The
// <remote_[in/out]_buf_[data/size/ppu]> fields are ignored and overwritten.
//
// If <num_spu> is 0, uses all SPUs and the first entry of <l> is used as the
// layout for all SPUs. In this case the <spu_id> field is ignored, and
// overwritten.
//
// <iters> specifies the *total* number of iterations - these are distributed
// (currently dumbly) across SPUs.
void *ext_data_parallel_shared(uint32_t num_spu, EXT_SPU_LAYOUT *l,
                               void *remote_in_buf_data,
                               void *remote_out_buf_data,
                               EXT_SPU_RATES *r, uint32_t iters,
                               GENERIC_COMPLETE_CB *cb, uint32_t tag);
void ext_dps_notify_input(void *op);
void ext_dps_notify_output(void *op);

#endif
