
#include <data_consumer.h>

data_consumer::data_consumer() {
  socket = NULL;
  items_read = 0;
}

void data_consumer::write_object(object_write_buffer *buf) {
  buf->write_int(items_read);
}


mysocket *data_consumer::get_socket() {
  return socket;
}

void data_consumer::set_socket(mysocket *socket) {
  this->socket = socket;
}

void data_consumer::read_item(void *buf, int size) {  
  socket->read_chunk((char*)buf, size);
  items_read++;
}

int data_consumer::read_int() {
  int result;
  read_item(&result, sizeof(int));
  return result;
}

float data_consumer::read_float() {
  float result;
  read_item(&result, sizeof(float));
  return result;
}
