
#include <connection_info.h>

connection_info::connection_info(int from, int to, socket_holder *s) {
  this->from_id = from;
  this->to_id = to;
  this->sock_h = s;
}

int connection_info::get_from() {
  return from_id;
}

int connection_info::get_to() {
  return to_id;
}

socket_holder *connection_info::get_socket_holder() {
  return sock_h;
}
