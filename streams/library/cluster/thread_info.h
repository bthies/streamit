
#ifndef __THREAD_INFO_H
#define __THREAD_INFO_H

#include <connection_info.h>

#include <pthread.h>
#include <unistd.h>
#include <stdio.h>
#include <vector>

using namespace std;

#define RUN_STATE 0

#define PAUSE_PROPER_REQUEST 1  //set by service thread. Asks thread
                                //to enter pause state between
                                //iterations

#define PAUSE_ANY_REQUEST 2     //set by service thread. Asks thread
                                //to enter pause state between
                                //iterations or during I/O
 
#define PAUSE_PROPER_ENTERED 3  //set by thread to signal that it has
                                //entered pause state between iterations

#define PAUSE_IO_ENTERED 4      //set by thread to signal that it has
                                //entered pause state during io

#define EXIT_THREAD 5


class thread_info {
  
  int thread_id;
  void (*check_thread_status_during_io)();

  pthread_t pthread;
  int state_flag;
  int latest_checkpoint;

  bool active;

  vector <connection_info*> incoming_data; // incoming data connections
  vector <connection_info*> outgoing_data; // outgoing data connections

 public:
  
  thread_info(int thread_id, void (*check_thread_status_during_io)() = NULL);

  int get_thread_id();

  void set_active(bool a);
  bool is_active();

  void add_incoming_data_connection(connection_info* info);
  void add_outgoing_data_connection(connection_info* info);

  vector<connection_info*> get_incoming_data_connections();
  vector<connection_info*> get_outgoing_data_connections();

  void set_pthread(pthread_t pthread);
 
  pthread_t get_pthread();
  int *get_state_flag();
  int *get_latest_checkpoint();
};


inline void exit_thread(thread_info *info) {

  printf("thread %d exited!\n", info->get_thread_id());
  pthread_exit(NULL);
}


inline void check_thread_status(int *flag, thread_info *info) {

  if (*flag == RUN_STATE) return;

  if (*flag == EXIT_THREAD) exit_thread(info);

  if (*flag == PAUSE_ANY_REQUEST || *flag == PAUSE_PROPER_REQUEST) {

    *flag = PAUSE_PROPER_ENTERED;
  
    for(;;) {
      
      usleep(10000); // sleep 1/100th of a second
 
      if (*flag == RUN_STATE) return;
      if (*flag == EXIT_THREAD) exit_thread(info);
    }
  }

}


inline void check_thread_status_during_io(int *flag, thread_info *info) {

  if (*flag == RUN_STATE) return;

  if (*flag == PAUSE_PROPER_REQUEST) return;

  if (*flag == EXIT_THREAD) exit_thread(info);

  if (*flag == PAUSE_ANY_REQUEST) {

    *flag = PAUSE_IO_ENTERED;
  
    for(;;) {

      usleep(10000); // sleep 1/100th of a second

      if (*flag == RUN_STATE) return;
      if (*flag == EXIT_THREAD) exit_thread(info);
    }
  }
}


#endif

