
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


  node_info *connect(unsigned ip);
  vector<int> list(node_info *node);

  int pause(node_info *node, int id);
  int restart(node_info *node, int id);

  vector<int> indata(node_info *node, int id);
  vector<int> outdata(node_info *node, int id);

 public:
  
  master_server() {}

  void print_commands();

  void process_command(char *cmd);

};

#endif
