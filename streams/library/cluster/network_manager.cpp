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

#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#include <vector.h>
#include <stdio.h>

#include <network_manager.h>

network_manager *network_manager::instance = NULL;

void read_data(int fd, network_connection *c) {

  int retval;
  retval = read(fd, 
		c->data_buffer + c->data_offset, 
		40000 - c->data_offset);
	  
  if (retval > 0) {
	    
    c->data_offset += retval;
    if (c->data_offset == 40000) {
	      
      //printf("done reading 40000 bytes\n");
      c->push(c->data_buffer);
      c->data_buffer = (char*)malloc(40000);
      c->data_offset = 0;

      read_data(fd, c); // read did not block - try once more
    }	    
  }

  return;
}

void write_data(int fd, network_connection *c) {

  int retval;

  if (c->data_offset == -1) {

    if (c->queue_empty()) return;
      
    c->data_buffer = (char*)c->pop();
    c->data_offset = 0;
  }
  
  retval = write(fd, 
		 c->data_buffer + c->data_offset, 
		 40000 - c->data_offset);
	    
  if (retval > 0) {
	      
    c->data_offset += retval;
    if (c->data_offset == 40000) {
		
      //printf("done writing 40000 bytes\n");
      free(c->data_buffer);
      c->data_offset = -1;

      write_data(fd, c); // write did not block - try once more
    }
  }	
}


void network_manager::run() {

  fd_set read_set;
  fd_set write_set;
  struct timeval rwait;
  
  int fd_max = 0;

  for (;;) {
  
    FD_ZERO(&read_set);
    FD_ZERO(&write_set);
    
    vector<network_connection*>::iterator i;
    
    for (i = in_connections.begin(); i < in_connections.end(); ++i) {      
      int fd = (*i)->get_fd();
      if (fd > fd_max) fd_max = fd;
      FD_SET(fd, &read_set);
    }

    for (i = out_connections.begin(); i < out_connections.end(); ++i) {      
      int fd = (*i)->get_fd();
      if (fd > fd_max) fd_max = fd;
      FD_SET(fd, &write_set);
    }
    
    rwait.tv_sec = 0;
    rwait.tv_usec = 100000; // 1/10th of a second
    
    int select_retval = select(fd_max + 1, &read_set, &write_set, NULL, &rwait);
    
    if (select_retval > 0) {
      
      for (i = in_connections.begin(); i < in_connections.end(); ++i) {
	int fd = (*i)->get_fd();
	if (FD_ISSET(fd, &read_set)) read_data(fd, *i);
      }

      for (i = out_connections.begin(); i < out_connections.end(); ++i) {
	int fd = (*i)->get_fd();
	if (FD_ISSET(fd, &write_set)) write_data(fd, *i); 
      }
    }

  } 

}


