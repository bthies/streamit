
#include <data_consumer.h>

#include <unistd.h>
#include <stdio.h>

data_consumer::data_consumer() {
  items_read = 0;
}

void data_consumer::write_object(object_write_buffer *buf) {
  buf->write_int(items_read);
}

void data_consumer::read_object(object_write_buffer *buf) {
  items_read = buf->read_int();
}

void data_consumer::read_chunk(void *buf, int size, int nitems) {  
  int retval;

  do {
    retval = sock->read_chunk((char*)buf, size);

    if (retval == -1) {
      printf("data_consumer: could not read data!");
      fflush(stdout);
      sock->check_thread_status();
      sleep(1);
    }
  } while (retval == -1);
    
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








