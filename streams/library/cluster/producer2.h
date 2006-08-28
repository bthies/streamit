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
#ifndef __PRODUCER2_H
#define __PRODUCER2_H

#include <init_instance.h>
#include <socket_holder.h>
#include <serializable.h>
#include <netsocket.h>
#include <memsocket.h>

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


  void init() {
#ifndef ARM
#ifdef PRODUCER_BUFFER_SIZE
    
    if (is_mem_socket) {

      ((memsocket*)sock)->set_buffer_size(PRODUCER_BUFFER_SIZE*sizeof(T));
      buf = (T*)((memsocket*)sock)->get_free_buffer();
      
    } else {
      
      buf = (T*)malloc(PRODUCER_BUFFER_SIZE*sizeof(T));
      
    }
#endif
#endif //ARM
  }


  void send_buffer() {
#ifndef ARM
#ifdef PRODUCER_BUFFER_SIZE
    if (is_mem_socket) {
      
      //while (((memsocket*)sock)->queue_full()) {
      //  ((memsocket*)sock)->wait_for_space();
      //}
      
      ((memsocket*)sock)->push_buffer(buf);
      buf = (T*)((memsocket*)sock)->get_free_buffer();
      offs = 0;
      
    } else {
      
      ((netsocket*)sock)->write_chunk((char*)buf, 
				      PRODUCER_BUFFER_SIZE*sizeof(T));
      offs = 0;
      
    }
#endif
#endif //ARM
  }


  virtual void write_object(object_write_buffer *) {}
  virtual void read_object(object_write_buffer *) {}

  inline void push_items(T *data, int num) {

#ifndef PRODUCER_BUFFER_SIZE

    //((netsocket*)sock)->write_chunk((char*)data, sizeof(T)*num);

#else

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

#endif
  }

  inline void push(T data) {

#ifndef PRODUCER_BUFFER_SIZE

    ((netsocket*)sock)->write_chunk((char*)&data, sizeof(T));    

#else

    buf[offs++] = data;
    //item_count++;
    
    if (offs == PRODUCER_BUFFER_SIZE) send_buffer();

#endif
  }

  inline void flush() {
    
    if (offs > 0) send_buffer();
  }

};

#endif
