#ifndef BARRIER_H
#define BARRIER_H

typedef struct barrier {
  int num_threads;
  volatile int count;
  volatile int generation;
} barrier_t;

extern int FetchAndDecr(volatile int *mem);
extern void barrier_init(barrier_t *barrier, int num_threads);
extern void barrier_wait(barrier_t *barrier);

#endif
