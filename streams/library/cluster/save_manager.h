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

#ifndef __SAVE_MANAGER_H
#define __SAVE_MANAGER_H

#include <stdio.h>
#include <unistd.h>
#include <pthread.h>
#include <queue>

#include <service.h>
#include <object_write_buffer.h>
#include <thread_info.h>

#define LOCK(var)   pthread_mutex_lock(var)
#define UNLOCK(var) pthread_mutex_unlock(var)

using namespace std;

class checkpoint_info {

 public:
  
  thread_info *t_info;
  int steady_iter;
  object_write_buffer *buf;

  checkpoint_info(thread_info *t_info, int steady_iter, object_write_buffer *buf) {
    this->t_info = t_info;
    this->steady_iter = steady_iter;
    this->buf = buf;
  }

};

class save_manager : public service {
 
  static pthread_mutex_t queue_lock;
  static queue <checkpoint_info*> checkpoints;

  static checkpoint_info *pop_item(); // removes a checkpoint from queue

  virtual void run();

 public:

  static void push_item(checkpoint_info *info); // adds checkpoint to queue

};

#endif



