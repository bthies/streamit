
#include <mysocket.h>

#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <ctype.h>
#include <unistd.h>
#include <strings.h>
#include <stdlib.h>
#include <fcntl.h>
#include <stdio.h>


unsigned get_myip() {

  char me[1024];
  FILE *p = popen("uname -n", "r");
  fscanf(p, "%s", me);
  pclose(p);

  return lookup_ip(me);

}

unsigned lookup_ip(char *hostname) {

  struct hostent *host = gethostbyname(hostname);
  return *(unsigned *)host->h_addr_list[0];
}

void print_ip(FILE *f, unsigned ip) {

  fprintf(f, "%d.%d.%d.%d", 
	  (ip % 256), 
	  ((ip>>8) % 256), 
	  ((ip>>16) % 256), 
	  ((ip>>24) % 256));
}


mysocket::mysocket(int s) {
  fd = s;
}


int mysocket::write_chunk(char *buf, int len) {


  /////////////////////////////////
  // initial attempt to write data

  int retval;

  retval = write(fd, buf, len);

  if (retval == len) return 0;


  /////////////////////////////////
  // if initial attempt failed try again
  
  int done;

  if (retval > 0) done = retval; else done = 0;
  
  fd_set set;
  struct timeval rwait;

  for (;;) {
  
    FD_ZERO(&set);
    FD_SET(fd, &set);
    
    rwait.tv_sec = 1;
    rwait.tv_usec = 0;
    
    if (select(fd + 1, NULL, &set, NULL, &rwait) > 0) {
      
      int res = write(fd, buf + done, len - done);

      if (res > 0) done += res;
    }

    if (done >= len) break;
  }

  return 0;

}


int mysocket::read_chunk(char *buf, int len) {

  /////////////////////////////////
  // initial attempt to read data

  int retval;

  retval = read(fd, buf, len);

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
    
    rwait.tv_sec = 1;
    rwait.tv_usec = 0;
    
    if (select(fd + 1, &set, NULL, NULL, &rwait) > 0) {
      
      retval = read(fd, buf + done, len - done);

      if (retval > 0) done += retval;
    }

    if (done >= len) break;
  }

  return 0;

}


int mysocket::read_int() {
  int a;
  read_chunk((char*)&a, 4);
  return a;
}

void mysocket::write_int(int a) {
  write_chunk((char*)&a, 4);
  return;
}


int mysocket::get_fd() {
  return fd;
}

