
#include <data_producer.h>

extern int __out_data_buffer;

data_producer::data_producer() {
  socket = NULL;
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

mysocket *data_producer::get_socket() {
  return socket;
}

void data_producer::set_socket(mysocket *socket) {
  this->socket = socket;
}

void data_producer::write_item(void *data, int size) {

  if (__out_data_buffer == 0) {

    socket->write_chunk((char*)data, size);
    items_sent++;

  } else {

    items_sent++;

    memcpy(data_buffer + buf_offset, data, size);
    buf_offset += size;

    if (buf_offset >= __out_data_buffer) {

      socket->write_chunk((char*)data_buffer, buf_offset);
      buf_offset = 0;
    }		
  }  
}


void data_producer::write_int(int data) {
  write_item(&data, sizeof(int)); 
}

void data_producer::write_float(float data) {
  write_item(&data, sizeof(float)); 
}
