
#ifndef __MYSOCKET_H
#define __MYSOCKET_H

#include <stdio.h>
#include <sys/poll.h>

extern unsigned get_myip();
extern unsigned lookup_ip(char *host);
extern void print_ip(FILE *f, unsigned ip);

class mysocket {

 private:
  int fd;

  static int total_data_received;
  static int total_data_sent;

 protected:
  void set_socket(int s) { fd = s; }

 public:

  mysocket() {}
  mysocket(int s);
  
  bool data_available();
  int get_fd();

  static int get_total_data_received();
  static int get_total_data_sent();
  
  int read_chunk(char *buf, int len);  
  int write_chunk(char *buf, int len);

  int read_int();
  void write_int(int);

  double read_double();
  void write_double(double);

  float read_float();
  void write_float(float);

};

#endif

