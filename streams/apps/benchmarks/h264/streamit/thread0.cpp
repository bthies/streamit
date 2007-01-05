// peek: 0 pop: 0 push 1
// init counts: 0 steady counts: 8

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
message *__msg_stack_0;
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

extern int BUFFER_0_1[];
extern int HEAD_0_1;
extern int TAIL_0_1;


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


