
#include <data_producer.h>

data_producer::data_producer() {
  socket = NULL;
  items_sent = 0;
  buf_size = 0;
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

void data_producer::write_item(void *buf, int size) {

  /*
  socket->write_chunk((char*)buf, size);
  items_sent++;
  */

  memcpy( data_buf + buf_size , buf, size);
  buf_size += size;
  items_sent++;

  if (buf_size >= 100) {
    socket->write_chunk(this->data_buf, buf_size);
    buf_size = 0;
  }
  
  
}


void data_producer::write_int(int data) {
  write_item(&data, sizeof(int)); 
}

void data_producer::write_float(float data) {
  write_item(&data, sizeof(float)); 
}
