
#ifndef __THREAD_INFO_H
#define __THREAD_INFO_H

#include <connection_info.h>

#include <pthread.h>
#include <unistd.h>
#include <vector>

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

class thread_info {
  
  int thread_id;
  void (*check_thread_status_during_io)();

  pthread_t pthread;
  int *state_flag;

  vector <connection_info*> incoming_data; // incoming data connections
  vector <connection_info*> outgoing_data; // outgoing data connections

 public:
  
  thread_info(int thread_id, void (*check_thread_status_during_io)() = NULL);

  int get_thread_id();

  void add_incoming_data_connection(connection_info* info);
  void add_outgoing_data_connection(connection_info* info);

  vector<connection_info*> get_incoming_data_connections();
  vector<connection_info*> get_outgoing_data_connections();

  void set_pthread(pthread_t pthread);
  void set_state_flag(int *state_flag);
 
  pthread_t get_pthread();
  int *get_state_flag();
};


inline void check_thread_status(int *flag, thread_info *info) {

  if (*flag == RUN_STATE) return;

  *flag = PAUSE_PROPER_ENTERED;
  
  for(;;) {

    usleep(10000); // sleep 1/100th of a second
    if (*flag == RUN_STATE) return;
  }
}


inline void check_thread_status_during_io(int *flag, thread_info *info) {

  if (*flag == RUN_STATE) return;
  if (*flag == PAUSE_PROPER_REQUEST) return;

  *flag = PAUSE_IO_ENTERED;
  
  for(;;) {

    usleep(10000); // sleep 1/100th of a second
    if (*flag == RUN_STATE) return;
  }
}


#endif

