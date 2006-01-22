
#include <timer.h>

#include <unistd.h>
#include <stdio.h>


void timer::start() {
  gettimeofday(&tv1, &tz);
}

void timer::stop() {
  gettimeofday(&tv2, &tz);
}

char *timer::get_str() {
  double elapsed = tv2.tv_sec - tv1.tv_sec + (tv2.tv_usec - tv1.tv_usec) / 1000000.0;
  sprintf(str, "Timer: %g sec", elapsed);
  return str;
}

void timer::output(FILE *f) {
  fprintf(f, "%s\n", get_str());
}

proc_timer::proc_timer(char* _name) {
  name = _name;
  user = 0.0;
  system = 0.0;
}

void proc_timer::output(FILE *f) {
  int ticks_per_sec = sysconf(_SC_CLK_TCK);
  
  user /= ticks_per_sec;
  system /= ticks_per_sec;

  fprintf(f, "%s: user %.02f; sys %.02f (%d ticks/sec)\n",
	 name, user, system, ticks_per_sec);
}

