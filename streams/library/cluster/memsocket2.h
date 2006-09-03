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

#ifndef __MEMSOCKET_H
#define __MEMSOCKET_H

#include <assert.h>
#include <mysocket.h>
#include <pthread.h>
#include <stdlib.h>
#include <stdio.h>
#include <queue>

using namespace std;

//#define NUMBER_OF_BLOCKS 65536 // 4-10
#define NUMBER_OF_BLOCKS 64 // 4-10

class memsocket : public mysocket {

  void *buffer[NUMBER_OF_BLOCKS];
  int head, tail;
  pthread_mutex_t lock;

  pthread_cond_t push_cond;
  pthread_cond_t release_cond;

  int buffer_size;

 public:

  memsocket() {
    buffer_size = 0;
    head = 0;
    tail = 0;
    pthread_mutex_init(&lock, NULL);
    pthread_cond_init(&push_cond, NULL);
    pthread_cond_init(&release_cond, NULL);
  }

  virtual void set_buffer_size(int size) {

    pthread_mutex_lock(&lock);

    if (buffer_size == 0) {
      for (int i = 0; i < NUMBER_OF_BLOCKS; i++) {
	buffer[i] = malloc(size);
      }
      buffer_size = size;
    } else {
      if (size != buffer_size) {
	fprintf(stderr, "ERR: Not alowed to change memsocket buffer size!");
	exit(0);
      }
    }

    pthread_mutex_unlock(&lock);
  }
  
  virtual bool is_mem_socket() { return true; }
  virtual bool is_net_socket() { return false; }
  virtual void close() {}

  inline void *get_free_buffer() {

    assert(buffer_size > 0);
    
    return buffer[head];
  }

  inline void push_buffer(void *ptr) {

    assert(ptr == buffer[head]);

    pthread_mutex_lock(&lock);
    int nhead = (head + 1) & (NUMBER_OF_BLOCKS-1); 
    while (nhead == tail) {
      pthread_cond_wait(&release_cond, &lock);
    }
    head = nhead;
    pthread_cond_signal(&push_cond);
    pthread_mutex_unlock(&lock);
  }      

  inline void* pop_buffer() {

    pthread_mutex_lock(&lock);
    while (tail == head) {
      pthread_cond_wait(&push_cond, &lock);
    }
    pthread_mutex_unlock(&lock);
    return buffer[tail];
  }

  void release_buffer(void *buf) {

    assert(buf == buffer[tail]);

    pthread_mutex_lock(&lock);
    tail = (tail + 1) & (NUMBER_OF_BLOCKS-1); 
    pthread_cond_signal(&release_cond);
    pthread_mutex_unlock(&lock);
  }

};

#endif


