#ifndef __CONSUMER2_H
#define __CONSUMER2_H

#include <socket_holder.h>
#include <serializable.h>

#define CONSUMER_BUFFER_SIZE 10000

template <class T>
class consumer2 : public socket_holder, public serializable {

  T *buf, *buf2;
  int offs;
  int item_count;

  bool have_buf2;

 public:

  consumer2() {
    buf = (T*)malloc(CONSUMER_BUFFER_SIZE * sizeof(T));
    buf2 = (T*)malloc(CONSUMER_BUFFER_SIZE * sizeof(T));
    offs = CONSUMER_BUFFER_SIZE;
    have_buf2 = false;
    item_count = 0;
  }

  virtual void write_object(object_write_buffer *) {}
  virtual void read_object(object_write_buffer *) {}

  inline void pop_items(T *data, int num) {
    for (int i = 0; i < num; i++) data[i] = pop();
  }
  
  inline T pop() {
    
    item_count++;

    if (offs == CONSUMER_BUFFER_SIZE) {

      if (have_buf2) {

	T *tmp = buf;
	buf = buf2;
	buf2 = tmp;
	offs = 0;
	have_buf2 = false;
      } else {

	sock->read_chunk((char*)buf, CONSUMER_BUFFER_SIZE * sizeof(T));
	offs = 0;
      }
    }
    
    return buf[offs++];
  }

  inline T peek(int index) {
    
    index += offs;
    if (index < CONSUMER_BUFFER_SIZE) {
      
      return buf[index];
    } else {

      if (have_buf2) {

	return buf2[index - CONSUMER_BUFFER_SIZE];
      } else {

	sock->read_chunk((char*)buf2, CONSUMER_BUFFER_SIZE * sizeof(T));
	have_buf2 = true;
	return buf2[index - CONSUMER_BUFFER_SIZE];
      }
    }
  }

};

#endif
