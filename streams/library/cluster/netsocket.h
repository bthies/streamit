
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
  
};

#endif

