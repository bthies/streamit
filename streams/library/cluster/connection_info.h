#ifndef __CONNECTION_INFO
#define __CONNECTION_INFO

#include <socket_holder.h>

class connection_info {

  int from_id, to_id; // thread id's

  socket_holder *sock_h;
  
 public:

  connection_info(int from, int to, socket_holder *s);

  int get_from();
  int get_to();

  socket_holder *get_socket_holder();

};

#endif
