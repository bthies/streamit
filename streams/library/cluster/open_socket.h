
#ifndef __OPEN_SOCKET_H
#define __OPEN_SOCKET_H

#include <netsocket.h>

class open_socket {

 public:
  static netsocket *listen(short port);
  static netsocket *connect(unsigned ipaddr, short port);

};


#endif
