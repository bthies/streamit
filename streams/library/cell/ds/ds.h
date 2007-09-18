#ifndef _DS_H_
#define _DS_H_

#include "dsconfig.h"
#include <stdlib.h>
#include <stdio.h>
#include "spulib.h"

#define MAX_TAPES 15

typedef struct _FILTER FILTER;
typedef struct _FILTER_INPUT_TAPE FILTER_INPUT_TAPE;
typedef struct _FILTER_OUTPUT_TAPE FILTER_OUTPUT_TAPE;
typedef struct _CHANNEL CHANNEL;
typedef struct _DP_INSTANCE DP_INSTANCE;

typedef void FILTER_WORK_FUNC(void *state, CHANNEL *const *inputs,
                              CHANNEL *const *outputs);

struct _FILTER {
  // from stream graph
  CHANNEL *inputs; // TODO: make these pointers
  CHANNEL *outputs;
  char *name;
  SPU_FILTER_DESC desc;
  bool_t data_parallel;
  // FILTER_WORK_FUNC *ppu_prework_func;
  // FILTER_WORK_FUNC *ppu_work_func;
  // computed
  bool_t exclusive;
  uint32_t group_iters;
  uint32_t loop_iters;
  uint32_t output_buffered_iters;
  // internal
  bool_t visited;
  // bool_t done_prework;
  uint32_t incomplete_inputs;
  uint32_t active_count;
  uint32_t avail_iters;
  uint32_t input_avail_iters;
  uint32_t output_avail_iters;
  DP_INSTANCE *instances;
  DP_INSTANCE *last_instance;
};

struct _FILTER_INPUT_TAPE {
  // from stream graph
  FILTER *f;
  uint32_t pop_bytes;
  uint32_t peek_extra_bytes;
  // computed
  uint32_t spu_buf_size;
};

struct _FILTER_OUTPUT_TAPE {
  // from stream graph
  FILTER *f;
  uint32_t push_bytes;
  // computed
  uint32_t spu_buf_size;
};

struct _CHANNEL {
  // from stream graph
  FILTER_OUTPUT_TAPE output;
  FILTER_INPUT_TAPE input;
  uint32_t buf_size;
  bool_t non_circular;
  CHANNEL *duplicates;
  uint32_t num_duplicates;
  // internal
  uint32_t used_bytes;
  uint32_t free_bytes;
  CHANNEL *source;
  CHANNEL *parent;
  uint32_t free_waiting;
  BUFFER_CB buf;
};

// Program defines and initializes
extern FILTER filters[];
extern CHANNEL channels[];

// Program initializes
extern uint32_t num_filters;
extern uint32_t num_channels;
extern uint32_t num_spu;

struct _DP_INSTANCE {
  uint32_t input_iters;
  uint32_t output_iters;
  bool_t done;
  DP_INSTANCE *next;
  DP_INSTANCE *prev;
};

typedef struct _ACTIVE_INPUT_TAPE {
  SPU_ADDRESS spu_buf_data;
  BUFFER_CB *channel_buf;
  BUFFER_CB channel_buf_instance;
} ACTIVE_INPUT_TAPE;

typedef struct _ACTIVE_OUTPUT_TAPE {
  SPU_ADDRESS spu_buf_data;
  BUFFER_CB *channel_buf;
  BUFFER_CB channel_buf_instance;
} ACTIVE_OUTPUT_TAPE;

typedef struct _ACTIVE_FILTER {
  uint32_t spu_id;
  FILTER *f;
  ACTIVE_INPUT_TAPE inputs[MAX_TAPES - 1];
  ACTIVE_OUTPUT_TAPE outputs[MAX_TAPES];
  SPU_ADDRESS filt_cb;
  SPU_ADDRESS cmd_data_start;
  uint32_t cmd_id_start;
  uint32_t cmd_group_start;
  uint32_t group_num_cmd;
  uint32_t group_data_size; // (64 + 64 per tape) * 2 = 1024 for 7 tapes
  uint32_t groups_left;
  uint32_t last_group_iters;
  uint32_t input_groups_left;
  uint32_t output_groups_left;
  bool_t done_unaligned_output;
  uint32_t done_input_groups;
  uint32_t done_output_groups;
  uint32_t output_buffered_iters;
  bool_t updated;
  uint32_t current_group;
  uint32_t cmd_mask;
  uint32_t phase;
  uint32_t waiting_mask;
  DP_INSTANCE *instance;
} ACTIVE_FILTER;

#define DS_SPU_BUFFER_SLOT_SIZE   33792 // (128 + 4 KB) * 8
#define DS_SPU_FILTER_SLOT_SIZE   16384
#define DS_SPU_CMD_DATA_SLOT_SIZE  1024

typedef struct _SPU_STATE {
  uint32_t spu_id;
  ACTIVE_FILTER filters[2];
  ACTIVE_FILTER *current;
  ACTIVE_FILTER *next;
  uint32_t next_filt;
  SPU_ADDRESS buffer_start;
  SPU_ADDRESS filter_start;
  SPU_ADDRESS cmd_data_start;
  uint32_t next_buf_slot;
  uint32_t next_filt_slot;
  uint32_t next_cmd_data_slot;
  uint32_t next_cmd_id_slot;
  uint32_t next_cmd_group_slot;
  bool_t last_run_issued;
  uint32_t last_run_id;
} SPU_STATE;

typedef struct _FILTER_METRIC {
  FILTER *filter;
  uint32_t metric;
} FILTER_METRIC;

static INLINE int32_t
min_int32(int32_t a, int32_t b)
{
  return (a <= b ? a : b);
}

static INLINE int32_t
max_int32(int32_t a, int32_t b)
{
  return (a >= b ? a : b);
}

static INLINE uint32_t
min_uint32(uint32_t a, uint32_t b)
{
  return (a <= b ? a : b);
}

static INLINE uint32_t
max_uint32(uint32_t a, uint32_t b)
{
  return (a >= b ? a : b);
}

static INLINE uint32_t
next_power_2(uint32_t a)
{
  return 0x80000000 >> (count_ms_zeros(a - 1) - 1);
}

#if CHECK
#define safe_dec(x, a) \
  do {                                                                        \
    typeof(x) *_x = &(x);                                                     \
    typeof(x) _a = (a);                                                       \
    check(*_x >= _a);                                                         \
    *_x -= _a;                                                                \
  } while (FALSE)
#else
#define safe_dec(x, a) ((x) -= (a))
#endif

#define errprintf(...) fprintf(stderr, __VA_ARGS__)

#if CHECK
void check_equal(uint32_t value, uint32_t expected, char *var_format, ...);
#else
#define check_equal(value, expected, var_format, ...) ((void)0)
#endif

extern uint32_t incomplete_filters;
extern uint32_t num_free_spu;

extern SPU_STATE spu_state[NUM_SPU];

extern void *channel_data;

extern DP_INSTANCE dp_instance_data[NUM_SPU * 4];
extern DP_INSTANCE *next_dp_instance;

void ds_init();
void init_update_down_channel_used(CHANNEL *c, uint32_t num_bytes);
void init_update_up_channel_free(CHANNEL *c, uint32_t num_bytes);

void ds_init_2(); // after prework has been done, only called by ds_run
void ds_run();

void schedule_filter(SPU_STATE *spu, FILTER *f, uint32_t iters);

void update_filter_done(FILTER *f);
void update_down_filter_incomplete(CHANNEL *c);
void update_down_channel_used(CHANNEL *c, uint32_t num_bytes);
void update_up_channel_free(CHANNEL *c, uint32_t num_bytes);
void update_filter_input(FILTER *f);
void update_filter_output(FILTER *f);
void update_all_filters();

uint32_t get_schedule_iters(FILTER *f, bool_t current);
uint32_t get_filter_metric(FILTER *f, bool_t current);
FILTER_METRIC find_best_filter(FILTER *reserved);
void schedule_free_spu(SPU_STATE *spu, FILTER *reserved);
void schedule_all_free_spu(FILTER *reserved);
uint32_t schedule_spu_next(SPU_STATE *spu, FILTER *current,
                           DP_INSTANCE *instance);

static INLINE DP_INSTANCE *
new_dp_instance()
{
  DP_INSTANCE *instance;
  check(next_dp_instance != NULL);
  instance = next_dp_instance;
  next_dp_instance = next_dp_instance->next;
  return instance;
}

static INLINE void
free_dp_instance(DP_INSTANCE *instance)
{
  instance->next = next_dp_instance;
  next_dp_instance = instance;
}

static INLINE SPU_ADDRESS
get_spu_buffer_slot(SPU_STATE *spu)
{
  SPU_ADDRESS da =
    spu->buffer_start + spu->next_buf_slot * DS_SPU_BUFFER_SLOT_SIZE;
  spu->next_buf_slot = (spu->next_buf_slot == 2 ? 0 : spu->next_buf_slot + 1);
  return da;
}

static INLINE SPU_ADDRESS
get_spu_filter_slot(SPU_STATE *spu)
{
  SPU_ADDRESS da =
    spu->filter_start + spu->next_filt_slot * DS_SPU_FILTER_SLOT_SIZE;
  spu->next_filt_slot ^= 1;
  return da;
}

static INLINE SPU_ADDRESS
get_spu_cmd_data_slot(SPU_STATE *spu)
{
  SPU_ADDRESS da = spu->cmd_data_start +
    spu->next_cmd_data_slot * DS_SPU_CMD_DATA_SLOT_SIZE;
  spu->next_cmd_data_slot ^= 1;
  return da;
}

static INLINE uint32_t
get_spu_cmd_id_slot(SPU_STATE *spu)
{
  uint32_t cmd_id_start = 0 + spu->next_cmd_id_slot * 16;
  spu->next_cmd_id_slot ^= 1;
  return cmd_id_start;
}

static INLINE uint32_t
get_spu_cmd_group_slot(SPU_STATE *spu)
{
  uint32_t gid_start = 28 + spu->next_cmd_group_slot * 2;
  spu->next_cmd_group_slot ^= 1;
  return gid_start;
}

#endif
