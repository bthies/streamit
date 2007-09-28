/*-----------------------------------------------------------------------------
 * filter.h
 *
 * Filter structure definitions (SPU).
 *---------------------------------------------------------------------------*/

#ifndef _FILTER_H_
#define _FILTER_H_

#undef C_FILE
#define C_FILE filter_h

#include "spucommand.h"
#include "buffer.h"

// 16-byte aligned and padded
typedef struct _FILTER_CB {
  FILTER_DESC desc;
// 20
  void *state;
  BUFFER_CB **inputs;
  BUFFER_CB **outputs;
// 32
#if CHECK
  bool_t busy;
  uint8_t attached_inputs;
  uint8_t attached_outputs;
  uint8_t _c_padding[13];
#endif
// 32/48
  uint8_t data[];
  /*
   * Following is:
   * - Array of pointers to input buffers (void *).
   * - Array of pointers to output buffers (void *).
   * - Padding to qword boundary.
   * - Filter state.
   */
} FILTER_CB;

C_ASSERT(sizeof(FILTER_CB) == SPU_FILTER_CB_SIZE);

typedef void FILTER_WORK_FUNC(void *params, void *state,
                              BUFFER_CB *const *inputs,
                              BUFFER_CB *const *outputs, uint32_t iters);

#endif
