#ifndef RDTSC_H
#define RDTSC_H

#include <stdint.h>

/* Returns the number of clock cycles that have passed since the machine
 * booted up. */
static __inline__ uint64_t rdtsc(void)
{
  uint32_t hi, lo;
  asm volatile ("rdtsc" : "=a"(lo), "=d"(hi));
  return ((uint64_t ) lo) | (((uint64_t) hi) << 32);
}

#endif
