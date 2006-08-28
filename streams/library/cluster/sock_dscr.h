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

#ifndef __SOCK_DSCR_H
#define __SOCK_DSCR_H

class sock_dscr {

 public:
  int from, to, type;

  sock_dscr(int from, int to, int type);
  sock_dscr &operator=(const sock_dscr &rhs);
  int operator==(const sock_dscr &rhs) const;
  int operator<(const sock_dscr &rhs) const;
};


#endif
