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

// proc_timer keeps track of the clock cycles consumed by a process

class proc_timer {

  // keeps track of start and stop for current segment
  struct tms t_start, t_end;

  // total times accumulated by this timer
  double user, system;

  // name for this timer
  char* name;

 public:
  proc_timer(char* _name);

  // inline to help avoid big overhead
  inline void start() { 
    times(&t_start); 
  }

  // inline to help avoid big overhead
  inline void stop() {
    times(&t_end);
    
    // add to cumulative
    user += t_end.tms_utime - t_start.tms_utime;
    system += t_end.tms_stime - t_start.tms_stime;
  }

  void output(FILE *f);
};

#endif
