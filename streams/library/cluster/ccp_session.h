
#ifndef __CCP_SESSION_H
#define __CCP_SESSION_H

#include <mysocket.h>

class ccp_session {

  unsigned ip;

  mysocket *sock;

 public:

  ccp_session(unsigned ip, mysocket *sock);

  mysocket *get_socket();
  unsigned get_ip();

  int read_data();

};



#endif
