
#ifndef __NODE_SERVER_H
#define __NODE_SERVER_H

#include <mysocket.h>
#include <open_socket.h>
#include <thread_info.h>
#include <init_instance.h>

#include <vector>
#include <unistd.h>

#define LIST_COMMAND 10

// parameters: none
// sample response "1", "2", "-1"

#define PAUSE_PROPER_COMMAND 20 // pause between iterations

// parameters: <thread_id>
// sample response: "0" - OK, "-1" - error

#define PAUSE_ANY_COMMAND 21 // pause between iterations or during I/O

// parameters: <thread_id>
// sample response: "0" - pasued between iterations
//                  "1" - paused during I/O operation
//                  "-1" - error

#define RESUME_COMMAND 30

// parameters: <thread_id>
// sample response: "0" - OK, "-1" - error

#define LIST_INCOMING_DATA_LINKS 40

// parameters: <thread_id>
// sample response "1", "2", "-1"

#define LIST_OUTGOING_DATA_LINKS 50

// parameters: <thread_id>
// sample response "1", "2", "-1"

#define CLUSTER_CONFIG 60

// params: <# of threads> 
// <thread id>  <ip address>  <iteration>  (* number of threads)
// response - none

#define ALIVE_COMMAND 70

// params: none
// response: max checkcpoint iteration (>0)

#define STOP_ALL_THREADS 80

// paramns: none
// response: 1 - ok


class node_server {

  vector <thread_info*> thread_list;

  void (*thread_init)();

  vector<int> list();
  vector<int> pause_proper(int id);
  vector<int> pause_any(int id);
  vector<int> resume(int id);
  vector<int> list_incoming_data_links(int id);
  vector<int> list_outgoing_data_links(int id);
  vector<int> stop_all();

  mysocket *wait_for_connection();
  static mysocket *connect_to_ccp(unsigned ip);
  void run_server(mysocket *sock);

  static int read_cluster_config(mysocket *sock);

 public:
  
  node_server(vector <thread_info*> list, void (*thread_init)() = NULL);
  
  void run(unsigned ccp_ip);
  
};


#endif




