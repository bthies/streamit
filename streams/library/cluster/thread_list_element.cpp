

#include <thread_list_element.h>

thread_list_element::thread_list_element(int thread_id, 
					 pthread_t pthread,
					 thread_list_element* next) {
  this->thread_id = thread_id;
  this->pthread = pthread;
  this->next = next;
}


int thread_list_element::get_thread_id() {
  return thread_id;
}


pthread_t thread_list_element::get_pthread() {
  return pthread;
}


thread_list_element *thread_list_element::get_next() {
  return next;
}

