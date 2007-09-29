#include "defs.h"
#include "stats.h"

#if STATS_ENABLE
#include <stdio.h>
#include "depend.h"

bool_t stats_started = FALSE;
uint32_t stats_first_start;
static uint32_t stats_total_ticks = 0; // Not necessary

uint8_t stats_loaded_count = 0;
uint32_t stats_loaded_start;
static uint32_t stats_last_loaded_ticks = 0; // Not necessary
static uint32_t stats_total_loaded_ticks = 0;

uint8_t stats_running_count = 0;
uint32_t stats_running_start;
uint32_t stats_running_ticks = 0;
static uint32_t stats_last_running_ticks = 0; // Not necessary
static uint32_t stats_total_running_ticks = 0;

uint32_t stats_work_ticks = 0;
static uint32_t stats_last_work_ticks = 0; // Not necessary
static uint32_t stats_total_work_ticks = 0;

static void stats_print(bool_t total_only);

static INLINE uint32_t
stats_percentage(float a, float total)
{
  return a * float_re(total) * 100.f;
}

static void
stats_print(bool_t total_only)
{
  float total_time = stats_total_ticks * TICK_PERIOD;
  float total_loaded_time = stats_total_loaded_ticks * TICK_PERIOD;
  float total_running_time = stats_total_running_ticks * TICK_PERIOD;
  float total_work_time = stats_total_work_ticks * TICK_PERIOD;
  uint32_t total_loaded_percent =
    stats_percentage(total_loaded_time, total_time);
  uint32_t total_running_percent =
    stats_percentage(total_running_time, total_time);
  uint32_t total_work_percent = stats_percentage(total_work_time, total_time);

  if (total_only) {
    printf("SPU %d: T:%8d L:%8d(%3d) R:%8d(%3d) W:%8d(%3d)\n",
           dep_params.id,
           (uint32_t)total_time,
           (uint32_t)total_loaded_time, total_loaded_percent,
           (uint32_t)total_running_time, total_running_percent,
           (uint32_t)total_work_time, total_work_percent);
  } else {
    float last_loaded_time = stats_last_loaded_ticks * TICK_PERIOD;
    float last_running_time = stats_last_running_ticks * TICK_PERIOD;
    float last_work_time = stats_last_work_ticks * TICK_PERIOD;

    printf("SPU %d:            L:%8d      R:%8d(%3d) W:%8d(%3d)\n"
           "       T:%8d   %8d(%3d)   %8d(%3d)   %8d(%3d)\n",
           dep_params.id,
           (uint32_t)last_loaded_time,
           (uint32_t)last_running_time, stats_percentage(last_running_time,
                                                         last_loaded_time),
           (uint32_t)last_work_time, stats_percentage(last_work_time,
                                                      last_loaded_time),
           (uint32_t)total_time,
           (uint32_t)total_loaded_time, total_loaded_percent,
           (uint32_t)total_running_time, total_running_percent,
           (uint32_t)total_work_time, total_work_percent);
  }
}

void
stats_accumulate()
{
  uint32_t cur = spu_read_decrementer();
  uint32_t loaded_ticks;
  uint32_t running_ticks;

  stats_total_ticks = stats_first_start - cur;
  loaded_ticks = stats_loaded_start - cur;
  stats_last_loaded_ticks = loaded_ticks;
  stats_total_loaded_ticks += loaded_ticks;

  running_ticks = stats_running_ticks;

  if (UNLIKELY(stats_running_count != 0)) {
    running_ticks += stats_running_start - cur;
  }

  stats_last_running_ticks = running_ticks;
  stats_total_running_ticks += running_ticks;
  stats_last_work_ticks = stats_work_ticks;
  stats_total_work_ticks += stats_work_ticks;

  stats_running_ticks = 0;
  stats_work_ticks = 0;

#if STATS_PRINT_ON_UNLOAD
  stats_print(FALSE);
#endif

  // This has no effect when loaded/running count is 0
  cur = spu_read_decrementer();
  stats_loaded_start = cur;
  stats_running_start = cur;
}

void
run_stats_print(STATS_PRINT_CMD *cmd)
{
  UNUSED_PARAM(cmd);
  stats_print(TRUE);
  dep_complete_command();
}

void
run_stats_update(STATS_UPDATE_CMD *cmd)
{
  UNUSED_PARAM(cmd);
  stats_total_ticks = stats_first_start - spu_read_decrementer();
  dep_complete_command();
}

#endif
