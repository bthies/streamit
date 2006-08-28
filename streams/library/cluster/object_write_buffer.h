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
#ifndef __OBJECT_WRITE_BUFFER_H
#define __OBJECT_WRITE_BUFFER_H

#define OWB_BUFFER_SIZE 1200000    // 1,200,000 bytes

class object_write_buffer {

  char buf[OWB_BUFFER_SIZE];

  int size;
  int read_offset;

 public:

  // =========== constructor

  object_write_buffer();

  // =========== erase, set read_offset, get_size
  
  void erase();
  void set_read_offset(int offset);
  int get_size();
  
  // =========== read and write

  void write(void *ptr, int size);
  void read(void *ptr, int size);

  // =========== convenience functions

  void write_int(int data);
  void write_float(float data);  
  int read_int();
  float read_float();

};

#endif
