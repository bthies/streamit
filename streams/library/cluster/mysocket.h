
#ifndef __MYSOCKET_H
#define __MYSOCKET_H

#include <stdio.h>

extern unsigned get_myip();
extern unsigned lookup_ip(char *host);
extern void print_ip(FILE *f, unsigned ip);

class mysocket {

 private:
  int fd;

 protected:
  void set_socket(int s) { fd = s; }

 public:

  mysocket() {}
  mysocket(int s);
  int read_chunk(char *buf, int len);  
  int write_chunk(char *buf, int len);
  int get_fd();

  int read_int();
  void write_int(int);

  double read_double();
  void write_double(double);

  float read_float();
  void write_float(float);

};

#endif

