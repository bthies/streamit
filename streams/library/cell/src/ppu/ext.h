/*-----------------------------------------------------------------------------
 * ext.h
 *
 * Public interface for library-handled operations. Automatically included by
 * spulib.h.
 *---------------------------------------------------------------------------*/

#ifndef _SPULIB_EXT_H_
#define _SPULIB_EXT_H_

// Resource configuration for ppu_spu_ppu operation.
typedef struct _EXT_PSP_LAYOUT {
  uint32_t spu_id;                // ID of SPU

  // Operation requires following resources on SPU, which must be available for
  // duration:
  // - 6 contiguous command IDs starting from <cmd_id>. Scheduler cannot issue
  //   commands with these IDs while operation is in progress.
  // - 512 bytes starting at data address <da> (must be 128-byte aligned).
  uint32_t cmd_id;
  SPU_ADDRESS da;

  void *ppu_in_buf_data;          // Address of input buffer data on PPU
  SPU_ADDRESS spu_in_buf_data;    // D-address of input buffer data on SPU
  SPU_ADDRESS filt;               // D-address of filter control block on SPU
  SPU_ADDRESS spu_out_buf_data;   // D-address of output buffer data on SPU
  void *ppu_out_buf_data;         // Address of output buffer data on PPU
} EXT_PSP_LAYOUT;

// Rate info for an "iteration" of a ppu_spu_ppu operation.
typedef struct _EXT_PSP_RATES {
  uint32_t in_bytes;    // Number of bytes consumed
  uint32_t run_iters;   // Number of iterations work function is run
  uint32_t out_bytes;   // Number of bytes produced
} EXT_PSP_RATES;

// Starts a ppu_spu_ppu operation.
//
// Data from PPU input buffer is processed on SPU and written to PPU output
// buffer.
//
// <r> specifies the unit of double-buffering.
void *ext_ppu_spu_ppu(EXT_PSP_LAYOUT *l, EXT_PSP_RATES *r, uint32_t iters,
                      GENERIC_COMPLETE_CB *cb, uint32_t tag);
// Notifies a ppu_spu_ppu operation that additional data/space is available in
// the PPU input/output buffer.
void ext_psp_notify_input(void *d);
void ext_psp_notify_output(void *d);

// Starts a data_parallel operation.
//
// Each SPU uses separate PPU input/output buffers. Essentially no alignment
// requirements.
//
// <l> specifies an array of EXT_PSP_LAYOUT structures, one for each SPU.
// <iters> specifies the number of iterations executed by *each* SPU.
void *ext_data_parallel(uint32_t num_spu, EXT_PSP_LAYOUT *l, EXT_PSP_RATES *r,
                        uint32_t iters, GENERIC_COMPLETE_CB *cb, uint32_t tag);
// Notifies a data_parallel operation that additional data/space is available
// in the PPU input/output buffer for the SPU at the specified index.
void ext_dp_notify_input(void *op, uint32_t n);
void ext_dp_notify_output(void *op, uint32_t n);

// Starts a data_parallel_shared operation.
//
// The same PPU input/output buffers are shared by all SPUs. Major alignment
// requirements.
//
// <l> specifies an array of EXT_PSP_LAYOUT structures, one for each SPU. The
// <ppu_in_buf_data>/<ppu_out_buf_data> fields are ignored, and are
// overwritten.
//
// If <num_spu> is 0, uses all SPUs and the first entry of <l> is used as the
// layout for all SPUs. In this case the <spu_id> field is ignored, and
// overwritten.
//
// <iters> specifies the *total* number of iterations - these are distributed
// (currently dumbly) across SPUs.
void *ext_data_parallel_shared(uint32_t num_spu, EXT_PSP_LAYOUT *l,
                               void *ppu_in_buf_data, void *ppu_out_buf_data,
                               EXT_PSP_RATES *r, uint32_t iters,
                               GENERIC_COMPLETE_CB *cb, uint32_t tag);
// Notifies a data_parallel_shared operation that additional data/space is
// available in the PPU input/output buffer.
void ext_dps_notify_input(void *op);
void ext_dps_notify_output(void *op);

#endif
