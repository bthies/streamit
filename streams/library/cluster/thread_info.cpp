
#include <thread_info.h>

thread_info::thread_info(int thread_id) {
  this->thread_id = thread_id;
}

int thread_info::get_thread_id() {
  return thread_id;
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


void thread_info::set_state_flag(int *state_flag) { 
  this->state_flag = state_flag; 
}

int *thread_info::get_state_flag() {
  return state_flag;
}


