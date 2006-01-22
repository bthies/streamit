
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
  inline void proc_timer::stop() {
    times(&t_end);
    
    // add to cumulative
    user += t_end.tms_utime - t_start.tms_utime;
    system += t_end.tms_stime - t_start.tms_stime;
  }

  void output(FILE *f);
};

#endif
