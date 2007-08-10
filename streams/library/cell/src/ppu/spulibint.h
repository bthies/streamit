/*-----------------------------------------------------------------------------
 * spulibint.h
 *
 * Internal declarations for PPU side of library.
 *---------------------------------------------------------------------------*/

#ifndef _SPULIB_INT_H_
#define _SPULIB_INT_H_

#define SPULIB_INTERNAL
#include "spulib.h"
#include <stdlib.h>

// PPU data transfer command types.
#define PPU_CMD_DT_IN_FRONT   0
#define PPU_CMD_DT_IN_BACK    1
#define PPU_CMD_DT_OUT_FRONT  2
#define PPU_CMD_DT_OUT_BACK   3

PPU_DT_PARAMS *ppu_dt_wait_spu(uint32_t spu_id, uint32_t spu_cmd_id,
                               bool_t run_cb, uint32_t tag);
void ppu_finish_dt(PPU_DT_PARAMS *cmd);

/*-----------------------------------------------------------------------------
 * spu_clear_group
 *
 * Clears the specified command group.
 *---------------------------------------------------------------------------*/

static INLINE void
spu_clear_group(SPU_CMD_GROUP *g)
{
  g->size = 0;
  g->end = &spu_info[g->spu_id].cmd_group_data[g->gid];
}

SPU_CMD_GROUP *spu_new_int_group(SPU_INFO *spu);
void spu_free_int_group(SPU_CMD_GROUP *g);

/*-----------------------------------------------------------------------------
 * spu_issue_int_group
 *---------------------------------------------------------------------------*/

static INLINE void
spu_issue_int_group(SPU_CMD_GROUP *g, SPU_ADDRESS da)
{
  spu_issue_group(g->spu_id, g->gid, da);
}

// Handler called when commands from library-handled operations complete.
typedef bool_t EXTENDED_OP_HANDLER(void *data, uint32_t mask);

// Bookkeeping info for library-handled operations.
struct _EXTENDED_OP {
  uint32_t spu_cmd_mask;          // Bitmap of SPU command IDs operation uses
  // Handler called when commands from operation complete.
  EXTENDED_OP_HANDLER *handler;
  GENERIC_COMPLETE_CB *cb;        // Scheduler callback to run when done
  uint32_t tag;                   // Callback tag
  struct _EXTENDED_OP *next;      // Next operation in list
  // Operation-specific data (dynamically allocated with structure).
  uint8_t data[];
};

void *spu_new_ext_op(SPU_INFO *spu, uint32_t spu_cmd_mask,
                     EXTENDED_OP_HANDLER *handler, GENERIC_COMPLETE_CB *cb,
                     uint32_t tag, uint32_t data_size);

#include "extspuint.h"

#endif
