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

#ifndef __PEEK_STREAM_H
#define __PEEK_STREAM_H

#include <serializable.h>
#include <data_consumer.h>

#define PEEK_STREAM_QUEUE_MAXSIZE 1024

template <class T> 
class peek_stream : public serializable {
  
 private:
  data_consumer *input;

  // the queue

  T *queue[PEEK_STREAM_QUEUE_MAXSIZE];
  int queue_size, head, tail;

  void init_queue() {
    for (int t = 0; t <  PEEK_STREAM_QUEUE_MAXSIZE; t++) {
      queue[t] = (T*)malloc(sizeof(T));
    }
    head = tail = queue_size = 0;
  }

  void free_queue() {
    for (int t = 0; t < PEEK_STREAM_QUEUE_MAXSIZE; t++) {
      free(queue[t]);
    }
  }

  T *push_queue() {
    
    if (queue_size == PEEK_STREAM_QUEUE_MAXSIZE) return NULL;
    
    int old_head = head;
    queue_size++;
    head += 1;
    if (head == PEEK_STREAM_QUEUE_MAXSIZE) head = 0;
    return queue[old_head];
  }

  T *pop_queue() {
    
    if (queue_size == 0) return NULL;
    
    int old_tail = tail;
    queue_size--;
    tail += 1;
    if (tail == PEEK_STREAM_QUEUE_MAXSIZE) tail = 0;
    return queue[old_tail];
  }


 public:
  peek_stream(data_consumer *input) {
    this->input = input;
    init_queue();
  }

  virtual void write_object(object_write_buffer *buf) {
    buf->write_int(queue_size);
    
    int c_index = tail;
    for (int i = 0; i < queue_size; i++) {
      buf->write(queue[c_index], sizeof(T));
      c_index++;
      if (c_index == PEEK_STREAM_QUEUE_MAXSIZE) c_index = 0;
    }
  }

  virtual void read_object(object_write_buffer *buf) {
    queue_size = buf->read_int();

    tail = head = 0;

    for (int i = 0; i < queue_size; i++) {
      buf->read(queue[head], sizeof(T));
      head++;
    }
  }

  T pop() {
  
    if (queue_size == 0) {
      input->read_item((void*)queue[0], sizeof(T));
      return *(queue[0]);
    }
    
    return *(pop_queue());
  }

  T peek(int depth) {

    depth++; // translate depth to 1 - next item, 2 - second next item, etc.
    
    if (depth > queue_size) {
      
      int t = depth - queue_size;
      while (t > 0) {
	T *buf = push_queue();
	input->read_item((void*)buf, sizeof(T));
	t--;
      }
    }
    
    int index = (tail + depth - 1) % PEEK_STREAM_QUEUE_MAXSIZE;
    
    return *(queue[index]);
  }

  
};

#endif
