
#ifndef __PEEK_STREAM_H
#define __PEEK_STREAM_H

#include <mysocket.h>

#define PEEK_STREAM_QUEUE_MAXSIZE 100

class peek_stream {
  
 private:
  mysocket *sock;
  int data_size;

  // the queue

  char *queue[PEEK_STREAM_QUEUE_MAXSIZE];
  int queue_size;  
  int head, tail;

  void init_queue();
  void free_queue();

  char *push_queue(); // increases queue size, returns pointer where to write data
  char *pop_queue(); // decreases queue size, returns pointer to data

 public:
  peek_stream(mysocket *sock, int data_size);

  char* peek(int depth);
  char* pop();
  
};

#endif
