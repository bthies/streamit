// peek: 8 pop: 8 push 1
// init counts: 0 steady counts: 1

// ClusterFusion isEliminated: false

#include <stdlib.h>
#include <unistd.h>
#include <math.h>

#include <init_instance.h>
#include <mysocket.h>
#include <object_write_buffer.h>
#include <save_state.h>
#include <sdep.h>
#include <message.h>
#include <timer.h>
#include <thread_info.h>
#include <consumer2.h>
#include <consumer2p.h>
#include <producer2.h>
#include "cluster.h"
#include "fusion.h"
#include "global.h"

extern int __max_iteration;
extern int __init_iter;
extern int __timer_enabled;
extern int __frequency_of_chkpts;
extern volatile int __vol;
message *__msg_stack_1;
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

extern int BUFFER_0_1[];
extern int HEAD_0_1;
extern int TAIL_0_1;

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

extern int BUFFER_1_2[];
extern int HEAD_1_2;
extern int TAIL_1_2;


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

