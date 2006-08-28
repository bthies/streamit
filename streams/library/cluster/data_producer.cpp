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

#include <data_producer.h>

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>

extern int __out_data_buffer;

data_producer::data_producer() {
  items_sent = 0;
  data_buffer = (char*)malloc(16000);
  buf_offset = 0;
}

void data_producer::write_object(object_write_buffer *buf) {
  buf->write_int(items_sent);
}

void data_producer::read_object(object_write_buffer *buf) {
  items_sent = buf->read_int();
}

void data_producer::write_chunk(void *data, int size, int nitems) {

  char *ptr = (char*)data;
  int fits;

  if (__out_data_buffer == 0) {

    sock->write_chunk(ptr, size);

  } else {

    while (buf_offset + size >= __out_data_buffer) {
      fits = __out_data_buffer - buf_offset;
      memcpy(data_buffer + buf_offset, ptr, fits);
      sock->write_chunk((char*)data_buffer, __out_data_buffer);      
      buf_offset = 0;
      ptr += fits;
      size -= fits;
    }

    if (size > 0) {
      memcpy(data_buffer + buf_offset, ptr, size);
      buf_offset += size;
    }
  }  

  items_sent += nitems;
}

void data_producer::flush() {
  // sends more data than has been written to the buffer, 
  // this is called at the end of execution

  if (buf_offset > 0) {
    sock->write_chunk((char*)data_buffer, __out_data_buffer); 
  }
}

void data_producer::write_item(void *data, int size) {
  write_chunk(data, size, 1); 
}

void data_producer::write_int(int data) {
  write_chunk(&data, sizeof(int), 1); 
}

void data_producer::write_float(float data) {
  write_chunk(&data, sizeof(float), 1); 
}
