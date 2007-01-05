// peek: 1 pop: 1 push 0
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
message *__msg_stack_2;
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

extern int BUFFER_1_2[];
extern int HEAD_1_2;
extern int TAIL_1_2;

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

