
#ifndef __MASTER_SERVER_H
#define __MASTER_SERVER_H

#include <string.h>
#include <vector>
#include <map>

#include <mysocket.h>
#include <open_socket.h>
#include <node_server.h>

class node_info {

  unsigned ip;
  mysocket *socket;

 public:
  
  node_info(unsigned ip, mysocket *socket); 

  unsigned get_ip();
  mysocket *get_socket();
};

class master_server {

  vector <node_info*> nodes;
  map <int, node_info*> node_map;

 public:
  
  master_server() {}

  void print_commands();

  node_info *connect(unsigned ip);
  vector<int> *list(node_info *node);

  void pause(node_info *node, int id);
  void restart(node_info *node, int id);

  void process_command(char *cmd);

};

#endif
