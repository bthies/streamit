#include "barrier.h"

int FetchAndDecr(volatile int *mem)
{
  int val = -1;

  asm volatile ("lock; xaddl %0,%1"
		: "=r" (val), "=m" (*mem)
		: "0" (val), "m" (*mem)
		: "memory", "cc");
  return val - 1;
}

void barrier_init(barrier_t *barrier, int num_threads) {
  barrier->num_threads = num_threads;
  barrier->count = num_threads;
  barrier->generation = 0;
}

void barrier_wait(barrier_t *barrier) {
  int cur_gen = barrier->generation;

  if(FetchAndDecr(&barrier->count) == 0) {
    barrier->count = barrier->num_threads;
    barrier->generation++;
  }
  else {
    while(cur_gen == barrier->generation);
  }
}
