
#include <object_write_buffer.h>

object_write_buffer::object_write_buffer() {
  reset();
}

void object_write_buffer::reset() {
  size = 0;
}

void object_write_buffer::write(void *data, int dsize) {
  memcpy(buf + this->size, data, dsize);
  this->size += dsize;
}

void object_write_buffer::write_int(int data) {
  write(&data, sizeof(int));
}

void object_write_buffer::write_float(float data) {
  write(&data, sizeof(float));
}

int object_write_buffer::get_size() {
  return size;
}

int object_write_buffer::get_data_int(int offset) {
  int result;
  
  memcpy(&result, buf + offset, sizeof(int));

  return result;
}
