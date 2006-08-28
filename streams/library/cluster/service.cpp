/*
 * Copyright 2006 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */
#include <stdio.h>
#include <service.h>

pthread_mutex_t __start_service_lock = PTHREAD_MUTEX_INITIALIZER;
service *__start_service_instance;
void *__start_service_method(void *);


void service::start() {

  //fprintf(stderr, "service:1 %s\n", "pthread_mutex_lock(&__start_service_lock);");
  pthread_mutex_lock(&__start_service_lock);
  
  __start_service_instance = this;
  
  pthread_t id;
  pthread_create(&id, NULL, __start_service_method, NULL);    
}


void *__start_service_method(void *) {
  //fprintf(stderr, "service:2 %s\n", "__start_service_instance->unlock_and_run();");
  __start_service_instance->unlock_and_run();
  return NULL;
}
