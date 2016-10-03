#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include <stdio.h>
#include <math.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdint.h>
#include <fcntl.h>
#include <pthread.h>
#include <sched.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <string.h>

#include "globals.h"
#include "barrier.h"
#include "rdtsc.h"
#include "structs.h"


// Global barrier
barrier_t barrier;

// Set CPU affinity for thread
void setCPUAffinity(int core) {
  cpu_set_t cpu_set;
  CPU_ZERO(&cpu_set);
  CPU_SET(core, &cpu_set);

  if(pthread_setaffinity_np(pthread_self(), sizeof(cpu_set_t), &cpu_set) < 0) {
    printf("Error setting pthread affinity\n");
    exit(-1);
  }
}

// main() Function Here
int main(int argc, char** argv) {

  // Initialize barrier
  barrier_init(&barrier, 1);

  // Spawn threads
  int rc;
  pthread_attr_t attr;
  void *status;

  pthread_attr_init(&attr);
  pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

  pthread_t thread_n0;
  if ((rc = pthread_create(&thread_n0, NULL, __main____11, (void *)NULL)) < 0)
    printf("Error creating thread for core 0: %d\n", rc);

  pthread_attr_destroy(&attr);

  if ((rc = pthread_join(thread_n0, &status)) < 0) {
    printf("Error joining thread for core 0: %d\n", rc);
    exit(-1);
  }

  // Exit
  pthread_exit(NULL);
}
