
#ifndef __SOCKET_HOLDER_H
#define __SOCKET_HOLDER_H

#include <mysocket.h>

class socket_holder {

 protected:
  mysocket *sock;
  
  // true if sock is memsocket, flase if it is netsocket.
  bool is_mem_socket; 
  
 public:
  socket_holder() { sock = NULL; }

  mysocket *get_socket() { 
    return sock; 
  }

  void set_socket(mysocket *sock) { 
    this->sock = sock; 
    is_mem_socket = sock->is_mem_socket();
  }

  void delete_socket_obj() {
    if (!is_mem_socket) delete sock;
  }
  
};

#endif
