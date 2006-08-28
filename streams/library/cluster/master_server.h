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

#ifndef __MASTER_SERVER_H
#define __MASTER_SERVER_H

#include <string.h>
#include <vector>
#include <map>

#include <netsocket.h>
#include <open_socket.h>
#include <node_server.h>

using namespace std;

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
