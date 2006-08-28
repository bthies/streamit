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

#ifndef __SOCKET_HOLDER_H
#define __SOCKET_HOLDER_H

#include <mysocket.h>

class socket_holder {

 protected:
  mysocket *sock;
  
  // true if sock is memsocket, flase if it is netsocket.
  bool is_mem_socket; 
  
 public:
  socket_holder() { sock = NULL; }

  mysocket *get_socket() { 
    return sock; 
  }

  void set_socket(mysocket *sock) { 
    this->sock = sock; 
    is_mem_socket = sock->is_mem_socket();
  }

  void delete_socket_obj() {
    if (!is_mem_socket) delete sock;
  }
  
};

#endif
