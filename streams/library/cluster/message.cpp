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

#include <message.h>

#include <string.h>
#include <stdlib.h>
#include <assert.h>

message::message(int size, int method_id, int execute_at) {
  this->size = size;
  this->method_id = method_id;
  this->execute_at = execute_at;
}

void message::read_params(netsocket *sock, int head_size) {
  int param_size = size - head_size;
  if (param_size <= 0) return;
  params = (int*)malloc(param_size);
  current = params;
  sock->read_chunk((char*)params, param_size);
}

void message::alloc_params(int param_size) {
  params = (int*)malloc(param_size);
  current = params;
  write_ptr = params;
}

void message::push_int(int a) {
  *(int*)(write_ptr++) = a;
}

void message::push_int_array(int* src, int length) {
  memcpy(write_ptr, src, length*sizeof(int));
  write_ptr+=length;
}

void message::push_float(float f) {
  *(float*)(write_ptr++) = f;
}

void message::push_float_array(float* src, int length) {
  memcpy(write_ptr, src, length*sizeof(float));
  write_ptr+=length;
}

int message::get_int_param() {
  return *((int*)(current++));
}

void message::get_int_array_param(int* dst, int length) {
  memcpy(dst, current, length*sizeof(int));
  current+=length;
}

float message::get_float_param() {
  return *((float*)(current++));
}

void message::get_float_array_param(float* dst, int length) {
  memcpy(dst, current, length*sizeof(float));
  current+=length;
}

message *message::push_on_stack(message *stack_top) {
  if (stack_top == NULL) {
    //previous = NULL;
    next = NULL;
    return this;
  } else {
    message *tmp = stack_top;
    while (tmp->next != NULL) tmp = tmp->next;
    tmp->next = this;
    next = NULL;
    //previous = tmp;
    return stack_top;
  }
}

message *message::remove_from_stack(message *stack_top) {
  assert(1==0);
  /*
  if (previous == NULL) {
    next = NULL;
    return next;
  } else {
    previous->next = next;
    previous = NULL;
    next = NULL;
    return stack_top;
  }
  */
}

/**
 * Called to indicate that a message delivery deadline has been missed.
 */
void message::missed_delivery() {
    fprintf(stderr, "ERROR:\n");
    fprintf(stderr, "Missed a message delivery deadline.  If you are running under -cluster,\n");
    fprintf(stderr, "this means that your program is invalid due to an impossibly tight\n");
    fprintf(stderr, "message constraint (or there is a bug in our messaging system.)\n");
    fprintf(stderr, "If you are running under default strc, then the default schedule does\n");
    fprintf(stderr, "not respect one of your constraints.  Try running under -cluster\n");
    fprintf(stderr, "or -library.\n");
    fprintf(stderr, "Exiting...\n");
    exit(1);
}

