
#ifndef __CCP_SESSION_H
#define __CCP_SESSION_H

#include <sys/time.h>

#include <mysocket.h>
#include <node_server.h>

class ccp_session {

  unsigned ip;
  mysocket *sock;

  timeval last_alive_request;
  bool alive_cmd_sent;
  bool alive_response_received;
  
  bool extended_alive_limit;

 public:

  ccp_session(unsigned ip, mysocket *sock);

  mysocket *get_socket();
  unsigned get_ip();

  int read_data();

  bool is_alive();
  void extend_alive_limit(); // temporarily extends alive limit
};



#endif
