
#include <service.h>

pthread_mutex_t __start_service_lock = PTHREAD_MUTEX_INITIALIZER;
service *__start_service_instance;
void *__start_service_method(void *);


void service::start() {

  pthread_mutex_lock(&__start_service_lock);
  
  __start_service_instance = this;
  
  pthread_t id;
  pthread_create(&id, NULL, __start_service_method, NULL);    
}


void *__start_service_method(void *) {
  __start_service_instance->unlock_and_run();
}
