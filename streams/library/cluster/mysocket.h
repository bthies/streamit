
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

