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

#include <mysocket.h>

mysocket::mysocket() {
  check_thread_fptr = NULL;
}

void mysocket::set_check_thread_status(void (*check_thread_status_during_io)()) {
  check_thread_fptr = check_thread_status_during_io;
}

void mysocket::check_thread_status() {
  if (check_thread_fptr != NULL) check_thread_fptr();    
}

/*

int mysocket::read_int() {
  int a;
  read_chunk((char*)&a, 4);
  return a;
}

void mysocket::write_int(int a) {
  write_chunk((char*)&a, 4);
  return;
}

double mysocket::read_double() {
  double a;
  read_chunk((char*)&a, sizeof(a));
  return a;
}

void mysocket::write_double(double a) {
  write_chunk((char*)&a, sizeof(a));
  return;
}

float mysocket::read_float() {
  float a;
  read_chunk((char*)&a, sizeof(a));
  return a;
}

void mysocket::write_float(float a) {
  write_chunk((char*)&a, sizeof(a));
  return;
}
*/

