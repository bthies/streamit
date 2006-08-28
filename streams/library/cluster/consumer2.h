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
#ifndef __CONSUMER2_H
#define __CONSUMER2_H

#include <init_instance.h>
#include <socket_holder.h>
#include <serializable.h>
#include <netsocket.h>
#include <memsocket.h>

template <class T>
class consumer2 : public socket_holder, public serializable {

  T *buf;
  int offs;
  int item_size;
  int item_count;

 public:

  consumer2() {
    buf = NULL;

#ifdef CONSUMER_BUFFER_SIZE
    offs = CONSUMER_BUFFER_SIZE;
#else
    offs = 0;
#endif

    item_size = sizeof(T);
    item_count = 0;
  }


  void init() {
#ifndef ARM

    buf = NULL;

#ifdef CONSUMER_BUFFER_SIZE
    if (is_mem_socket) {
      
      ((memsocket*)sock)->set_buffer_size(CONSUMER_BUFFER_SIZE * sizeof(T));
      
    } else {
      
      buf =  (T*)malloc(CONSUMER_BUFFER_SIZE * sizeof(T));
      
    }
#endif

#endif //ARM
  }

  void recv_buffer() {
#ifndef ARM
#ifdef CONSUMER_BUFFER_SIZE
   
    if (is_mem_socket) {

      if (buf != NULL) ((memsocket*)sock)->release_buffer(buf);

      //while (((memsocket*)sock)->queue_empty()) {
      //  ((memsocket*)sock)->wait_for_data();
      //}

      buf = (T*)((memsocket*)sock)->pop_buffer();
      offs = 0;
      
    } else {

      ((netsocket*)sock)->read_chunk((char*)buf, 
				     CONSUMER_BUFFER_SIZE * sizeof(T));
      offs = 0;
    }
#endif
#endif //ARM
  }

  virtual void write_object(object_write_buffer *) {}
  virtual void read_object(object_write_buffer *) {}

  inline void pop_items(T *data, int num) {


#ifndef CONSUMER_BUFFER_SIZE

    ((netsocket*)sock)->read_chunk((char*)data, sizeof(T)*num);

#else

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

#endif

  }
  
  inline T pop() {

#ifndef CONSUMER_BUFFER_SIZE

    T tmp;
    ((netsocket*)sock)->read_chunk((char*)&tmp, sizeof(T));
    return tmp;

#else

    //item_count++;

    if (offs == CONSUMER_BUFFER_SIZE) {
      recv_buffer();
    }
    
    return buf[offs++];

#endif
  }

  inline void peek(int index) {
    
#ifdef CONSUMER_BUFFER_SIZE
    if (offs == CONSUMER_BUFFER_SIZE) {
      recv_buffer();
    }
#endif
    
    return;
  }

};

#endif
