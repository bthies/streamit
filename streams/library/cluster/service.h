
#ifndef __SERVICE_H
#define __SERVICE_H

#include <pthread.h>

class service;

extern pthread_mutex_t __start_service_lock;
extern service *__start_service_instance;
void *__start_service_method(void *);

class service {

  virtual void run() = 0;

  void unlock_and_run() {
    pthread_mutex_unlock(&__start_service_lock);
    run();
  }

 public:

  void start();

  friend void *__start_service_method(void *);

};


#endif
