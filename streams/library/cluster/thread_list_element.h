
#ifndef __THREAD_LIST_ELEMENT
#define __THREAD_LIST_ELEMENT

#include <pthread.h>

class thread_list_element {
  
  int thread_id;
  
  pthread_t pthread;

  thread_list_element *next;

 public:
  
  thread_list_element(int thread_id,
		      pthread_t pthread,
		      thread_list_element* next);

  int get_thread_id();

  pthread_t get_pthread();

  thread_list_element *get_next();

};


#endif
