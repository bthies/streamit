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
#ifndef __CCP_SESSION_H
#define __CCP_SESSION_H

#include <sys/time.h>

#include <netsocket.h>
#include <node_server.h>

class ccp_session {

  unsigned ip;
  netsocket *sock;

  bool socket_open;

  timeval last_alive_request;
  bool alive_cmd_sent;
  bool alive_response_received;
  
  // Per CPU info
  int latest_checkpoint;
  int cpu_utilization;
  int idle_time;

  int avg_cpu_utilization;
  int avg_idle_time;

  vector<int> cpuUtil;
  vector<int> idleTime;

  int read_int(int *ptr);
  void calculate_avg();

 public:

  ccp_session(unsigned ip, netsocket *sock);

  netsocket *get_socket();
  bool is_socket_open();
  unsigned get_ip();

  int get_idle_time();
  int get_cpu_util();

  int get_avg_idle_time();
  int get_avg_cpu_util();

  int read_data();

  bool is_alive();
  void extend_alive_limit(); // temporarily extends alive limit

  void wait_until_configuration_read(); // waits until node confirms that it has received
                                        // the cluster configuration

  void read_alive_response();

  int get_latest_checkpoint();
};



#endif
