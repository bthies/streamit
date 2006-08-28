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

#ifndef __MYSOCKET_H
#define __MYSOCKET_H

#include <stdlib.h>

class mysocket {

 protected:

  void (*check_thread_fptr)();

 public:

  mysocket();

  void set_check_thread_status(void (*check_thread_status_during_io)());

  void check_thread_status();

  // virtual functions

  virtual void set_buffer_size(int size) = 0;
  virtual bool is_mem_socket() = 0;
  virtual bool is_net_socket() = 0;
  
  virtual void close() = 0;

  /*
  virtual int eof() = 0;
  virtual void close() = 0;
  virtual bool data_available() = 0;
  virtual int get_fd() = 0;
  virtual int read_chunk(char *buf, int len) = 0;  
  virtual int write_chunk(char *buf, int len) = 0;

  int read_int();
  void write_int(int);

  double read_double();
  void write_double(double);

  float read_float();
  void write_float(float);
  */
  
};

#endif

