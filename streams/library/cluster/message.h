
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
  void push_float(float f);

  int get_int_param(); // advances current
  float get_float_param(); // advances current

  message *push_on_stack(message *top); // returns the new stack
  message *remove_from_stack(message *top); // returns the new stack

};

#endif
