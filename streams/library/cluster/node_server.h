
#ifndef __NODE_SERVER_H
#define __NODE_SERVER_H

#include <vector>

#include <thread_info.h>

#define LIST_COMMAND 1

// parameters: NONE
// response "1", "2", "-1"

#define PAUSE_COMMAND 2

// parameters: "1"
// response: "0" - OK, "-1" - error

#define RESTART_COMMAND 3

// parameters: "1"
// response: "0" - OK, "-1" - error


class node_server {

  vector <thread_info*> thread_list;

 public:
  
  node_server(vector <thread_info*> list);

  void run_server();

};


#endif
