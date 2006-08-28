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

#ifndef __NETWORK_CONNECTION_H
#define __NETWORK_CONNECTION_H

#include <stdlib.h>
#include <pthread.h>
#include <queue>

using namespace std;

class network_connection {

  // read/write queue   push() --> pop()

  queue<void*> free_buffers;

  queue<void*> data_queue;
  pthread_mutex_t queue_lock; 
  pthread_cond_t queue_push_cond; 
  pthread_cond_t queue_pop_cond; 

  pthread_mutex_t free_buffer_lock; 
  pthread_cond_t free_buffer_release_cond; 

  int fd;

  char *data_buffer;
  int data_offset;

  int read; // 1 - read connection, 0 - write connection

  friend class network_manager;
  friend void read_data(int fd, network_connection *c);
  friend void write_data(int fd, network_connection *c);

 public:

  network_connection(int fd, int read) {

    // allocate data buffers
    for (int y = 0; y < 25; y++) { 
      free_buffers.push(malloc(40000));
    }

    if (read) {
      data_buffer = (char*)malloc(40000);
      data_offset = 0;
    } else {
      data_offset = -1;
    }
    this->fd = fd;
    this->read = read;

    pthread_mutex_init(&queue_lock, NULL);
    pthread_cond_init(&queue_push_cond, NULL);
    pthread_cond_init(&queue_pop_cond, NULL);

    pthread_mutex_init(&free_buffer_lock, NULL);
    pthread_cond_init(&free_buffer_release_cond, NULL);
  }

  void *get_free_buffer() {
    void *res;
    pthread_mutex_lock(&free_buffer_lock);
    if (free_buffers.size() == 0) {
      pthread_cond_wait(&free_buffer_release_cond, &free_buffer_lock);
    }
    res = free_buffers.front();
    free_buffers.pop();
    pthread_mutex_unlock(&free_buffer_lock);
    return res;
  }

  void release_buffer(void *buf) {
    pthread_mutex_lock(&free_buffer_lock);
    free_buffers.push(buf);
    pthread_cond_signal(&free_buffer_release_cond);
    pthread_mutex_unlock(&free_buffer_lock);
  }

  inline int get_fd() {
    return fd;
  }

  inline bool queue_empty() {
    return queue_size() == 0;
  }

  inline bool queue_full() {
    return queue_size() >= 24;
  }

  inline void wait_for_data() {
    pthread_mutex_lock(&queue_lock);
    pthread_cond_wait(&queue_push_cond, &queue_lock);
    pthread_mutex_unlock(&queue_lock);
    return;
  }

  inline void wait_for_space() {
    pthread_mutex_lock(&queue_lock);
    pthread_cond_wait(&queue_pop_cond, &queue_lock);
    pthread_mutex_unlock(&queue_lock);
    return;
  }

  inline int queue_size() {
    int size;
    pthread_mutex_lock(&queue_lock);

    size = data_queue.size();

    pthread_mutex_unlock(&queue_lock);
    return size;
  }

  inline void push(void *ptr) {

    pthread_mutex_lock(&queue_lock);

    data_queue.push(ptr);
    pthread_cond_signal(&queue_push_cond);

    pthread_mutex_unlock(&queue_lock);
  }

  inline void* pop() {

    void *res;
    pthread_mutex_lock(&queue_lock);

    res = data_queue.front();
    data_queue.pop();
    pthread_cond_signal(&queue_pop_cond);

    pthread_mutex_unlock(&queue_lock);
    return res;
  }

};

#endif
