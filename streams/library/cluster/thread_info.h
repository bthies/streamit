
#ifndef __THREAD_INFO_H
#define __THREAD_INFO_H

#include <pthread.h>
#include <vector>

#define PAUSE_STATE 0
#define RUN_STATE 1

class thread_info {
  
  int thread_id;

  pthread_t pthread;
  int *state_flag;

  vector <int> incoming_data; // incoming data connections
  vector <int> outgoing_data; // outgoing data connections

 public:
  
  thread_info(int thread_id);

  int get_thread_id();

  void add_incoming_data_connection(int id);
  void add_outgoing_data_connection(int id);

  vector<int> get_incoming_data_connections();
  vector<int> get_outgoing_data_connections();

  void set_pthread(pthread_t pthread);
  void set_state_flag(int *state_flag);
 
  pthread_t get_pthread();
  int *get_state_flag();
};

#endif
