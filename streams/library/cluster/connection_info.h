#ifndef __CONNECTION_INFO
#define __CONNECTION_INFO

#include <mysocket.h>

class connection_info {

  int from_id, to_id; // thread id's

  mysocket *sock;
  
 public:

  connection_info(int from, int to, mysocket *s);

  int get_from();
  int get_to();

  mysocket *get_socket();

};

#endif
