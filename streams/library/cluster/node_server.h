
#ifndef __NODE_SERVER_H
#define __NODE_SERVER_H

#include <vector>

#include <thread_info.h>

#define LIST_COMMAND 1

// parameters: none
// sample response "1", "2", "-1"

#define PAUSE_COMMAND 2

// parameters: <thread_id>
// sample response: "0" - OK, "-1" - error

#define RESTART_COMMAND 3

// parameters: <thread_id>
// sample response: "0" - OK, "-1" - error

#define LIST_INCOMING_DATA_LINKS 4

// parameters: <thread_id>
// sample response "1", "2", "-1"

#define LIST_OUTGOING_DATA_LINKS 5

// parameters: <thread_id>
// sample response "1", "2", "-1"

class node_server {

  vector <thread_info*> thread_list;

  vector<int> list();
  vector<int> pause(int id);
  vector<int> restart(int id);
  vector<int> list_incoming_data_links(int id);
  vector<int> list_outgoing_data_links(int id);

 public:
  
  node_server(vector <thread_info*> list);

  void run_server();

};


#endif




