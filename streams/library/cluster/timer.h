
#ifndef __TIMER_H
#define __TIMER_H

#include <sys/time.h>
#include <sys/types.h>
#include <stdio.h>

class timer {

  struct timeval tv1, tv2;
  struct timezone tz;

 public:
  void start();
  void stop();
  void output(FILE *f);
};


#endif
