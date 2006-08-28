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

#ifndef __MESSAGE_H
#define __MESSAGE_H

#include <netsocket.h>

class message {

  int *params;
  int *current;
  int *write_ptr;

public:

  int size;
  int method_id;
  int execute_at;

  // stack variables
  message *next;
  message *previous;
  
  message(int size, int method_id, int execute_at);

  void read_params(netsocket *sock) { read_params(sock, 12); }
  void read_params(netsocket *sock, int head_size);

  void alloc_params(int param_size);
  void push_int(int a);
  void push_int_array(int* src, int length);
  void push_float(float f);
  void push_float_array(float* src, int length);

  int get_int_param(); // advances current
  void get_int_array_param(int* dst, int length);
  float get_float_param(); // advances current
  void get_float_array_param(float* dst, int length);

  message *push_on_stack(message *top); // returns the new stack
  message *remove_from_stack(message *top); // returns the new stack

  /**
   * Called to indicate that a message delivery deadline has been missed.
   */
  static void missed_delivery();
};

#endif
