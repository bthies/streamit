
#ifndef __INIT_INSTANCE_H
#define __INIT_INSTANCE_H

#include <int_pair.h>

#include <map>
#include <vector>
#include <string>

using namespace std;

#define LOCK(var)   pthread_mutex_lock(var)
#define UNLOCK(var) pthread_mutex_unlock(var)

class init_instance {

  static short listen_port;
  
  static map<int_pair, int> in_sockets;
  static map<int_pair, int> out_sockets;

  static map<int_pair, bool> in_done;

  static vector<int_pair> in_connections;

  static vector<int_pair> out_connections;
  static vector<unsigned> out_ip_addrs;

  static map<int, string> thread_machines;

 public:

  static void read_config_file();
  static char* get_node_name(int node);

  static void add_incoming(int from, int to);
  static void add_outgoing(int from, int to, unsigned to_ip_addr);

  static void initialize_sockets();
  static void close_sockets();
  
  static int get_incoming_socket(int from , int to);
  static int get_outgoing_socket(int from , int to);


  //// helpers

  static int listen();

  static pthread_mutex_t accept_lock;
  static pthread_mutex_t bind_lock;

};

#endif
