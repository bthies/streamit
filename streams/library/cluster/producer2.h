#ifndef __PRODUCER2_H
#define __PRODUCER2_H

#include <socket_holder.h>
#include <serializable.h>

#include <memsocket.h>
#include <netsocket.h>

#define PRODUCER_BUFFER_SIZE 10000

template <class T>
class producer2 : public socket_holder, public serializable {

  T *buf;
  int offs;
  int item_count;

 public:

  producer2() {
    buf = NULL;
    offs = 0;
    item_count = 0;
  }

  void init() {
    if (is_mem_socket) {
      ((memsocket*)sock)->set_buffer_size(PRODUCER_BUFFER_SIZE * sizeof(T));
      buf = (T*)((memsocket*)sock)->get_free_buffer();
    } else {
      buf = (T*)malloc(PRODUCER_BUFFER_SIZE * sizeof(T));
    }
  }

  virtual void write_object(object_write_buffer *) {}
  virtual void read_object(object_write_buffer *) {}

  inline void send_buffer() {
    if (is_mem_socket) {

      while (((memsocket*)sock)->queue_full()) {
	((memsocket*)sock)->wait_for_space();
      }
      ((memsocket*)sock)->push_buffer(buf);
      buf = (T*)((memsocket*)sock)->get_free_buffer();
      offs = 0;

    } else {
    
      ((netsocket*)sock)->write_chunk((char*)buf, PRODUCER_BUFFER_SIZE * sizeof(T));
      offs = 0;
      
    }
  }

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
