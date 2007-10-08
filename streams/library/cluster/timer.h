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

#ifndef __TIMER_H
#define __TIMER_H

#include <sys/time.h>
#include <sys/times.h>
#include <sys/types.h>
#include <stdio.h>

// timer keeps track of the wall time clock

class timer {

  struct timeval tv1, tv2;
  struct timezone tz;
  char str[128];

 public:
  void start();
  void stop();
  char *get_str();
  void output(FILE *f);
};

#define GET_TIME(x) __asm__ volatile(".byte 0x0f,0x31":"=A"(x))

extern double total_time;

// proc_timer keeps track of the clock cycles consumed by a process
class proc_timer {

  // keeps track of start and stop for current segment
  unsigned long long int t_start, t_end;

  // starting and ending times for complete run
  unsigned long long int start_run, end_run;

  // total times accumulated by this procedure
  double my_time;

  // name for this timer
  char* name;

 public:
  proc_timer(char* _name);

  // inline to help avoid big overhead
  inline void start() { 
      GET_TIME(t_start);
  }

  // inline to help avoid big overhead
  inline void stop() {
    GET_TIME(t_end);
    
    // add to cumulative
    my_time += t_end - t_start;
    total_time += t_end - t_start;
  }

  void output(FILE *f);
};

#endif
