#ifndef __PRODUCER2_H
#define __PRODUCER2_H

#include <socket_holder.h>
#include <serializable.h>

#define PRODUCER_BUFFER_SIZE 10000

template <class T>
class producer2 : public socket_holder, public serializable {

  T *buf;
  int offs;
  int item_count;

 public:

  producer2() {
    buf = (T*)malloc(PRODUCER_BUFFER_SIZE * sizeof(T));
    offs = 0;
    item_count = 0;
  }

  virtual void write_object(object_write_buffer *) {}
  virtual void read_object(object_write_buffer *) {}

  inline void push_items(T *data, int num) {
    for (int i = 0; i < num; i++) push(data[i]);
  }

  inline void push(T data) {

    buf[offs++] = data;
    item_count++;
    
    if (offs == PRODUCER_BUFFER_SIZE) {
      sock->write_chunk((char*)buf, PRODUCER_BUFFER_SIZE * sizeof(T));
      offs = 0;
    }
  }

  inline void flush() {
    
    if (offs > 0) {
      sock->write_chunk((char*)buf, PRODUCER_BUFFER_SIZE * sizeof(T)); // write more
      offs = 0;
    }
  }

};

#endif
