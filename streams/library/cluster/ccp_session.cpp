
#include <ccp_session.h>

ccp_session::ccp_session(unsigned ip, mysocket *sock) {
  this->ip = ip;
  this->sock = sock;
}

mysocket *ccp_session::get_socket() {
  return sock;
}

unsigned ccp_session::get_ip() {
  return ip;
}

int ccp_session::read_data() {
  
  char c;
  int retval = sock->read_chunk(&c, 1);

  if (retval == -1) {
    printf("Socket closed !!\n");
    return -1;
  } else {
    printf("Read data: [%c]\n", c);
    return 0;
  }
}


