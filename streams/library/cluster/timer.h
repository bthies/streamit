
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

  struct tms t_start, t_end;

 public:
  void start();
  void stop();
  void output(FILE *f);
};

#endif
