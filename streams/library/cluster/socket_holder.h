
#ifndef __SOCKET_HOLDER_H
#define __SOCKET_HOLDER_H

#include <mysocket.h>

class socket_holder {

 protected:
  mysocket *sock;

 public:
  socket_holder() { sock = NULL; }

  mysocket *get_socket() { return sock; }
  void set_socket(mysocket *sock) { this->sock = sock; }
};

#endif
