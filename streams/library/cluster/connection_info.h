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
