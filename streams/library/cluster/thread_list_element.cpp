

#include <thread_list_element.h>

thread_list_element::thread_list_element(int thread_id, 
					 pthread_t pthread,
					 int *state_flag,
					 thread_list_element* next) {
  this->thread_id = thread_id;
  this->pthread = pthread;
  this->state_flag = state_flag;
  this->next = next;
}


int thread_list_element::get_thread_id() {
  return thread_id;
}


pthread_t thread_list_element::get_pthread() {
  return pthread;
}


int *thread_list_element::get_state_flag() {
  return state_flag;
}


thread_list_element *thread_list_element::get_next() {
  return next;
}

