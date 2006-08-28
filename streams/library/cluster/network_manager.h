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

#ifndef __NETWORK_MANAGER_H
#define __NETWORK_MANAGER_H

#include <service.h>
#include <network_connection.h>

using namespace std;

class network_manager : public service {

  vector<network_connection*> in_connections;
  vector<network_connection*> out_connections;

  static network_manager *instance;

 public:

  static network_manager *get_instance() {
    if (instance == NULL) {
      instance = new network_manager(); 
    }
    return instance;
  }

  void add_connection(network_connection *c) {
    if (c->read) {
      in_connections.push_back(c);
    } else {
      out_connections.push_back(c);
    }
  }

  virtual void run();

};

#endif




