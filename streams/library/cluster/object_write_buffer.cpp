
#include <object_write_buffer.h>

object_write_buffer::object_write_buffer() {
  size = 0;
  read_offset = 0;
}

void object_write_buffer::erase() {
  size = 0;
}

void object_write_buffer::set_read_offset(int offset) {
  read_offset = offset;
}

int object_write_buffer::get_size() {
  return size;
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

int object_write_buffer::read_int() {
  int result;
  memcpy(&result, buf + read_offset, sizeof(int));
  read_offset+=4;
  return result;
}

float object_write_buffer::read_float() {
  float result;
  memcpy(&result, buf + read_offset, sizeof(float));
  read_offset+=4;
  return result;
}
