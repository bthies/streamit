
#include <timer.h>

#include <sys/time.h>
#include <sys/types.h>
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
