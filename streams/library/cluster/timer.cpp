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

// to keep track of the total elapsed time
double total_time = 0.0;

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
  my_time = 0.0;
}

void proc_timer::output(FILE *f) {
  my_time = my_time * 100.0 / total_time;

  fprintf(f, "%s: %.02f%\n",
	 name, my_time);
}

