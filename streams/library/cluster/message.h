
#ifndef __MESSAGE_H
#define __MESSAGE_H

#include "mysocket.h"

class message {

  int *params;
  int *current;

public:

  int size;
  int method_id;
  int execute_at;

  // stack variables
  message *next;
  message *previous;
  
  message(int size, int method_id, int execute_at);

  void read_params(mysocket *sock);

  int get_int_param(); // advances current
  float get_float_param(); // advances current

  message *push_on_stack(message *top); // returns the new stack
  message *remove_from_stack(message *top); // returns the new stack

};


#endif
