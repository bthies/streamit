
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




