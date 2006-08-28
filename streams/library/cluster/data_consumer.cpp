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

#include <data_consumer.h>

#include <unistd.h>
#include <stdio.h>
#include <string.h>

extern int __out_data_buffer;

data_consumer::data_consumer() {
  items_read = 0;
  data_buffer = (char*)malloc(16000);
  buf_offset = 16000; // buffer empty
}

void data_consumer::write_object(object_write_buffer *buf) {
  buf->write_int(items_read);
}

void data_consumer::read_object(object_write_buffer *buf) {
  items_read = buf->read_int();
}

void data_consumer::read_chunk(void *buf, int size, int nitems) {  

  char *ptr = (char*)buf;
  int fits;

  if (__out_data_buffer == 0) {
    
    sock->read_chunk(ptr, size);
    
  } else {
  
    while (buf_offset + size > __out_data_buffer) {
      fits = __out_data_buffer - buf_offset;
      if (fits > 0) {
	memcpy(ptr, data_buffer + buf_offset, fits);
	ptr += fits;
	size -= fits;
      }
      sock->read_chunk(data_buffer, __out_data_buffer);
      buf_offset = 0;
    }
    
    if (size > 0) {
      memcpy(ptr, data_buffer + buf_offset, size);
      buf_offset += size;
    }

  }

  /*
				
  do {
    retval = sock->read_chunk((char*)buf, size);

    if (retval == -1) {
      fprintf(stderr,"data_consumer: could not read data!");
      fflush(stderr);
      sock->check_thread_status();
      sleep(1);
    }
  } while (retval == -1);
    
  */

  items_read += nitems;
}

void data_consumer::read_item(void *buf, int size) {  
  read_chunk(buf, size, 1);
}

int data_consumer::read_int() {
  int result;
  read_chunk(&result, sizeof(int), 1);
  return result;
}

float data_consumer::read_float() {
  float result;
  read_chunk(&result, sizeof(float), 1);
  return result;
}








