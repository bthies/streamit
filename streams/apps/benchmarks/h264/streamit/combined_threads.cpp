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
void init_FileReader__2_4__0();
void FileReader__2_4__work__0(int);
void FileReader__2_4__work__0__close();
void init_BitStream2IntStream__9_5__1();
void work_BitStream2IntStream__9_5__1(int);
#ifdef BUFFER_MERGE
void work_BitStream2IntStream__9_5__1__mod(int ____n, int *____in, int *____out);
void work_BitStream2IntStream__9_5__1__mod2(int ____n, int *____in, int *____out, int s1, int s2);
#endif
void init_MacroblockMaker__12_6__2();
void work_MacroblockMaker__12_6__2(int);
#ifdef BUFFER_MERGE
void work_MacroblockMaker__12_6__2__mod(int ____n, int *____in, void *____out);
void work_MacroblockMaker__12_6__2__mod2(int ____n, int *____in, void *____out, int s1, int s2);
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

// moved or inserted by concat_cluster_threads.pl
#include <message.h>
message *__msg_stack_1;
message *__msg_stack_0;
message *__msg_stack_2;

// end of moved or inserted by concat_cluster_threads.pl

// peek: 0 pop: 0 push 1
// init counts: 0 steady counts: 8

// ClusterFusion isEliminated: false


#include <mysocket.h>
#include <sdep.h>
#include <thread_info.h>
#include <consumer2.h>
#include <consumer2p.h>
#include <producer2.h>
#include "cluster.h"
#include "global.h"

int __number_of_iterations_0;
int __counter_0 = 0;
int __steady_0 = 0;
int __tmp_0 = 0;
int __tmp2_0 = 0;
int *__state_flag_0 = NULL;
thread_info *__thread_0 = NULL;



void save_file_pointer__0(object_write_buffer *buf);
void load_file_pointer__0(object_write_buffer *buf);

 
void init_FileReader__2_4__0();
inline void check_status__0();

void FileReader__2_4__work__0(int);



inline void __push__0(int data) {
BUFFER_0_1[HEAD_0_1]=data;
HEAD_0_1++;
}



#include <FileReader.h>
int __file_descr__0;

FILE* FileReader__2_4__fp;

void init_FileReader__2_4__0() {
  FileReader__2_4__fp = fopen("../testvideos/news.qcif", "r");
  assert (FileReader__2_4__fp);
}

void FileReader__2_4__work__0__close() {
  fclose(FileReader__2_4__fp);
}

void save_file_pointer__0(object_write_buffer *buf)
{ buf->write_int(FileReader_getpos(__file_descr__0)); }

void load_file_pointer__0(object_write_buffer *buf)
{ FileReader_setpos(__file_descr__0, buf->read_int()); }

void FileReader__2_4__work__0(int ____n) {
    for (; 0 < ____n; ____n--) {
      static unsigned char FileReader__2_4__the_bits = 0;
      static int FileReader__2_4__bits_to_go = 0;

      if (FileReader__2_4__bits_to_go == 0) {
        if (fread(&FileReader__2_4__the_bits, sizeof(FileReader__2_4__the_bits), 1, FileReader__2_4__fp)) {
          FileReader__2_4__bits_to_go = 8 * sizeof(FileReader__2_4__the_bits);
          __push__0((FileReader__2_4__the_bits & (1 << (sizeof(FileReader__2_4__the_bits) * 8 - 1))) ? 1 : 0);
          FileReader__2_4__the_bits <<= 1;
          FileReader__2_4__bits_to_go--;
        }
      } else {
        __push__0((FileReader__2_4__the_bits & (1 << (sizeof(FileReader__2_4__the_bits) * 8 - 1))) ? 1 : 0);
        FileReader__2_4__the_bits <<= 1;
        FileReader__2_4__bits_to_go--;
      }
    }
}


// peek: 8 pop: 8 push 1
// init counts: 0 steady counts: 1

// ClusterFusion isEliminated: false



int __number_of_iterations_1;
int __counter_1 = 0;
int __steady_1 = 0;
int __tmp_1 = 0;
int __tmp2_1 = 0;
int *__state_flag_1 = NULL;
thread_info *__thread_1 = NULL;



void save_peek_buffer__1(object_write_buffer *buf);
void load_peek_buffer__1(object_write_buffer *buf);
void save_file_pointer__1(object_write_buffer *buf);
void load_file_pointer__1(object_write_buffer *buf);

 
void init_BitStream2IntStream__9_5__1();
inline void check_status__1();

void work_BitStream2IntStream__9_5__1(int);


inline int __pop__1() {
int res=BUFFER_0_1[TAIL_0_1];
TAIL_0_1++;
return res;
}

inline int __pop__1(int n) {
int res=BUFFER_0_1[TAIL_0_1];
TAIL_0_1+=n;

return res;
}

inline int __peek__1(int offs) {
return BUFFER_0_1[TAIL_0_1+offs];
}



inline void __push__1(int data) {
BUFFER_1_2[HEAD_1_2]=data;
HEAD_1_2++;
}



 
void init_BitStream2IntStream__9_5__1(){
}
void save_file_pointer__1(object_write_buffer *buf) {}
void load_file_pointer__1(object_write_buffer *buf) {}


#ifdef BUFFER_MERGE


void work_BitStream2IntStream__9_5__1__mod(int ____n, int *____in, int *____out) {
  for (; (0 < ____n); ____n--)
{
  int some_int__5 = 0;/* int */
  int two_power__6 = 0;/* int */
  int add_int__7 = 0;/* int */
  int i__8 = 0;/* int */

  // mark begin: SIRFilter BitStream2IntStream

  (some_int__5 = 0)/*int*/;
  (two_power__6 = 1)/*int*/;

  // TIMER_PRINT_CODE: __print_sink__ += (int)("printing bits"); 
  printf( "%s", "printing bits"); printf("\n");

  for ((i__8 = 0)/*int*/; (i__8 < 8); (i__8++)) {{
      (add_int__7 = ((*(____in+(8 - i__8))) * two_power__6))/*int*/;
      (two_power__6 = (two_power__6 * 2))/*int*/;
      (some_int__5 = (some_int__5 + add_int__7))/*int*/;

      // TIMER_PRINT_CODE: __print_sink__ += (int)((*(____in+i__8))); 
      printf( "%d", (*(____in+i__8))); 
    }
  }

  // TIMER_PRINT_CODE: __print_sink__ += (int)("now to int value"); 
  printf( "%s", "now to int value"); printf("\n");

  ((*____out++)=some_int__5);
  // mark end: SIRFilter BitStream2IntStream

}}


void work_BitStream2IntStream__9_5__1__mod2(int ____n, int *____in, int *____out, int s1, int s2) {
  for (; (0 < ____n); (____n--, ____in+=s1, ____out+=s2))
{
  int some_int__5 = 0;/* int */
  int two_power__6 = 0;/* int */
  int add_int__7 = 0;/* int */
  int i__8 = 0;/* int */

  // mark begin: SIRFilter BitStream2IntStream

  (some_int__5 = 0)/*int*/;
  (two_power__6 = 1)/*int*/;

  // TIMER_PRINT_CODE: __print_sink__ += (int)("printing bits"); 
  printf( "%s", "printing bits"); printf("\n");

  for ((i__8 = 0)/*int*/; (i__8 < 8); (i__8++)) {{
      (add_int__7 = ((*(____in+(8 - i__8))) * two_power__6))/*int*/;
      (two_power__6 = (two_power__6 * 2))/*int*/;
      (some_int__5 = (some_int__5 + add_int__7))/*int*/;

      // TIMER_PRINT_CODE: __print_sink__ += (int)((*(____in+i__8))); 
      printf( "%d", (*(____in+i__8))); 
    }
  }

  // TIMER_PRINT_CODE: __print_sink__ += (int)("now to int value"); 
  printf( "%s", "now to int value"); printf("\n");

  ((*____out++)=some_int__5);
  // mark end: SIRFilter BitStream2IntStream

}}


#endif // BUFFER_MERGE


 
void work_BitStream2IntStream__9_5__1(int ____n){
  for (
  ; (0 < ____n); (____n--)) {{
      int some_int__5 = 0;/* int */
      int two_power__6 = 0;/* int */
      int add_int__7 = 0;/* int */
      int i__8 = 0;/* int */

      // mark begin: SIRFilter BitStream2IntStream

      (some_int__5 = 0)/*int*/;
      (two_power__6 = 1)/*int*/;

      // TIMER_PRINT_CODE: __print_sink__ += (int)("printing bits"); 
      printf( "%s", "printing bits"); printf("\n");

      for ((i__8 = 0)/*int*/; (i__8 < 8); (i__8++)) {{
          (add_int__7 = (__peek__1((8 - i__8)) * two_power__6))/*int*/;
          (two_power__6 = (two_power__6 * 2))/*int*/;
          (some_int__5 = (some_int__5 + add_int__7))/*int*/;

          // TIMER_PRINT_CODE: __print_sink__ += (int)(__peek__1(i__8)); 
          printf( "%d", __peek__1(i__8)); 
        }
      }

      // TIMER_PRINT_CODE: __print_sink__ += (int)("now to int value"); 
      printf( "%s", "now to int value"); printf("\n");

      __push__1(some_int__5);
      // mark end: SIRFilter BitStream2IntStream

    }
  }
}

// peek: 1 pop: 1 push 0
// init counts: 0 steady counts: 1

// ClusterFusion isEliminated: false



int __number_of_iterations_2;
int __counter_2 = 0;
int __steady_2 = 0;
int __tmp_2 = 0;
int __tmp2_2 = 0;
int *__state_flag_2 = NULL;
thread_info *__thread_2 = NULL;



void save_peek_buffer__2(object_write_buffer *buf);
void load_peek_buffer__2(object_write_buffer *buf);
void save_file_pointer__2(object_write_buffer *buf);
void load_file_pointer__2(object_write_buffer *buf);

 
void init_MacroblockMaker__12_6__2();
inline void check_status__2();

void work_MacroblockMaker__12_6__2(int);


inline int __pop__2() {
int res=BUFFER_1_2[TAIL_1_2];
TAIL_1_2++;
return res;
}

inline int __pop__2(int n) {
int res=BUFFER_1_2[TAIL_1_2];
TAIL_1_2+=n;

return res;
}

inline int __peek__2(int offs) {
return BUFFER_1_2[TAIL_1_2+offs];
}


 
void init_MacroblockMaker__12_6__2(){
}
void save_file_pointer__2(object_write_buffer *buf) {}
void load_file_pointer__2(object_write_buffer *buf) {}
 
void work_MacroblockMaker__12_6__2(int ____n){
  for (
  ; (0 < ____n); (____n--)) {{
      // mark begin: SIRFilter MacroblockMaker


      // TIMER_PRINT_CODE: __print_sink__ += (int)(__pop__2()); 
      printf( "%d", __pop__2()); printf("\n");

      // mark end: SIRFilter MacroblockMaker

    }
  }
}

