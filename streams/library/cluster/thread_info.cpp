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

#include <thread_info.h>

thread_info::thread_info(int thread_id, void (*check_thread_status_during_io)()) {
  this->thread_id = thread_id;
  this->check_thread_status_during_io = check_thread_status_during_io;
  this->active = false;
  this->latest_checkpoint = 0;
  gettimeofday(&last_jiff, NULL);
  last_count = 0;
  usage = 0;
}

int thread_info::get_thread_id() {
  return thread_id;
}

void thread_info::set_active(bool a) {
  this->active = a;
}

bool thread_info::is_active() {
  return active;
}

void thread_info::add_incoming_data_connection(connection_info *info){
  incoming_data.push_back(info);
}

void thread_info::add_outgoing_data_connection(connection_info *info){
  outgoing_data.push_back(info);
}

vector<connection_info*> thread_info::get_incoming_data_connections() {
  return incoming_data;
}

vector<connection_info*> thread_info::get_outgoing_data_connections() {
  return outgoing_data;
}


void thread_info::set_pthread(pthread_t pthread) { 
  this->pthread = pthread; 
}

pthread_t thread_info::get_pthread() {
  return pthread;
}


int *thread_info::get_state_flag() {
  return &state_flag;
}


int *thread_info::get_latest_checkpoint() {
  return &latest_checkpoint;
}


