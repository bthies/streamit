/*-----------------------------------------------------------------------------
 * spucommand.c
 *
 * SPU command data.
 *---------------------------------------------------------------------------*/

#include "defs.h"
#include "spucommand.h"

// Macro to fill in entries of spu_cmd_size array.
#define define_command(cmd) [SPU_CMD_##cmd] = sizeof(SPU_##cmd##_CMD)

#ifdef __SPU__  // SPU
uint16_t
#else           // PPU
uint32_t
#endif
spu_cmd_size[SPU_NUM_CMD_TYPES] = {
  define_command(NULL),
  define_command(LOAD_DATA),
  define_command(CALL_FUNC),
  define_command(FILTER_LOAD),
  define_command(FILTER_UNLOAD),
  define_command(FILTER_ATTACH_INPUT),
  define_command(FILTER_ATTACH_OUTPUT),
  define_command(FILTER_RUN),
  define_command(BUFFER_ALLOC),
  define_command(BUFFER_ALIGN),
  define_command(DT_IN_FRONT),
  define_command(DT_IN_BACK),
  define_command(DT_OUT_FRONT),
  define_command(DT_OUT_BACK),
  define_command(DT_OUT_FRONT_PPU),
  define_command(DT_OUT_BACK_PPU),
  define_command(STATS_PRINT)
};
