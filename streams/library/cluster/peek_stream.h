
#ifndef __PEEK_STREAM_H
#define __PEEK_STREAM_H

#include <mysocket.h>

#define PEEK_STREAM_QUEUE_MAXSIZE 100

template <class T> 
class peek_stream {
  
 private:
  mysocket *sock;

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
  peek_stream(mysocket *sock) {
    this->sock = sock;
    init_queue();
  }

  T pop() {
  
    if (queue_size == 0) {
      sock->read_chunk((char*)queue[0], sizeof(T));
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
	sock->read_chunk((char*)buf, sizeof(T));
	t--;
      }
    }
    
    int index = (tail + depth - 1) % PEEK_STREAM_QUEUE_MAXSIZE;
    
    return *(queue[index]);
  }

  
};

#endif
