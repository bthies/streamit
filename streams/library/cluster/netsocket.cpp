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

#include <netsocket.h>

#ifdef ARM

unsigned get_myip() { return 0; }
unsigned lookup_ip(const char *hostname) { return 0; }
void print_ip(FILE *f, unsigned ip) {}
netsocket::netsocket(int s) { fd = s; }
void netsocket::close() { fd = -1; }
void netsocket::set_item_size(int size) {}
int netsocket::eof() { if (fd == -1) return -1; return 0; }
int netsocket::get_fd() { return fd; }
bool netsocket::data_available() { return false; }
int netsocket::write_OOB(char val) { return -1; }
int netsocket::check_OOB(char *val) { return -1; }
int netsocket::write_chunk(char *buf, int len) { return -1; }
int netsocket::read_chunk(char *buf, int len) { return -1; }

#else  //ARM

#include <sys/time.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <ctype.h>
#include <unistd.h>
#include <strings.h>
#include <stdlib.h>
#include <fcntl.h>
#include <errno.h>
#include <stdio.h>
#include <sys/poll.h>

unsigned get_myip() {

  char me[1024];
  FILE *p = popen("uname -n", "r");
  fscanf(p, "%s", me);
  pclose(p);

  return lookup_ip(me);
}

unsigned lookup_ip(const char *hostname) {

  struct hostent *host = gethostbyname(hostname);
  if (host == NULL) {
    fprintf(stderr, "ERROR unable to lookup ip address of host %s\n", hostname);
  }
  return *(unsigned *)host->h_addr_list[0];
}

void print_ip(FILE *f, unsigned ip) {

  fprintf(f, "%d.%d.%d.%d", 
	  (ip % 256), 
	  ((ip>>8) % 256), 
	  ((ip>>16) % 256), 
	  ((ip>>24) % 256));
}


netsocket::netsocket(int s) {
  fd = s;
}


void netsocket::close() {
  ::close(fd);
  fd = -1;
}

void netsocket::set_item_size(int size) {
}

int netsocket::eof() {
  if (fd == -1) return -1;
  return 0;
}

int netsocket::get_fd() {
  return fd;
}


bool netsocket::data_available() {

  if (fd == -1) return false;

  struct pollfd pfd;
  pfd.fd = fd;
  pfd.events = POLLIN;
  pfd.revents = 0;
  if (poll(&pfd, 1, 0) > 0 && pfd.revents == POLLIN) 
    return true; 
  else 
    return false;

}


int netsocket::write_OOB(char val) {
  int retval;

  if (fd == -1) return -1;

  retval = send(fd, &val, 1, MSG_OOB);
  if (retval == 1) return 0;
  return -1;
}

int netsocket::check_OOB(char *val) {
  int flag;

  if (fd == -1) return 0;

  if (ioctl(fd, SIOCATMARK, &flag) == -1) { perror("ioctl"); return 0; }
  if (flag) {
    if (recv(fd, &flag, 1, MSG_OOB) == -1) { perror("recv"); return 0; }
    *val = flag;
    return 1;
  }
  return 0;
}



int netsocket::write_chunk(char *buf, int len) {

  if (fd == -1) return -1;

  /////////////////////////////////
  // initial attempt to write data

  int retval;

  retval = write(fd, buf, len);

  if (retval == len) return 0;

  if (retval == -1 && errno == EPIPE) return 0;

  /////////////////////////////////
  // if initial attempt failed try again
  
  int done;

  if (retval > 0) done = retval; else done = 0;
  
  fd_set set;
  struct timeval rwait;

  for (;;) {
  
    FD_ZERO(&set);
    FD_SET(fd, &set);
    
    rwait.tv_sec = 0;
    rwait.tv_usec = 100000; // 1/10th of a second
    
    int select_retval = select(fd + 1, NULL, &set, NULL, &rwait);

    if (select_retval == 0) {

      if (check_thread_fptr != NULL) check_thread_fptr();    
    }

    if (select_retval > 0) {

       int res = write(fd, buf + done, len - done);
 
       if (retval == -1 && errno == EPIPE) return 0;
     
       if (res > 0) done += res;
    }

    if (done >= len) break;
  }

  return 0;

}


int netsocket::read_chunk(char *buf, int len) {

  if (fd == -1) return -1;

  /////////////////////////////////
  // initial attempt to read data

  int retval;

  retval = read(fd, buf, len);

  if (retval == 0) {
    close(); 
    return -1;
  }

  if (retval == len) return 0;

  ////////////////////////////////
  // if initial attempt failed try again

  int done;

  if (retval > 0) done = retval; else done = 0;

  fd_set set;
  struct timeval rwait;

  for (;;) {
  
    FD_ZERO(&set);
    FD_SET(fd, &set);
    
    rwait.tv_sec = 0;
    rwait.tv_usec = 100000; // 1/10th of a second

    int select_retval = select(fd + 1, &set, NULL, NULL, &rwait);

    if (select_retval == 0) {
      if (check_thread_fptr != NULL) check_thread_fptr(); 
    
    }
    
    if (select_retval > 0) {
      
      //fprintf(stderr,"read_chunk :: select returns true\n");

      retval = read(fd, buf + done, len - done);

      if (retval == 0) {
	close(); 
	return -1;
      }

      if (retval > 0) done += retval;
    }

    if (done >= len) break;
  }

  return 0;

}

#endif //ARM
