
#include <sock_dscr.h>

sock_dscr::sock_dscr(int from, int to, int type) { 
  this->from = from; 
  this->to = to; 
  this->type = type;
}


sock_dscr& sock_dscr::operator=(const sock_dscr &rhs) {

  this->from = rhs.from;
  this->to = rhs.to;
  this->type = rhs.type;
  return *this;
}

int sock_dscr::operator==(const sock_dscr &rhs) const {
  if ( this->from != rhs.from) return 0;
  if ( this->to != rhs.to) return 0;
  if ( this->type != rhs.type) return 0;
  return 1;
}


int sock_dscr::operator<(const sock_dscr &rhs) const {

  if (this->from < rhs.from) return 1;

  if (this->from == rhs.from && 
      this->to < rhs.to) return 1;

  if (this->from == rhs.from && 
      this->to == rhs.to &&
      this->type < rhs.type) return 1;

  return 0;
}
