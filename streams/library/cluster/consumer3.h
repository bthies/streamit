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
#ifndef __CONSUMER3_H
#define __CONSUMER3_H

#include <assert.h>

#include <socket_holder.h>
#include <serializable.h>
#include <netsocket.h>
#include <memsocket.h>

#define CONSUMER_BUFFER_SIZE 21000

template <class T>
class consumer3 : public socket_holder, public serializable {

  T *buf;
  int offs;
  int item_size;
  int item_count;

  int frame_id;
  int last_frame_id;

  // wrapover backup!!
  
  T *buf2;
  int backup_size;

 public:

  consumer3() {
    buf = NULL;
    offs = 0; //CONSUMER_BUFFER_SIZE;
    item_size = sizeof(T);
    item_count = 0;
    backup_size = 0;
    frame_id = -1;
  }


  void init() {
#ifndef ARM

    buf = NULL;
    if (is_mem_socket) {
      
      assert(1 == 0);

      ((memsocket*)sock)->set_buffer_size(CONSUMER_BUFFER_SIZE * sizeof(T));
      
    } else {
      
      buf =  (T*)malloc(CONSUMER_BUFFER_SIZE * sizeof(T));
      buf2 =  (T*)malloc(1000 * sizeof(T));
      
    }
#endif //ARM
  }

  void read_frame() {
#ifndef ARM


   
    if (is_mem_socket) {

      assert(1 == 0);

      if (buf != NULL) ((memsocket*)sock)->release_buffer(buf);

      //while (((memsocket*)sock)->queue_empty()) {
      //  ((memsocket*)sock)->wait_for_data();
      //}

      buf = (T*)((memsocket*)sock)->pop_buffer();
      offs = 0;
      
    } else {

      //printf("consumer3::read_frame() leftover is: %d\n", item_count - offs);

      for (int y = offs; y < item_count; y++) {
	buf[y - offs] = buf[y];
      }

      item_count -= offs;

      last_frame_id = ((netsocket*)sock)->read_int();
      

      if (last_frame_id == -1) {

	// do nothing !!

      } else if (last_frame_id > frame_id) {

	frame_id = last_frame_id;
	assert(item_count <= 1000);
	for (int y = 0; y < item_count; y++) buf2[y] = buf[y];
	backup_size = item_count;

      } else {

	assert(last_frame_id == frame_id);

	for (int y = 0; y < item_count; y++) buf[y] = buf2[y];
	item_count = backup_size;

	//printf("CONSUMER3 ==========> restored %d items\n", item_count); 

	//assert(item_count == 0);
      }

      int n = ((netsocket*)sock)->read_int();
      ((netsocket*)sock)->read_chunk((char*)&buf[item_count], 
				     n * sizeof(T));
      
      item_count += n;
      offs = 0;

      //printf("consumer3-read-frame id: %d size: %d\n",
      //	     frame_id, n);


    }


#endif //ARM
  }

  int get_size() { 
    //printf("item-size: %d offs: %d\n", item_count, offs);
    return item_count - offs; 
  }

  int rollback(int size) {
    if (offs >= size) {
      offs -= size;
      return 0;
    }
    printf("consumer3: invalid rollback!\n");
    assert(1 == 0);
  }
  
  int get_frame_id() { return last_frame_id; }

  virtual void write_object(object_write_buffer *) {}
  virtual void read_object(object_write_buffer *) {}

  inline void pop_items(T *data, int num) {
    for (int i = 0; i < num; i++, offs++) data[i] = buf[offs];
  }
  
  inline T pop() {
    return buf[offs++];
  }

  inline T peek(int index) {
    return buf[offs + index];
  }

};

#endif

