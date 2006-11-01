/*
 * Copyright 2006 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

#ifndef __INIT_INSTANCE_H
#define __INIT_INSTANCE_H

#include <mysocket.h>
#include <sock_dscr.h>

#include <map>
#include <vector>
#include <string>

#define CONSUMER_BUFFER_SIZE 10000
#define PRODUCER_BUFFER_SIZE 10000

using namespace std;

#define DATA_SOCKET 1
#define MESSAGE_SOCKET 2

#define LOCK(var)   pthread_mutex_lock(var)
#define UNLOCK(var) pthread_mutex_unlock(var)

class init_instance {
public:
  typedef struct {
     int t_usage;
     int t_host_ip;
  } Thread_Info;

  typedef map<int, Thread_Info> Thread_info;
private:
  
  static short listen_port;
  static int start_iter;
  
  static vector<sock_dscr> in_connections, out_connections;
  static map<sock_dscr, mysocket*> in_sockets, out_sockets;
  static map<sock_dscr, bool> in_done, out_done;

  static map<int, unsigned> thread_machines;
  static map<int, unsigned> thread_start_iter;

  static Thread_info threadInfo;
  //// helpers

  static int listen();

  static pthread_mutex_t accept_lock;
  static pthread_mutex_t bind_lock;
  
  friend void *accept_thread(void *param);

  
 public:

  static void reset_all();

  static void set_start_iter(int iter);
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

  static void set_thread_usage(int thread, int usage);
  static int get_thread_usage(int thread);
  static Thread_info return_thread_map();
};

#endif
