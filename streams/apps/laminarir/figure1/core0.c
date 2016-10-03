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
void * __main____2(void * arg__10);
void init__41__43__3();
float frand__15__45__4();
void init__17__46__5();
void init__20__47__6();
void init__1__22__48__7();
void init__7__23__49__8();
void init__36__50__9();


int seed__14__42__0 = 0;


void buffer_and_address_init__n0() {
}


void __INITSTAGE__0__1() {
}


void * __main____2(void * arg__10) {
  setCPUAffinity(0);
  buffer_and_address_init__n0();
  init__41__43__3();
  __INITSTAGE__0__1();

  while (1) {
    float ___POP_BUFFER_2_1__63__destroyed_1__11 = 0.0f;
    float ___POP_BUFFER_2_1__63__destroyed_0__12 = 0.0f;
    float ___POP_BUFFER_1_1__57__destroyed_1__13 = 0.0f;
    float ___POP_BUFFER_1_1__57__destroyed_0__14 = 0.0f;
    float __tmp5__18__69__conflict__0__15 = 0.0f;
    float __tmp5__18__69__16 = 0.0f;
    float __sa0__2__25__71__17 = 0.0f;
    float __sa1__3__26__72__18 = 0.0f;
    float __tmp6__4__27__73__19 = 0.0f;
    float __sa2__8__29__75__20 = 0.0f;
    float __sa3__9__30__76__21 = 0.0f;
    float __tmp2__10__31__77__22 = 0.0f;
    double __tmp3__11__32__78__23 = 0.0f;
    double __tmp4__12__33__79__24 = 0.0f;
    float __tmp7__37__80__25 = 0.0f;
    float __tmp8__38__81__26 = 0.0f;
    (___POP_BUFFER_2_1__63__destroyed_1__11 = ((float)0.0));
    (___POP_BUFFER_2_1__63__destroyed_0__12 = ((float)0.0));
    (___POP_BUFFER_1_1__57__destroyed_1__13 = ((float)0.0));
    (___POP_BUFFER_1_1__57__destroyed_0__14 = ((float)0.0));
    (__tmp5__18__69__conflict__0__15 = ((float)0.0));
    (__tmp5__18__69__16 = ((float)0.0));
    (__sa0__2__25__71__17 = ((float)0.0));
    (__sa1__3__26__72__18 = ((float)0.0));
    (__tmp6__4__27__73__19 = ((float)0.0));
    (__sa2__8__29__75__20 = ((float)0.0));
    (__sa3__9__30__76__21 = ((float)0.0));
    (__tmp2__10__31__77__22 = ((float)0.0));
    (__tmp3__11__32__78__23 = ((float)0.0));
    (__tmp4__12__33__79__24 = ((float)0.0));
    (__tmp7__37__80__25 = ((float)0.0));
    (__tmp8__38__81__26 = ((float)0.0));
    // mark begin: filter Fused_A___Ano_D____82_18.work__40__44

    (__tmp5__18__69__conflict__0__15 = ((float)0.0));
    (__tmp5__18__69__conflict__0__15 = frand__15__45__4());
    (___POP_BUFFER_1_1__57__destroyed_0__14 = __tmp5__18__69__conflict__0__15);
    (__tmp5__18__69__16 = ((float)0.0));
    (__tmp5__18__69__16 = frand__15__45__4());
    (___POP_BUFFER_1_1__57__destroyed_1__13 = __tmp5__18__69__16);
    (__sa0__2__25__71__17 = ((float)0.0));
    (__sa1__3__26__72__18 = ((float)0.0));
    (__tmp6__4__27__73__19 = ((float)0.0));
    (__sa0__2__25__71__17 = ((float)0.0));
    (__sa1__3__26__72__18 = ((float)0.0));
    (__sa0__2__25__71__17 = ((float)0.0));
    (__sa1__3__26__72__18 = ((float)0.0));
    (__sa0__2__25__71__17 = ___POP_BUFFER_1_1__57__destroyed_0__14);
    (__sa1__3__26__72__18 = ___POP_BUFFER_1_1__57__destroyed_1__13);
    (__tmp6__4__27__73__19 = (__sa0__2__25__71__17 + (__sa1__3__26__72__18 / ((float)2.0))));
    (___POP_BUFFER_2_1__63__destroyed_0__12 = __tmp6__4__27__73__19);
    (__sa2__8__29__75__20 = ((float)0.0));
    (__sa3__9__30__76__21 = ((float)0.0));
    (__tmp2__10__31__77__22 = ((float)0.0));
    (__tmp3__11__32__78__23 = ((float)0.0));
    (__tmp4__12__33__79__24 = ((float)0.0));
    (__sa2__8__29__75__20 = ((float)0.0));
    (__sa3__9__30__76__21 = ((float)0.0));
    (__tmp2__10__31__77__22 = ((float)0.0));
    (__tmp3__11__32__78__23 = ((float)0.0));
    (__tmp4__12__33__79__24 = ((float)0.0));
    (__sa2__8__29__75__20 = ((float)0.0));
    (__sa3__9__30__76__21 = ((float)0.0));
    (__tmp2__10__31__77__22 = ((float)0.0));
    (__tmp3__11__32__78__23 = ((float)0.0));
    (__tmp4__12__33__79__24 = ((float)0.0));
    (__sa2__8__29__75__20 = ___POP_BUFFER_1_1__57__destroyed_0__14);
    (__sa3__9__30__76__21 = ___POP_BUFFER_1_1__57__destroyed_1__13);
    (__tmp2__10__31__77__22 = ((float)0.0));
    (__tmp3__11__32__78__23 = ((float)0.0));
    (__tmp4__12__33__79__24 = ((float)0.0));
    (__tmp4__12__33__79__24 = ((double)((__sa2__8__29__75__20 * __sa3__9__30__76__21))));
    (__tmp3__11__32__78__23 = sqrtf(__tmp4__12__33__79__24));
    (__tmp2__10__31__77__22 = ((float)(__tmp3__11__32__78__23)));
    (___POP_BUFFER_2_1__63__destroyed_1__11 = __tmp2__10__31__77__22);
    (__tmp7__37__80__25 = ((float)0.0));
    (__tmp8__38__81__26 = ((float)0.0));
    (__tmp7__37__80__25 = ___POP_BUFFER_2_1__63__destroyed_0__12);

    // TIMER_PRINT_CODE: __print_sink__ += (int)(__tmp7__37__80__25); 
    printf( "%f", __tmp7__37__80__25); printf("\n");

    (__tmp8__38__81__26 = ___POP_BUFFER_2_1__63__destroyed_1__11);

    // TIMER_PRINT_CODE: __print_sink__ += (int)(__tmp8__38__81__26); 
    printf( "%f", __tmp8__38__81__26); printf("\n");

    // mark end: filter Fused_A___Ano_D____82_18.work__40__44

  }
  pthread_exit(NULL);
}


void init__41__43__3() {
  // mark begin: filter Fused_A___Ano_D____82_18.init__41__43

  init__17__46__5();
  init__20__47__6();
  init__36__50__9();
  // mark end: filter Fused_A___Ano_D____82_18.init__41__43

}


float frand__15__45__4() {
  // mark begin: filter Fused_A___Ano_D____82_18.frand__15__45

  ((seed__14__42__0) = (((65793 * (seed__14__42__0)) + 4282663) % 8388608));
  return ((float)((((seed__14__42__0) < 0) ? (-(seed__14__42__0)) : (seed__14__42__0))));
  // mark end: filter Fused_A___Ano_D____82_18.frand__15__45

}


void init__17__46__5() {
  // mark begin: filter Fused_A___Ano_D____82_18.init__17__46

  ((seed__14__42__0) = 0);
  // mark end: filter Fused_A___Ano_D____82_18.init__17__46

}


void init__20__47__6() {
  // mark begin: filter Fused_A___Ano_D____82_18.init__20__47

  init__1__22__48__7();
  init__7__23__49__8();
  // mark end: filter Fused_A___Ano_D____82_18.init__20__47

}


void init__1__22__48__7() {
  // mark begin: filter Fused_A___Ano_D____82_18.init__1__22__48

  // mark end: filter Fused_A___Ano_D____82_18.init__1__22__48

}


void init__7__23__49__8() {
  // mark begin: filter Fused_A___Ano_D____82_18.init__7__23__49

  // mark end: filter Fused_A___Ano_D____82_18.init__7__23__49

}


void init__36__50__9() {
  // mark begin: filter Fused_A___Ano_D____82_18.init__36__50

  // mark end: filter Fused_A___Ano_D____82_18.init__36__50

}
