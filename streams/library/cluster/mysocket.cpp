
#include <mysocket.h>

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
#include <stdio.h>
#include <sys/poll.h>

int mysocket::total_data_received = 0;
int mysocket::total_data_sent = 0;

int mysocket::get_total_data_received() {
  return total_data_received;
}

int mysocket::get_total_data_sent() {
  return total_data_sent;
}

unsigned get_myip() {

  char me[1024];
  FILE *p = popen("uname -n", "r");
  fscanf(p, "%s", me);
  pclose(p);

  return lookup_ip(me);

}

unsigned lookup_ip(const char *hostname) {

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


mysocket::mysocket(int s, void (*check_thread_status_during_io)() = NULL) {
  fd = s;
  check_thread_fptr = check_thread_status_during_io;
}


void mysocket::close() {
  ::close(fd);
  fd = -1;
}

int mysocket::eof() {
  if (fd == -1) return -1;
  return 0;
}

int mysocket::get_fd() {
  return fd;
}


bool mysocket::data_available() {

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


int mysocket::write_OOB(char val) {
  int retval;

  if (fd == -1) return -1;

  retval = send(fd, &val, 1, MSG_OOB);
  if (retval == 1) return 0;
  return -1;
}

int mysocket::check_OOB(char *val) {
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



int mysocket::write_chunk(char *buf, int len) {

  if (fd == -1) return -1;

  total_data_sent += len;

  /////////////////////////////////
  // initial attempt to write data

  int retval;

  retval = write(fd, buf, len);

  if (retval == len) return 0;

  total_data_sent -= len;

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

      if (res > 0) done += res;
    }

    if (done >= len) break;
  }

  total_data_sent += len;

  return 0;

}


int mysocket::read_chunk(char *buf, int len) {

  if (fd == -1) return -1;

  total_data_received += len;

  /////////////////////////////////
  // initial attempt to read data

  int retval;

  retval = read(fd, buf, len);

  if (retval == 0) {
    close(); 
    return -1;
  }

  if (retval == len) return 0;

  total_data_received -= len;

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
      
      //printf("read_chunk :: select returns true\n");

      retval = read(fd, buf + done, len - done);

      if (retval == 0) {
	close(); 
	return -1;
      }

      if (retval > 0) done += retval;
    }

    if (done >= len) break;
  }

  total_data_received += len;

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

double mysocket::read_double() {
  double a;
  read_chunk((char*)&a, sizeof(a));
  return a;
}

void mysocket::write_double(double a) {
  write_chunk((char*)&a, sizeof(a));
  return;
}

float mysocket::read_float() {
  float a;
  read_chunk((char*)&a, sizeof(a));
  return a;
}

void mysocket::write_float(float a) {
  write_chunk((char*)&a, sizeof(a));
  return;
}

