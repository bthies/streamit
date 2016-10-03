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


void __INITSTAGE__0__2();
void * __main____3(void * arg__9);
void init__17__20__4();
void ___initWork__15__21__5();
void init__2__23__6();
void init__5__24__7();
void init__12__25__8();


int ___PEEK_BUFFER_1__19__destroyed_0__0 = 0;
int seed__0__18__1 = 0;


void buffer_and_address_init__n0() {
}


void __INITSTAGE__0__2() {
  ___initWork__15__21__5();
}


void * __main____3(void * arg__9) {
  setCPUAffinity(0);
  buffer_and_address_init__n0();
  init__17__20__4();
  __INITSTAGE__0__2();

  while (1) {
    int ___POP_BUFFER_2_1__56__destroyed_0__10 = 0;
    int ___POP_BUFFER_1_1__50__destroyed_1__11 = 0;
    int ___POP_BUFFER_1_1__50__destroyed_0__12 = 0;
    int __tmp2__6__62__13 = 0;
    int __tmp3__7__63__14 = 0;
    int __tmp4__8__64__15 = 0;
    int __tmp5__9__65__16 = 0;
    int __tmp6__13__66__17 = 0;
    (___POP_BUFFER_2_1__56__destroyed_0__10 = 0);
    (___POP_BUFFER_1_1__50__destroyed_1__11 = 0);
    (___POP_BUFFER_1_1__50__destroyed_0__12 = 0);
    (__tmp2__6__62__13 = 0);
    (__tmp3__7__63__14 = 0);
    (__tmp4__8__64__15 = 0);
    (__tmp5__9__65__16 = 0);
    (__tmp6__13__66__17 = 0);
    // mark begin: filter Fused_Ran_Mov_Pri__67_7.work__16__22

    (___POP_BUFFER_1_1__50__destroyed_1__11 = (seed__0__18__1));
    ((seed__0__18__1) = (((65793 * (seed__0__18__1)) + 4282663) % 8388608));
    (___POP_BUFFER_1_1__50__destroyed_0__12 = (___PEEK_BUFFER_1__19__destroyed_0__0));
    (__tmp2__6__62__13 = 0);
    (__tmp3__7__63__14 = 0);
    (__tmp4__8__64__15 = 0);
    (__tmp5__9__65__16 = 0);
    (__tmp4__8__64__15 = ___POP_BUFFER_1_1__50__destroyed_0__12);
    (__tmp5__9__65__16 = ___POP_BUFFER_1_1__50__destroyed_1__11);
    (__tmp3__7__63__14 = (__tmp4__8__64__15 + __tmp5__9__65__16));
    (__tmp2__6__62__13 = (__tmp3__7__63__14 / 2));
    (___POP_BUFFER_2_1__56__destroyed_0__10 = __tmp2__6__62__13);
    ((___PEEK_BUFFER_1__19__destroyed_0__0) = ___POP_BUFFER_1_1__50__destroyed_1__11);
    (__tmp6__13__66__17 = 0);
    (__tmp6__13__66__17 = ___POP_BUFFER_2_1__56__destroyed_0__10);

    // TIMER_PRINT_CODE: __print_sink__ += (int)(__tmp6__13__66__17); 
    printf( "%d", __tmp6__13__66__17); printf("\n");

    // mark end: filter Fused_Ran_Mov_Pri__67_7.work__16__22

  }
  pthread_exit(NULL);
}


void init__17__20__4() {
  // mark begin: filter Fused_Ran_Mov_Pri__67_7.init__17__20

  init__2__23__6();
  init__5__24__7();
  init__12__25__8();
  // mark end: filter Fused_Ran_Mov_Pri__67_7.init__17__20

}


void ___initWork__15__21__5() {
  int ___POP_BUFFER_1_0__32__destroyed_0__18 = 0;
  (___POP_BUFFER_1_0__32__destroyed_0__18 = 0);
  // mark begin: filter Fused_Ran_Mov_Pri__67_7.___initWork__15__21

  (___POP_BUFFER_1_0__32__destroyed_0__18 = (seed__0__18__1));
  ((seed__0__18__1) = (((65793 * (seed__0__18__1)) + 4282663) % 8388608));
  ((___PEEK_BUFFER_1__19__destroyed_0__0) = ___POP_BUFFER_1_0__32__destroyed_0__18);
  // mark end: filter Fused_Ran_Mov_Pri__67_7.___initWork__15__21

}


void init__2__23__6() {
  // mark begin: filter Fused_Ran_Mov_Pri__67_7.init__2__23

  ((seed__0__18__1) = 0);
  // mark end: filter Fused_Ran_Mov_Pri__67_7.init__2__23

}


void init__5__24__7() {
  // mark begin: filter Fused_Ran_Mov_Pri__67_7.init__5__24

  // mark end: filter Fused_Ran_Mov_Pri__67_7.init__5__24

}


void init__12__25__8() {
  // mark begin: filter Fused_Ran_Mov_Pri__67_7.init__12__25

  // mark end: filter Fused_Ran_Mov_Pri__67_7.init__12__25

}
