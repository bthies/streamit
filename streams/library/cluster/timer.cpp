
#include <timer.h>

#include <unistd.h>
#include <stdio.h>


void timer::start() {
  gettimeofday(&tv1, &tz);
}

void timer::stop() {
  gettimeofday(&tv2, &tz);
}

void timer::output(FILE *f) {
  double elapsed = tv2.tv_sec - tv1.tv_sec + (tv2.tv_usec - tv1.tv_usec) / 1000000.0;
  fprintf(f, "Timer: %g sec\n", elapsed);
}




void proc_timer::start() {
  times(&t_start);
}

void proc_timer::stop() {
  times(&t_end);
}

void proc_timer::output(FILE *f) {
  int ticks_per_sec = sysconf(_SC_CLK_TCK);

  double user = t_end.tms_utime - t_start.tms_utime;
  double system = t_end.tms_stime - t_start.tms_stime;

  user /= ticks_per_sec;
  system /= ticks_per_sec;

  fprintf(f, "user: %.02f sys: %.02f (%d ticks/sec)\n",
	 user, system, ticks_per_sec);

}

