
#ifndef __INIT_INSTANCE_H
#define __INIT_INSTANCE_H

#include <sock_dscr.h>

#include <map>
#include <vector>
#include <string>

using namespace std;

#define DATA_SOCKET 1
#define MESSAGE_SOCKET 2

#define LOCK(var)   pthread_mutex_lock(var)
#define UNLOCK(var) pthread_mutex_unlock(var)

class init_instance {

  static short listen_port;
  
  static vector<sock_dscr> in_connections, out_connections;
  static map<sock_dscr, int> in_sockets, out_sockets;
  static map<sock_dscr, bool> in_done, out_done;

  static map<int, string> thread_machines;

 public:

  static void read_config_file();
  static char* get_node_name(int node);

  static void add_incoming(int from, int to, int type);
  static void add_outgoing(int from, int to, int type);
  
  static void initialize_sockets();
  static void close_sockets();
  
  static int get_incoming_socket(int from, int to, int type);
  static int get_outgoing_socket(int from, int to, int type);


  //// helpers

  static int listen();

  static pthread_mutex_t accept_lock;
  static pthread_mutex_t bind_lock;

};

#endif
