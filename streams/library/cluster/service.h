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
