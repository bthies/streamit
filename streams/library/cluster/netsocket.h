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

#ifndef __NETSOCKET_H
#define __NETSOCKET_H

#include <mysocket.h>

#include <stdio.h>
#include <sys/poll.h>

extern unsigned get_myip();
extern unsigned lookup_ip(const char *host);
extern void print_ip(FILE *f, unsigned ip);

class netsocket : public mysocket {

 private:
  int fd;

 protected:
  void set_socket(int s) { fd = s; }

 public:

  netsocket(int s);

  virtual void set_item_size(int size);
  virtual int eof();
  virtual void close();  
  virtual bool data_available();
  virtual int get_fd();
  virtual int read_chunk(char *buf, int len);  
  virtual int write_chunk(char *buf, int len);

  int write_OOB(char val);
  int check_OOB(char *val);


  virtual void set_buffer_size(int size) {}
  virtual bool is_mem_socket() { return false; }
  virtual bool is_net_socket() { return true; }

  int read_int() {
    int a;
    read_chunk((char*)&a, sizeof(int));
    return a;
  }

  void read_int_array(int* dst, int length) {
    read_chunk((char*)dst, length*sizeof(int));
  }

  void write_int(int a) {
    write_chunk((char*)&a, sizeof(int));
    return;
  }

  void write_int_array(int* src, int length) {
    write_chunk((char*)src, length*sizeof(int));
    return;
  }

  double read_double() {
    double a;
    read_chunk((char*)&a, sizeof(double));
    return a;
  }

  void write_double(double a) {
    write_chunk((char*)&a, sizeof(double));
    return;
  }

  float read_float() {
    float a;
    read_chunk((char*)&a, sizeof(float));
    return a;
  }
  
  void read_float_array(float* dst, int length) {
    read_chunk((char*)dst, length*sizeof(float));
  }

  void write_float(float a) {
    write_chunk((char*)&a, sizeof(float));
    return;
  }

  void write_float_array(float* src, int length) {
    write_chunk((char*)src, length*sizeof(float));
    return;
  }
  
};

#endif
