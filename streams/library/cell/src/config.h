/*-----------------------------------------------------------------------------
 * config.h
 *
 * Configurable parameters. Automatically included.
 *---------------------------------------------------------------------------*/

#ifndef _SPULIB_CONFIG_H_
#define _SPULIB_CONFIG_H_

// Set to 1 to turn on debugging of library implementation.
#define DEBUG         1
// Set to 1 to turn on consistency checks on commands sent by scheduler.
#define CHECK         1
// Set to 1 to turn on even more checks on commands sent by scheduler (only
// takes effect if CHECK is 1).
#define CHECK_PARAMS  1

// If 0, data transfers must be aligned on qword boundaries.
#define DT_ALLOW_UNALIGNED      1

#ifdef __SPU__  // SPU

// If 1, dt_in_* commands on SPU will automatically adjust head/tail pointers
// of empty buffers to match the data offset in the source buffer, if
// necessary.
#define DT_AUTO_ADJUST_POINTERS 0

// If 1, dma_reserve_tag returns INVALID_TAG if all tags are already reserved.
// Otherwise, caller must ensure a tag is available.
#define DMA_ALLOW_RESERVE_FAIL 0

#else           // PPU

// Number of SPUs to initialize. Must be <= number of free physical SPUs.
#define NUM_SPU 6

// If 1, filters in extended SPU executions may consume no input/produce no
// output.
#define EXT_ALLOW_SPU_NO_INPUT  1
#define EXT_ALLOW_SPU_NO_OUTPUT 1

#endif

#endif
