
#ifndef __INIT_INSTANCE_H
#define __INIT_INSTANCE_H

#include <mysocket.h>
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
  static map<sock_dscr, mysocket*> in_sockets, out_sockets;
  static map<sock_dscr, bool> in_done, out_done;

  static map<int, unsigned> thread_machines;
  static map<int, unsigned> thread_start_iter;

  //// helpers

  static int listen();

  static pthread_mutex_t accept_lock;
  static pthread_mutex_t bind_lock;
  
  friend void *accept_thread(void *param);

 public:

  static void reset_all();

  static void read_config_file();

  static void set_thread_ip(int thread, unsigned ip); 
  static unsigned get_thread_ip(int thread);

  static void set_thread_start_iter(int thread, unsigned iter); 
  static unsigned get_thread_start_iter(int thread);

  static void add_incoming(int from, int to, int type);
  static void add_outgoing(int from, int to, int type);
  
  static void initialize_sockets();
  static void close_sockets();
  
  static mysocket* get_incoming_socket(int from, int to, int type);
  static mysocket* get_outgoing_socket(int from, int to, int type);


};

#endif
