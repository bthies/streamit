
#include <connection_info.h>

connection_info::connection_info(int from, int to, mysocket *s) {
  this->from_id = from;
  this->to_id = to;
  this->sock = s;
}

int connection_info::get_from() {
  return from_id;
}

int connection_info::get_to() {
  return to_id;
}

mysocket *connection_info::get_socket() {
  return sock;
}
