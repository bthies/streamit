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


// code for core 0


void __INITSTAGE__0__1();
void * __main____2(void * arg__8);
void init__17__19__3();
void ___initWork__15__20__4();
void init__2__22__5();
void init__6__23__6();
void init__12__24__7();


int seed__0__18__0 = 0;


void buffer_and_address_init__n0() {
}


void __INITSTAGE__0__1() {
  ___initWork__15__20__4();
}


void * __main____2(void * arg__8) {
  setCPUAffinity(0);
  buffer_and_address_init__n0();
  init__17__19__3();
  __INITSTAGE__0__1();

  while (1) {
    int ___POP_BUFFER_2_1__58__destroyed_0__9 = 0;
    int ___POP_BUFFER_1_1__52__destroyed_0__10 = 0;
    int __tmp2__7__64__11 = 0;
    int __tmp5__13__65__12 = 0;
    (___POP_BUFFER_2_1__58__destroyed_0__9 = 0);
    (___POP_BUFFER_1_1__52__destroyed_0__10 = 0);
    (__tmp2__7__64__11 = 0);
    (__tmp5__13__65__12 = 0);
    // mark begin: filter Fused_Ran_Dup_Pri__66_7.work__16__21

    (___POP_BUFFER_1_1__52__destroyed_0__10 = (seed__0__18__0));
    ((seed__0__18__0) = (((65793 * (seed__0__18__0)) + 4282663) % 8388608));
    (__tmp2__7__64__11 = 0);
    (__tmp2__7__64__11 = ___POP_BUFFER_1_1__52__destroyed_0__10);
    (___POP_BUFFER_2_1__58__destroyed_0__9 = __tmp2__7__64__11);
    (__tmp5__13__65__12 = 0);
    (__tmp5__13__65__12 = ___POP_BUFFER_2_1__58__destroyed_0__9);

    // TIMER_PRINT_CODE: __print_sink__ += (int)(__tmp5__13__65__12); 
    printf( "%d", __tmp5__13__65__12); printf("\n");

    // mark end: filter Fused_Ran_Dup_Pri__66_7.work__16__21

  }
  pthread_exit(NULL);
}


void init__17__19__3() {
  // mark begin: filter Fused_Ran_Dup_Pri__66_7.init__17__19

  init__2__22__5();
  init__6__23__6();
  init__12__24__7();
  // mark end: filter Fused_Ran_Dup_Pri__66_7.init__17__19

}


void ___initWork__15__20__4() {
  int ___POP_BUFFER_1_0__31__destroyed_0__13 = 0;
  int ___POP_BUFFER_2_0__37__destroyed_1__14 = 0;
  int ___POP_BUFFER_2_0__37__destroyed_0__15 = 0;
  int __tmp3__8__43__16 = 0;
  int __tmp4__9__44__17 = 0;
  int __tmp5__13__45__conflict__0__18 = 0;
  int __tmp5__13__45__19 = 0;
  (___POP_BUFFER_1_0__31__destroyed_0__13 = 0);
  (___POP_BUFFER_2_0__37__destroyed_1__14 = 0);
  (___POP_BUFFER_2_0__37__destroyed_0__15 = 0);
  (__tmp3__8__43__16 = 0);
  (__tmp4__9__44__17 = 0);
  (__tmp5__13__45__conflict__0__18 = 0);
  (__tmp5__13__45__19 = 0);
  // mark begin: filter Fused_Ran_Dup_Pri__66_7.___initWork__15__20

  (___POP_BUFFER_1_0__31__destroyed_0__13 = (seed__0__18__0));
  ((seed__0__18__0) = (((65793 * (seed__0__18__0)) + 4282663) % 8388608));
  (__tmp3__8__43__16 = 0);
  (__tmp4__9__44__17 = 0);
  (__tmp3__8__43__16 = ___POP_BUFFER_1_0__31__destroyed_0__13);
  (___POP_BUFFER_2_0__37__destroyed_0__15 = __tmp3__8__43__16);
  (__tmp4__9__44__17 = ___POP_BUFFER_1_0__31__destroyed_0__13);
  (___POP_BUFFER_2_0__37__destroyed_1__14 = __tmp4__9__44__17);
  (__tmp5__13__45__conflict__0__18 = 0);
  (__tmp5__13__45__conflict__0__18 = ___POP_BUFFER_2_0__37__destroyed_0__15);

  // TIMER_PRINT_CODE: __print_sink__ += (int)(__tmp5__13__45__conflict__0__18); 
  printf( "%d", __tmp5__13__45__conflict__0__18); printf("\n");

  (__tmp5__13__45__19 = 0);
  (__tmp5__13__45__19 = ___POP_BUFFER_2_0__37__destroyed_1__14);

  // TIMER_PRINT_CODE: __print_sink__ += (int)(__tmp5__13__45__19); 
  printf( "%d", __tmp5__13__45__19); printf("\n");

  // mark end: filter Fused_Ran_Dup_Pri__66_7.___initWork__15__20

}


void init__2__22__5() {
  // mark begin: filter Fused_Ran_Dup_Pri__66_7.init__2__22

  ((seed__0__18__0) = 0);
  // mark end: filter Fused_Ran_Dup_Pri__66_7.init__2__22

}


void init__6__23__6() {
  // mark begin: filter Fused_Ran_Dup_Pri__66_7.init__6__23

  // mark end: filter Fused_Ran_Dup_Pri__66_7.init__6__23

}


void init__12__24__7() {
  // mark begin: filter Fused_Ran_Dup_Pri__66_7.init__12__24

  // mark end: filter Fused_Ran_Dup_Pri__66_7.init__12__24

}
