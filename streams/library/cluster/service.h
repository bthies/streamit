
#ifndef __SERVICE_H
#define __SERVICE_H

#include <pthread.h>

class service;

extern pthread_mutex_t __start_service_lock;
extern service *__start_service_instance;
void *__start_service_method(void *);

class service {

  virtual void run() = 0;

 public:

  void start();

  friend void *__start_service_method(void *);

};


#endif
