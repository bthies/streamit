
#include <int_pair.h>

int_pair::int_pair(int from, int to) { 
  this->from = from; 
  this->to = to; 
}


int_pair& int_pair::operator=(const int_pair &rhs) {

  this->from = rhs.from;
  this->to = rhs.to;
  return *this;
}

int int_pair::operator==(const int_pair &rhs) const {
  if ( this->from != rhs.from) return 0;
  if ( this->to != rhs.to) return 0;
  return 1;
}


int int_pair::operator<(const int_pair &rhs) const {
  if (this->from == rhs.from && this->to < rhs.to) return 1;
  if (this->from < rhs.from) return 1;
  return 0;
}
