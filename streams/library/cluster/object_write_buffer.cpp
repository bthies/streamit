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

#include <object_write_buffer.h>

#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>

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



//================================= read and write

void object_write_buffer::write(void *data, int dsize) {

  if (this->size + dsize > OWB_BUFFER_SIZE) {
    fprintf(stderr, "object_write_buffer::write - buffer overflow!\n");
    fflush(stderr);
    fflush(stdout);
    exit(1);
  }
  
  memcpy(buf + this->size, data, dsize);
  this->size += dsize;
}


void object_write_buffer::read(void *data, int dsize) {

  if (read_offset + dsize > OWB_BUFFER_SIZE) {
    fprintf(stderr, "object_write_buffer::read - buffer overflow!\n");
    fflush(stderr);
    fflush(stdout);
    exit(1);
  }

  memcpy(data, buf + read_offset, dsize);
  read_offset += dsize;
}

//================================= convenience functinos

void object_write_buffer::write_int(int data) {
  write(&data, sizeof(int));
}

void object_write_buffer::write_float(float data) {
  write(&data, sizeof(float));
}

int object_write_buffer::read_int() {
  int result;
  read(&result, sizeof(int));
  return result;
}

float object_write_buffer::read_float() {
  float result;
  read(&result, sizeof(float));
  return result;
}
