
#ifndef __THREAD_LIST_ELEMENT
#define __THREAD_LIST_ELEMENT

#include <pthread.h>

#define PAUSE_STATE 0
#define RUN_STATE 1

class thread_list_element {
  
  int thread_id;
  
  pthread_t pthread;

  int *state_flag;

  thread_list_element *next;

 public:
  
  thread_list_element(int thread_id,
		      pthread_t pthread,
		      int *state_flag,
		      thread_list_element* next);

  int get_thread_id();

  pthread_t get_pthread();

  int *get_state_flag();

  thread_list_element *get_next();

};


#endif
