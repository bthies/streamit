

#ifndef __OPEN_SOCKET_H
#define __OPEN_SOCKET_H

#include <mysocket.h>

class open_socket : public mysocket {

 public:
  static mysocket *listen(short port);
  static mysocket *connect(unsigned ipaddr, short port);

};


#endif
