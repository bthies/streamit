
#include <ccp_session.h>

ccp_session::ccp_session(unsigned ip, mysocket *sock) {
  this->ip = ip;
  this->sock = sock;
  this->alive_cmd_sent = false;
  this->alive_response_received = false;
  this->extended_alive_limit = false;
}

mysocket *ccp_session::get_socket() {
  return sock;
}

unsigned ccp_session::get_ip() {
  return ip;
}

int ccp_session::read_data() {
  
  char c[4];
  int retval = sock->read_chunk(&(c[0]), 1);

  if (retval == -1) {

    // socket closed
    return -1;

  } else {

    if (alive_cmd_sent && !alive_response_received) {

        int retval = sock->read_chunk(&(c[1]), 3);

	if (retval != -1) {
	  
	  if ( *((int*)c) == 1 ) {
	    alive_response_received = true;	    

	  }

	} else {
	  
	  // socket closed
	  return -1;
	}
    }

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

  extended_alive_limit = true;

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
    
    if ((extended_alive_limit && diff > 1000) || diff > 100) {

      if (alive_response_received == true) {
	
	alive_cmd_sent = false;
	extended_alive_limit = false;
	return true;
      } else {
	
	return false;	
      }
    } 
    
  }
  
  return true;
}
