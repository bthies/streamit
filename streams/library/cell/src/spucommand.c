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
  // Filter commands.
  define_command(LOAD_DATA),
  define_command(FILTER_LOAD),
  define_command(FILTER_UNLOAD),
  define_command(FILTER_ATTACH_INPUT),
  define_command(FILTER_ATTACH_OUTPUT),
  define_command(FILTER_RUN),
  // Buffer commands.
  define_command(BUFFER_ALLOC),
  define_command(BUFFER_ALIGN),
  // Data transfer commands.
  define_command(DT_OUT_FRONT),
  define_command(DT_OUT_FRONT_PPU),
  define_command(DT_IN_BACK),
  define_command(DT_OUT_BACK),
  define_command(DT_OUT_BACK_PPU),
  define_command(DT_IN_FRONT),
  // Misc commands.
  define_command(NULL),
  define_command(CALL_FUNC),
  // Stats commands.
  define_command(STATS_PRINT),
  define_command(STATS_UPDATE)
};
