#ifndef __PRODUCER2_H
#define __PRODUCER2_H

#include <socket_holder.h>
#include <serializable.h>

#define PRODUCER_BUFFER_SIZE 10000

template <class T>
class producer2 : public socket_holder, public serializable {

  T *buf;
  int offs;
  int item_size;
  int item_count;

 public:

  producer2() {
    buf = NULL;
    offs = 0;
    item_size = sizeof(T);
    item_count = 0;
  }

  void init();
  void send_buffer(); 

  virtual void write_object(object_write_buffer *) {}
  virtual void read_object(object_write_buffer *) {}

  inline void push_items(T *data, int num) {
    
  __start: 
    
    if (num < PRODUCER_BUFFER_SIZE - offs) {
      int _offs = offs;
      for (int i = 0; i < num; i++, _offs++) buf[_offs] = data[i];
      offs = _offs;
      return;
    }

    int avail = PRODUCER_BUFFER_SIZE - offs;
    int _offs = offs;
    for (int i = 0; i < avail; i++, _offs++) buf[_offs] = data[i];

    send_buffer();

    num -= avail;
    data += avail;

    goto __start;
  }

  inline void push(T data) {

    buf[offs++] = data;
    //item_count++;
    
    if (offs == PRODUCER_BUFFER_SIZE) send_buffer();
  }

  inline void flush() {
    
    if (offs > 0) send_buffer();
  }

};

#endif
