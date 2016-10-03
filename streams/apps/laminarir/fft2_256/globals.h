#ifndef GLOBALS_H
#define GLOBALS_H

#include "barrier.h"
#include "structs.h"
#include <stdint.h>

#define ITERATIONS -1

#define minf(a, b) ((a) < (b) ? (a) : (b))
#define maxf(a, b) ((a) > (b) ? (a) : (b))

// Global barrier
extern barrier_t barrier;

// Shared buffers

// CPU Affinity
extern void setCPUAffinity(int core);

// Thread entry points
extern void *__main____255(void * arg);

#endif
