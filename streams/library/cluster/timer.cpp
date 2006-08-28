/*
 * Copyright 2006 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

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

