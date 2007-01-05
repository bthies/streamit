#include <math.h>
#include <pthread.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>

#include <netsocket.h>
#include <node_server.h>
#include <init_instance.h>
#include <master_server.h>
#include <save_state.h>
#include <save_manager.h>
#include <delete_chkpts.h>
#include <object_write_buffer.h>
#include <read_setup.h>
#include <ccp.h>
#include <timer.h>
#include "fusion.h"
#include "structs.h"

int __max_iteration;
int __timer_enabled = 0;
int __frequency_of_chkpts;
volatile int __vol;
proc_timer tt("total runtime");


int BUFFER_0_1[__BUF_SIZE_MASK_0_1 + 1];
int HEAD_0_1 = 0;
int TAIL_0_1 = 0;
int BUFFER_1_2[__BUF_SIZE_MASK_1_2 + 1];
int HEAD_1_2 = 0;
int TAIL_1_2 = 0;
extern void init_FileReader__2_4__0();
extern void FileReader__2_4__work__0(int);
extern void FileReader__2_4__work__0__close();
extern void init_BitStream2IntStream__9_5__1();
extern void work_BitStream2IntStream__9_5__1(int);
#ifdef BUFFER_MERGE
extern void work_BitStream2IntStream__9_5__1__mod(int ____n, int *____in, int *____out);
extern void work_BitStream2IntStream__9_5__1__mod2(int ____n, int *____in, int *____out, int s1, int s2);
#endif
extern void init_MacroblockMaker__12_6__2();
extern void work_MacroblockMaker__12_6__2(int);
#ifdef BUFFER_MERGE
extern void work_MacroblockMaker__12_6__2__mod(int ____n, int *____in, void *____out);
extern void work_MacroblockMaker__12_6__2__mod2(int ____n, int *____in, void *____out, int s1, int s2);
#endif

int main(int argc, char **argv) {
  read_setup::read_setup_file();
  __max_iteration = read_setup::max_iteration;
  for (int a = 1; a < argc; a++) {
    if (argc > a + 1 && strcmp(argv[a], "-i") == 0) {
      int tmp;
      sscanf(argv[a + 1], "%d", &tmp);
#ifdef VERBOSE
      fprintf(stderr,"Number of Iterations: %d\n", tmp);
#endif
      __max_iteration = tmp;
    }
    if (strcmp(argv[a], "-t") == 0) {
#ifdef VERBOSE
       fprintf(stderr,"Timer enabled.\n");
#endif
       __timer_enabled = 1;    }
  }
// number of phases: 3


  // ============= Initialization =============

init_FileReader__2_4__0();
init_BitStream2IntStream__9_5__1();
init_MacroblockMaker__12_6__2();

  // ============= Steady State =============

  if (__timer_enabled) {
    tt.start();
  }
  for (int n = 0; n < (__max_iteration  ); n++) {
HEAD_0_1 = 0;
TAIL_0_1 = 0;
    FileReader__2_4__work__0(8 );
HEAD_1_2 = 0;
TAIL_1_2 = 0;
    work_BitStream2IntStream__9_5__1(1 );
    work_MacroblockMaker__12_6__2(1 );
  }
if (__timer_enabled) {
    tt.stop();
    tt.output(stderr);
  }


  return 0;
}
