#ifndef __CCP_H
#define __CCP_H

#include <sys/time.h>
#include <sys/types.h>
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

#include <vector>
#include <map>

#include <mysocket.h>
#include <ccp_session.h>

class ccp {

  vector <ccp_session*> sessions;
  map<int, int> partition;         // from thread to machine id
  map<int, unsigned> machines;     // from machine id to ip address

  int number_of_threads;
  int machines_in_partition;

  int initial_iteration;
  
  void read_config_file();

  void start_execution();

  void handle_extra_node();
  void handle_node_failure();

 public:

  ccp();

  void set_init_iter(int iter);

  int run_ccp();

};


#endif
