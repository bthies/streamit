#ifndef __CONSUMER2_H
#define __CONSUMER2_H

#include <socket_holder.h>
#include <serializable.h>

#include <memsocket.h>
#include <netsocket.h>

#define CONSUMER_BUFFER_SIZE 10000

template <class T>
class consumer2 : public socket_holder, public serializable {

  T *buf;
  int offs;
  int item_count;

 public:

  consumer2() {
    buf = NULL;
    offs = CONSUMER_BUFFER_SIZE;
    item_count = 0;
  }

  void init() {
    if (is_mem_socket) {
      ((memsocket*)sock)->set_buffer_size(CONSUMER_BUFFER_SIZE * sizeof(T));
    } else {
      buf =  (T*)malloc(CONSUMER_BUFFER_SIZE * sizeof(T));
    }
  }

  virtual void write_object(object_write_buffer *) {}
  virtual void read_object(object_write_buffer *) {}

  inline void recv_buffer() {
    if (is_mem_socket) {

      if (buf != NULL) ((memsocket*)sock)->release_buffer(buf);
      while (((memsocket*)sock)->queue_empty()) {
	((memsocket*)sock)->wait_for_data();
      }
      buf = (T*)((memsocket*)sock)->pop_buffer();
      offs = 0;
      
    } else {

      ((netsocket*)sock)->read_chunk((char*)buf, CONSUMER_BUFFER_SIZE * sizeof(T));
      offs = 0;
    }
  }

  inline void pop_items(T *data, int num) {

  __start:
    
    if (num <= CONSUMER_BUFFER_SIZE - offs) {
      int _offs = offs;
      for (int i = 0; i < num; i++, _offs++) data[i] = buf[_offs];
      offs = _offs;
      return;
    }
    
    int avail = CONSUMER_BUFFER_SIZE - offs;
    int _offs = offs;
    for (int i = 0; i < avail; i++, _offs++) data[i] = buf[_offs];

    recv_buffer();

    num -= avail;
    data += avail;
    
    goto __start;
  }
  
  inline T pop() {
    
    //item_count++;

    if (offs == CONSUMER_BUFFER_SIZE) {
      recv_buffer();
    }
    
    return buf[offs++];
  }

  inline T peek(int index) {
    
    if (offs == CONSUMER_BUFFER_SIZE) {
      recv_buffer();
    }
    
    return 0;
  }

};

#endif
