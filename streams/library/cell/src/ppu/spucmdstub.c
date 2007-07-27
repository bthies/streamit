/*-----------------------------------------------------------------------------
 * spucmdstub.c
 *
 * Stubs for initializing SPU command structures.
 *---------------------------------------------------------------------------*/

#include "spulibint.h"
#include <stdarg.h>

/*-----------------------------------------------------------------------------
 * Helper macros.
 *---------------------------------------------------------------------------*/

#define DECLARE_SPU_COMMAND(name, type, ...) \
  SPU_CMD_HEADER *                                                            \
  spu_##name(SPU_CMD_GROUP *g, ##__VA_ARGS__, uint32_t cmd_id,                \
             uint32_t num_deps, ...)                                          \
  {                                                                           \
    SPU_##type##_CMD *cmd = (SPU_##type##_CMD *)spu_new_command(g);           \
    {                                                                         \
      va_list deps;                                                           \
      va_start(deps, num_deps);                                               \
      spu_init_header(&cmd->header, SPU_CMD_##type, cmd_id, num_deps, deps);  \
      va_end(deps);                                                           \
    }

#define END_SPU_COMMAND \
    spu_done_command(g, &cmd->header);                                        \
    return &cmd->header;                                                      \
  }

/*-----------------------------------------------------------------------------
 * spu_init_header
 *---------------------------------------------------------------------------*/
static INLINE void
spu_init_header(SPU_CMD_HEADER *cmd, uint32_t type, uint32_t id,
                uint32_t num_deps, va_list deps)
{
  pcheck((id < SPU_MAX_COMMANDS) && (num_deps <= SPU_CMD_MAX_DEPS));

  cmd->type = type;
  cmd->id = id;
  cmd->num_back_deps = num_deps;
  cmd->num_forward_deps = 0;

  for (uint32_t i = 0; i < num_deps; i++) {
    uint32_t dep_id = va_arg(deps, uint32_t);
    // Duplicate dependencies are not checked for - the SPU side works fine
    // with them.
    pcheck(dep_id < SPU_MAX_COMMANDS);
    cmd->deps[i] = dep_id;
  }
}

/*-----------------------------------------------------------------------------
 * spu_new_command
 *---------------------------------------------------------------------------*/
static INLINE SPU_CMD_HEADER *
spu_new_command(SPU_CMD_GROUP *g)
{
  return (SPU_CMD_HEADER *)g->end;
}

/*-----------------------------------------------------------------------------
 * spu_done_command
 *---------------------------------------------------------------------------*/
static INLINE void
spu_done_command(SPU_CMD_GROUP *g, SPU_CMD_HEADER *cmd)
{
  uint32_t cmd_size = spu_cmd_get_size(cmd);
  g->size += cmd_size;
  pcheck(g->size <= SPU_CMD_GROUP_MAX_SIZE);
  g->end += cmd_size;
}

/*-----------------------------------------------------------------------------
 * Stub implementations.
 *---------------------------------------------------------------------------*/

/*-----------------------------------------------------------------------------
 * spu_load_data
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(load_data, LOAD_DATA,
                    SPU_ADDRESS dest_da, void *src_addr, uint32_t num_bytes)
{
  cmd->dest_lsa = spu_lsa(g->spu_id, dest_da);
  cmd->src_addr = src_addr;
  cmd->num_bytes = num_bytes;

  cmd->state = 0;
}
END_SPU_COMMAND

/*-----------------------------------------------------------------------------
 * spu_call_func
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(call_func, CALL_FUNC,
                    LS_ADDRESS func)
{
  cmd->func = func;
}
END_SPU_COMMAND

/*-----------------------------------------------------------------------------
 * spu_filter_load
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(filter_load, FILTER_LOAD,
                    SPU_ADDRESS filt, SPU_FILTER_DESC *desc)
{
  cmd->filt = spu_lsa(g->spu_id, filt);
  cmd->desc = *(SPU_INT_FILTER_DESC *)desc;

  cmd->state = 0;
}
END_SPU_COMMAND

/*-----------------------------------------------------------------------------
 * spu_filter_unload
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(filter_unload, FILTER_UNLOAD,
                    SPU_ADDRESS filt)
{
  cmd->filt = spu_lsa(g->spu_id, filt);

  cmd->state = 0;
}
END_SPU_COMMAND

/*-----------------------------------------------------------------------------
 * spu_filter_attach_input
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(filter_attach_input, FILTER_ATTACH_INPUT,
                    SPU_ADDRESS filt, uint32_t tape_id, SPU_ADDRESS buf_data)
{
  cmd->filt = spu_lsa(g->spu_id, filt);
  cmd->tape_id = tape_id;
  cmd->buf_data = spu_lsa(g->spu_id, buf_data);
}
END_SPU_COMMAND

/*-----------------------------------------------------------------------------
 * spu_filter_attach_output
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(filter_attach_output, FILTER_ATTACH_OUTPUT,
                    SPU_ADDRESS filt, uint32_t tape_id, SPU_ADDRESS buf_data)
{
  cmd->filt = spu_lsa(g->spu_id, filt);
  cmd->tape_id = tape_id;
  cmd->buf_data = spu_lsa(g->spu_id, buf_data);
}
END_SPU_COMMAND

/*-----------------------------------------------------------------------------
 * spu_filter_run
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(filter_run, FILTER_RUN,
                    SPU_ADDRESS filt, uint32_t iters)
{
  cmd->filt = spu_lsa(g->spu_id, filt);
  cmd->iters = iters;

  IF_CHECK(cmd->state = 0);
}
END_SPU_COMMAND

/*-----------------------------------------------------------------------------
 * spu_buffer_alloc
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(buffer_alloc, BUFFER_ALLOC,
                    SPU_ADDRESS buf_data, uint32_t size, uint32_t data_offset)
{
  cmd->buf_data = spu_lsa(g->spu_id, buf_data);
  cmd->mask = size - 1;
  cmd->data_offset = data_offset;
}
END_SPU_COMMAND

/*-----------------------------------------------------------------------------
 * spu_buffer_align
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(buffer_align, BUFFER_ALIGN,
                    SPU_ADDRESS buf_data, uint32_t data_offset)
{
  cmd->buf_data = spu_lsa(g->spu_id, buf_data);
  cmd->data_offset = data_offset;
}
END_SPU_COMMAND

/*-----------------------------------------------------------------------------
 * spu_dt_in_back
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(dt_in_back, DT_IN_BACK,
                    SPU_ADDRESS buf_data, void *src_buf_data,
                    uint32_t src_buf_size, uint32_t num_bytes)
{
  cmd->buf_data = spu_lsa(g->spu_id, buf_data);
  cmd->src_buf_data = src_buf_data;
  cmd->src_buf_mask = src_buf_size - 1;
  cmd->num_bytes = num_bytes;

  cmd->state = 0;
}
END_SPU_COMMAND

/*-----------------------------------------------------------------------------
 * spu_dt_out_front
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(dt_out_front, DT_OUT_FRONT,
                    SPU_ADDRESS buf_data, void *dest_buf_data,
                    uint32_t num_bytes)
{
  cmd->buf_data = spu_lsa(g->spu_id, buf_data);
  cmd->dest_buf_data = dest_buf_data;
  cmd->num_bytes = num_bytes;

  cmd->state = 0;
}
END_SPU_COMMAND

/*-----------------------------------------------------------------------------
 * spu_dt_out_front_spu
 *
 * Wrapper with same signature as spu_dt_out_front_ppu.
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(dt_out_front_spu, DT_OUT_FRONT,
                    SPU_ADDRESS buf_data, void *dest_buf_data,
                    uint32_t dest_buf_size, uint32_t num_bytes)
{
  UNUSED_PARAM(dest_buf_size);

  cmd->buf_data = spu_lsa(g->spu_id, buf_data);
  cmd->dest_buf_data = dest_buf_data;
  cmd->num_bytes = num_bytes;

  cmd->state = 0;
}
END_SPU_COMMAND

/*-----------------------------------------------------------------------------
 * spu_dt_out_front_ppu
 *---------------------------------------------------------------------------*/
DECLARE_SPU_COMMAND(dt_out_front_ppu, DT_OUT_FRONT_PPU,
                    SPU_ADDRESS buf_data, void *dest_buf_data,
                    uint32_t dest_buf_size, uint32_t num_bytes)
{
  cmd->buf_data = spu_lsa(g->spu_id, buf_data);
  cmd->dest_buf_data = dest_buf_data;
  cmd->dest_buf_mask = dest_buf_size - 1;
  cmd->num_bytes = num_bytes;

  cmd->state = 0;
}
END_SPU_COMMAND
