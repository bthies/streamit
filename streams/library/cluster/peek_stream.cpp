

#include <peek_stream.h>

#include <stdlib.h>

void peek_stream::init_queue() {
  for (int t = 0; t <  PEEK_STREAM_QUEUE_MAXSIZE; t++) {
    queue[t] = (char*)malloc(data_size);
  }
  head = tail = queue_size = 0;
}

void peek_stream::free_queue() {
  for (int t = 0; t < PEEK_STREAM_QUEUE_MAXSIZE; t++) {
    free(queue[t]);
  }
}

char *peek_stream::pop_queue() {

  if (queue_size == 0) return NULL;

  int old_tail = tail;
  queue_size--;
  tail += 1;
  if (tail == PEEK_STREAM_QUEUE_MAXSIZE) tail = 0;
  return queue[old_tail];
}

char *peek_stream::push_queue() {

  if (queue_size == PEEK_STREAM_QUEUE_MAXSIZE) return NULL;

  int old_head = head;
  queue_size++;
  head += 1;
  if (head == PEEK_STREAM_QUEUE_MAXSIZE) head = 0;
  return queue[old_head];
}

peek_stream::peek_stream(mysocket *sock, int data_size) {
  this->sock = sock;
  this->data_size = data_size;

  init_queue();
}

char *peek_stream::pop() {

  if (queue_size == 0) {
    sock->read_chunk(queue[0], data_size);
    return queue[0];
  }

  return pop_queue();
}

char *peek_stream::peek(int depth) {

  depth++; // translate depth to 1 - next item, 2 - second next item, etc.

  if (depth > queue_size) {
  
    int t = depth - queue_size;
    while (t > 0) {
      char *buf = push_queue();
      sock->read_chunk(buf, data_size);
      t--;
    }
  }

  int index = (tail + depth - 1) % PEEK_STREAM_QUEUE_MAXSIZE;

  return queue[index];
}





