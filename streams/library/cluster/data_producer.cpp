
#include <data_producer.h>

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>

extern int __out_data_buffer;

data_producer::data_producer() {
  items_sent = 0;
  data_buffer = (char*)malloc(2000);
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

  if (__out_data_buffer == 0) {

    sock->write_chunk(ptr, size);

  } else {

    while (buf_offset + size >= __out_data_buffer) {
      int fits = __out_data_buffer - buf_offset;
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

void data_producer::write_item(void *data, int size) {
  write_chunk(data, size, 1); 
}

void data_producer::write_int(int data) {
  write_chunk(&data, sizeof(int), 1); 
}

void data_producer::write_float(float data) {
  write_chunk(&data, sizeof(float), 1); 
}
