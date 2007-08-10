#include "defs.h"

#if STATS_ENABLE
#include <stdio.h>
#include "depend.h"
#include "stats.h"

bool_t stats_started = FALSE;
uint32_t stats_first_start;

uint8_t stats_loaded_count = 0;
uint32_t stats_loaded_start;
uint32_t stats_total_loaded_ticks = 0;

uint8_t stats_running_count = 0;
uint32_t stats_running_start;
uint32_t stats_running_ticks = 0;
uint32_t stats_total_running_ticks = 0;

uint32_t stats_work_ticks = 0;
uint32_t stats_total_work_ticks = 0;

static INLINE uint32_t
stats_percentage(float a, float total)
{
  return a * float_re(total) * 100.f;
}

void
stats_accumulate()
{
  uint32_t cur = spu_read_decrementer();
  uint32_t total_ticks;
  uint32_t loaded_ticks;
  float total_time;
  float loaded_time;
  float total_loaded_time;
  float running_time;
  float total_running_time;
  float work_time;
  float total_work_time;

  total_ticks = stats_first_start - cur;
  loaded_ticks = stats_loaded_start - cur;
  stats_total_loaded_ticks += loaded_ticks;

  if (UNLIKELY(stats_running_count != 0)) {
    stats_running_ticks += stats_running_start - cur;
  }

  stats_total_running_ticks += stats_running_ticks;
  stats_total_work_ticks += stats_work_ticks;

  total_time = total_ticks * TICK_PERIOD;
  loaded_time = loaded_ticks * TICK_PERIOD;
  total_loaded_time = stats_total_loaded_ticks * TICK_PERIOD;
  running_time = stats_running_ticks * TICK_PERIOD;
  total_running_time = stats_total_running_ticks * TICK_PERIOD;
  work_time = stats_work_ticks * TICK_PERIOD;
  total_work_time = stats_total_work_ticks * TICK_PERIOD;
  
  printf("SPU %d:            L:%8d      R:%8d(%3d) W:%8d(%3d)\n"
         "       T:%8d   %8d(%3d)   %8d(%3d)   %8d(%3d)\n",
         dep_params.id,
         (uint32_t)loaded_time,
         (uint32_t)running_time, stats_percentage(running_time, loaded_time),
         (uint32_t)work_time, stats_percentage(work_time, loaded_time),
         (uint32_t)total_time,
         (uint32_t)total_loaded_time,
         stats_percentage(total_loaded_time, total_time),
         (uint32_t)total_running_time,
         stats_percentage(total_running_time, total_time),
         (uint32_t)total_work_time,
         stats_percentage(total_work_time, total_time));

  if (UNLIKELY(stats_loaded_count != 0)) {
    cur = spu_read_decrementer();
    stats_loaded_start = cur;

    if (UNLIKELY(stats_running_count != 0)) {
      stats_running_start = cur;
    }
  }

  stats_running_ticks = 0;
  stats_work_ticks = 0;
}

#endif
