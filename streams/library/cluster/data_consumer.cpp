
#include <data_consumer.h>

data_consumer::data_consumer() {
  socket = NULL;
  items_read = 0;
}

void data_consumer::write_object(object_write_buffer *buf) {
  buf->write_int(items_read);
}

void data_consumer::read_object(object_write_buffer *buf) {
  items_read = buf->read_int();
}


mysocket *data_consumer::get_socket() {
  return socket;
}

void data_consumer::set_socket(mysocket *socket) {
  this->socket = socket;
}

void data_consumer::read_item(void *buf, int size) {  
  int retval;

  do {
    retval = socket->read_chunk((char*)buf, size);

    if (retval == -1) {
      printf("data_consumer: could not read data!");
      fflush(stdout);
      socket->check_thread_status();
      sleep(1);
    }
  } while (retval == -1);
    
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








