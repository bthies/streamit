
#ifndef __SOCKET_HOLDER_H
#define __SOCKET_HOLDER_H

#include <mysocket.h>

class socket_holder {

 public:

  virtual mysocket *get_socket() = 0;
  virtual void set_socket(mysocket *) = 0;
};

#endif
