
#ifndef __MASTER_SERVER_H
#define __MASTER_SERVER_H

#include <string.h>
#include <vector>
#include <map>

#include <netsocket.h>
#include <open_socket.h>
#include <node_server.h>

class node_info {

  unsigned ip;
  netsocket *socket;

 public:
  
  node_info(unsigned ip, netsocket *socket); 

  unsigned get_ip();
  netsocket *get_socket();
};

class master_server {

  vector <node_info*> nodes;
  map <int, node_info*> node_map;


  node_info *connect(unsigned ip);
  vector<int> list(node_info *node);

  int pause_proper(node_info *node, int id);
  int pause_any(node_info *node, int id);
  int resume(node_info *node, int id);

  vector<int> indata(node_info *node, int id);
  vector<int> outdata(node_info *node, int id);

 public:
  
  master_server() {}

  void print_commands();

  void process_command(char *cmd);

};

#endif
