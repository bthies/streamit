
#ifndef __NODE_SERVER_H
#define __NODE_SERVER_H

#include <thread_list_element.h>

class node_server {

  thread_list_element *thread_top;

 public:
  
  node_server(thread_list_element *thread_list_top);

  void run_server();

};


#endif
