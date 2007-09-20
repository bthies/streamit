#include <unistd.h>
#include <sys/times.h>

static clock_t ticks_per_sec;

// Returns the number of milliseconds that have elapsed since something
int
ticks() {
  struct tms t;
  return times(&t) * 1000 / ticks_per_sec;
}

void
init_ticks()
{
  ticks_per_sec = sysconf(_SC_CLK_TCK);
}
