
#ifndef __SAVE_MANAGER_H
#define __SAVE_MANAGER_H

#include <stdio.h>
#include <unistd.h>
#include <pthread.h>
#include <queue>

#include <object_write_buffer.h>

#define LOCK(var)   pthread_mutex_lock(var)
#define UNLOCK(var) pthread_mutex_unlock(var)

class checkpoint_info {

 public:
  
  int thread;
  int steady_iter;
  object_write_buffer *buf;

  checkpoint_info(int thread, int steady_iter, object_write_buffer *buf) {
    this->thread = thread;
    this->steady_iter = steady_iter;
    this->buf = buf;
  }

};

void *run_save_manager(void *);

class save_manager {
 
  static pthread_mutex_t queue_lock;
  static queue <checkpoint_info*> checkpoints;

 public:

  static checkpoint_info *pop_item(); // removes a checkpoint from queue

  static void push_item(checkpoint_info *info); // pushes a checkpoint onto queue

  static void run(); // starts the save manager in a separate thread

};

#endif
