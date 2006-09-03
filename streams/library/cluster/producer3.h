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
#ifndef __PRODUCER3_H
#define __PRODUCER3_H

#include<assert.h>
#include <socket_holder.h>
#include <serializable.h>
#include <netsocket.h>
#include <memsocket.h>

#define PRODUCER_BUFFER_SIZE 10000

template <class T>
class producer3 : public socket_holder, public serializable {

  T *buf;
  int offs;
  int item_size;
  int item_count;

  int frame_id;

 public:

  producer3() {
    buf = NULL;
    offs = 0;
    frame_id = 0;
    item_size = sizeof(T);
    item_count = 0;
  }


  void inc_frame() { frame_id++; }
  void dec_frame() { frame_id--; }

  int get_frame() { return frame_id; }
  inline int get_size() { return offs; }



  int rollback(int size) {
    if (offs >= size) {
      offs -= size;
      return 0;
    }
    printf("producer3: invalid rollback!\n");
    assert(1 == 0);
  }


  void init() {
#ifndef ARM
    
    if (is_mem_socket) {

      ((memsocket*)sock)->set_buffer_size(PRODUCER_BUFFER_SIZE*sizeof(T));
      buf = (T*)((memsocket*)sock)->get_free_buffer();
      
    } else {
      
      buf = (T*)malloc(PRODUCER_BUFFER_SIZE*sizeof(T));
      offs = 0;

      
    }
#endif //ARM
  }


  void send_buffer() {
#ifndef ARM
    if (is_mem_socket) {
      
      //while (((memsocket*)sock)->queue_full()) {
      //  ((memsocket*)sock)->wait_for_space();
      //}
      
      ((memsocket*)sock)->push_buffer(buf);
      buf = (T*)((memsocket*)sock)->get_free_buffer();
      offs = 0;
      
    } else {
      
      //printf("producer3 ===== id %d offs %d\n", frame_id, offs); 

      ((netsocket*)sock)->write_int(frame_id);
      ((netsocket*)sock)->write_int(offs);
      ((netsocket*)sock)->write_chunk((char*)buf, 
				      offs*sizeof(T));
      offs = 0;
      
    }
#endif //ARM
  }


  virtual void write_object(object_write_buffer *) {}
  virtual void read_object(object_write_buffer *) {}


  inline void push_items(T *data, int num) {
    for (int i = 0; i < num; i++, offs++) buf[offs] = data[i];
  }    

  inline void push(T data) {
    buf[offs++] = data;    
  }

  inline void flush() {
    //printf("producer3-flush frame: %d size: %d\n", frame_id, offs);     
    if (offs > 0) send_buffer();
  }

  inline void flush_pseudo() {
    //printf("producer3-flush frame: %d size: %d\n", frame_id, offs);     
    int id = frame_id;
    frame_id = -1;
    if (offs > 0) send_buffer();
    frame_id = id;
  }

};

#endif
