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
