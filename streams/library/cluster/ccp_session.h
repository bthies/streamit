
#ifndef __CCP_SESSION_H
#define __CCP_SESSION_H

#include <sys/time.h>

#include <netsocket.h>
#include <node_server.h>

class ccp_session {

  unsigned ip;
  netsocket *sock;

  timeval last_alive_request;
  bool alive_cmd_sent;
  bool alive_response_received;
  
  int latest_checkpoint;

  int read_int(int *ptr);

 public:

  ccp_session(unsigned ip, netsocket *sock);

  netsocket *get_socket();
  unsigned get_ip();

  int read_data();

  bool is_alive();
  void extend_alive_limit(); // temporarily extends alive limit

  void wait_until_configuration_read(); // waits until node confirms that it has received
                                        // the cluster configuration

  int get_latest_checkpoint();
};



#endif
