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

#include <ccp_session.h>

ccp_session::ccp_session(unsigned ip, netsocket *sock) {
  this->ip = ip;
  this->sock = sock;
  this->alive_cmd_sent = false;
  this->alive_response_received = false;
  this->latest_checkpoint = 0;
  this->cpu_utilization = 0;
  this->idle_time = 0;
  this->socket_open = true;
}

netsocket *ccp_session::get_socket() {
  return sock;
}

bool ccp_session::is_socket_open() {
  return socket_open;
}

unsigned ccp_session::get_ip() {
  return ip;
}

// DB_COMMENT
int ccp_session::get_cpu_util() {
  return cpu_utilization;
}

// DB_COMMENT
int ccp_session::get_idle_time() {
  return idle_time;
}

int ccp_session::read_int(int *ptr) {

  char c[4];
  int retval = sock->read_chunk(&(c[0]), 1);
  
  if (retval == -1) {

    // socket closed
    socket_open = false;
    return -1;
    
  } else {
    
    int retval = sock->read_chunk(&(c[1]), 3);
      
    if (retval != -1) {
	  
      *ptr = *((int*)c);
      
      // DB_COMMENT read another char
      return 0;

    } else {

      // socket closed
      socket_open = false;
      return -1;
    }
  }
}

void ccp_session::read_alive_response() {

  int retval;
  int tmp;

  //QM
  int tmp2, tmp3;

  retval = read_int(&tmp);
  if (retval == -1) {
    printf("ccp_session::read_alive_response socket-closed-1\n");
    return;
  }
    
  // QM
  retval = read_int(&tmp2);
  if (retval == -1) {
    printf("ccp_session::read_alive_response socket-closed-2\n");
    return;
  }
  retval = read_int(&tmp3);
  if (retval == -1) {
    printf("ccp_session::read_alive_response socket-closed-3\n");
    return;
  }
  
  alive_response_received = true;	    
  latest_checkpoint = tmp;

  // QM
  cpu_utilization = tmp2;
  idle_time = tmp3;

  cpuUtil.push_back(cpu_utilization);  
  idleTime.push_back(idle_time);  

  if(cpuUtil.size() > 33)
  {
    vector<int>::iterator i;
    i = cpuUtil.begin();
    cpuUtil.erase(i,i+1);
  }

  if(idleTime.size() > 33)
  {
    vector<int>::iterator i;
    i = idleTime.begin();
    idleTime.erase(i,i+1);
  }


  calculate_avg();

/*  printf("[%d.%d.%d.%d] Alive resp is (chkpt=%d, util=%d, idle=%d)\n", 
	 (ip % 256), 
	 ((ip>>8) % 256), 
	 ((ip>>16) % 256), 
	 ((ip>>24) % 256),
	 latest_checkpoint, 
	 cpu_utilization, 
	 idle_time);*/

}

void ccp_session::wait_until_configuration_read() {

  int retval;
  int tmp;

  //QM
  int tmp2, tmp3;

  if (alive_cmd_sent && !alive_response_received) {

    read_alive_response();
  }

  retval = read_int(&tmp);
}


int ccp_session::read_data() {
  
  int retval;
  int tmp;

  if (alive_cmd_sent && !alive_response_received) {

    read_alive_response();
    return 0;
    
  }

}


/* returns difference in 1/100 seconds */

int difference(timeval tv1, timeval tv2) {
  
  int sec = tv2.tv_sec - tv1.tv_sec;
  int usec = tv2.tv_usec/10000 - tv1.tv_usec/10000;

  return sec*100 + usec;
}


void ccp_session::extend_alive_limit() {

  alive_cmd_sent = false;

}

bool ccp_session::is_alive() {
  
  // if last request more than 1 sec old & no response => dead
  // if last request more than 1 sec old & have response => resend request

  timeval now;

  if (!alive_cmd_sent) {

    sock->write_int(ALIVE_COMMAND);

    gettimeofday(&last_alive_request, NULL);
    alive_cmd_sent = true;
    alive_response_received = false;

    return true;

  } else {

    gettimeofday(&now, NULL);
  
    /* difference in 1/100 seconds */
    int diff = difference(last_alive_request, now);
    
    if (diff > 100) {

      if (alive_response_received == true) {
	
	alive_cmd_sent = false;
	return true;
      } else {
	
	return false;	
      }
    } 
    
  }
  
  return true;
}


int ccp_session::get_latest_checkpoint() {

  return latest_checkpoint;
}


void ccp_session::calculate_avg(){
  
  avg_cpu_utilization = 0;

  for(int i=0; i < cpuUtil.size(); i++)  
  {
     avg_cpu_utilization = avg_cpu_utilization + cpuUtil[i];
  }

  avg_cpu_utilization = (int)((double)avg_cpu_utilization/(double)cpuUtil.size()); 
  
  for(int i =0; i < idleTime.size(); i++)
  {
     avg_idle_time = avg_idle_time + idleTime[i];
  }

  avg_idle_time = (int)((double)avg_idle_time/(double)idleTime.size()); 
  
}

int ccp_session::get_avg_cpu_util() {  
  return avg_cpu_utilization;
}

// DB_COMMENT
int ccp_session::get_avg_idle_time() {

  return avg_idle_time;
}
