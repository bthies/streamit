/*-----------------------------------------------------------------------------
 * stats.h
 *
 * This tracks the following basic utilization stats:
 * - Total time since first command was received.
 * - Time spent with at least one loaded filter.
 * - Time spent with at least one running filter_run command (no outstanding
 *   dependencies).
 * - Time spent in work functions.
 *
 * Stats are printed to stdout whenever a filter is unloaded. The last 3 times
 * are recorded since the previous filter unload and cumulatively.
 *---------------------------------------------------------------------------*/

#ifndef _STATS_H_
#define _STATS_H_

#if STATS_ENABLE
#include <spu_mfcio.h>

extern bool_t stats_started;
extern uint32_t stats_first_start;

extern uint8_t stats_loaded_count;
extern uint32_t stats_loaded_start;

extern uint8_t stats_running_count;
extern uint32_t stats_running_start;
extern uint32_t stats_running_ticks;

extern uint32_t stats_work_ticks;

void stats_accumulate();

static INLINE void
stats_receive_command()
{
  if (UNLIKELY(!stats_started)) {
    stats_started = TRUE;
    spu_write_decrementer(0);
    stats_first_start = spu_read_decrementer();
  }
}

static INLINE void
stats_start_filter_load()
{
  if (LIKELY(stats_loaded_count++ == 0)) {
    stats_loaded_start = spu_read_decrementer();
  }
}

static INLINE void
stats_start_filter_unload()
{
  stats_loaded_count--;
  stats_accumulate();
}

static INLINE void
stats_start_filter_run()
{
  if (UNLIKELY(stats_running_count++ == 0)) {
    stats_running_start = spu_read_decrementer();
  }
}

static INLINE void
stats_done_filter_run()
{
  if (UNLIKELY(--stats_running_count == 0)) {
    stats_running_ticks += stats_running_start - spu_read_decrementer();
  }
}

#else // !STATS_ENABLE

#define stats_receive_command()
#define stats_start_filter_load()
#define stats_start_filter_unload()
#define stats_start_filter_run()
#define stats_done_filter_run()

#endif

#endif
