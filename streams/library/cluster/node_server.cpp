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

#include <node_server.h>

node_server::node_server(vector <thread_info*> list, void (*thread_init)()) {
  this->thread_list = list;
  this->thread_init = thread_init;
  gettimeofday(&last_jiff, NULL);
}


void node_server::run(unsigned ccp_ip) {

  netsocket *sock;

  if (ccp_ip == 0) {
    init_instance::read_config_file();

    if (thread_init != NULL) thread_init();

    //fprintf(stderr, "node_server: %s\n", "sleeping now..."); fflush(stderr);
    for (;;) {
      sleep(10);

      //sock = wait_for_connection();
      //run_server(sock);
    }

  } else {

    int t1, t2;
    old_cpu_utilization = 0;
    old_idle_time = 0;
    measure_load(&t1,&t2);
    printf("measure_load: Ok.\n");

    sock = connect_to_ccp(ccp_ip);
    run_server(sock);

  }
}

netsocket *node_server::wait_for_connection() {

  netsocket *sock;
  
  for (;;) {
    sock = open_socket::listen(22223);
    if (sock != NULL) return sock;
  }
}


netsocket *node_server::connect_to_ccp(unsigned ccp_ip) {

  netsocket *sock;
  
  for (;;) {
    sock = open_socket::connect(ccp_ip, 3000);
    if (sock != NULL) return sock;
    //fprintf(stderr, "node_server::connect_to_ccp: sleep and retry open"); fflush(stderr);
    sleep(1);
  }
}


int node_server::read_cluster_config(netsocket *sock) {
  
  int tmp;

  tmp = sock->read_int(); // number of threads

  /*
  if (tmp != n_threads) {
    fprintf(stderr,"ERROR: reading cluster config (n_threads)!\n");
    return -1;
  }
  */

  int thread, iter;
  unsigned ip;
  
  for (int i = 0; i < tmp; i++) {
  
    thread = sock->read_int();
    sock->read_chunk((char*)&ip, sizeof(unsigned));
    iter = sock->read_int();

    fprintf(stderr,"read: %d %u %d\n", thread, ip, iter);
    
    init_instance::set_thread_ip(thread, ip);
    init_instance::set_thread_start_iter(thread, iter);
  }

  fprintf(stderr,"done.\n");

  return 0;
}


void node_server::run_server(netsocket *sock) {

  int cmd, par1;
  int thread_id;
    
  for (;;) {
    cmd = sock->read_int();

    //fprintf(stderr,"received command: %d\n", cmd);
    
    if (sock->eof()) break;
    
    vector<int> resp;

    //printf("Node server: got command: %d\n", cmd);

    switch (cmd) {

    case ALIVE_COMMAND: {

      int __n = find_latest_checkpoint();

      // QM
      int __cu, __it;
      measure_load(&__cu, &__it);
      //printf("Alive...\n");

      fprintf(stderr,"Alive resp is (chkpt=%d, util=%d, idle= %d)\n", __n, __cu, __it);
      resp.push_back(__n);
      resp.push_back(__cu);
      resp.push_back(__it);
      break;
    }
    case CLUSTER_CONFIG:
      
      init_instance::reset_all();
      read_cluster_config(sock);

      for (vector<thread_info*>::iterator iter = thread_list.begin();
	   iter < thread_list.end(); ++iter) 	
	(*iter)->set_active(false);

      if (thread_init != NULL) thread_init();   // initialize sockets and threads according to configuration
      resp.push_back(1);
      break;

    case STOP_ALL_THREADS:

      resp = stop_all();
      break;

    case LIST_COMMAND:
	
      resp = list();
      break;

    case PAUSE_PROPER_COMMAND:
	  
      thread_id = sock->read_int();
      resp = pause_proper(thread_id);
      break;
    
    case PAUSE_ANY_COMMAND:
      
      thread_id = sock->read_int();
      resp = pause_any(thread_id);
      break;

    case RESUME_COMMAND:
      
      thread_id = sock->read_int();
      resp = resume(thread_id);
      break;

    case LIST_INCOMING_DATA_LINKS:
      
      thread_id = sock->read_int();
      resp = list_incoming_data_links(thread_id);
      break;
    
    case LIST_OUTGOING_DATA_LINKS:
      
      thread_id = sock->read_int();
      resp = list_outgoing_data_links(thread_id);
      break;
    
    }
    
    for (vector<int>::iterator iter = resp.begin(); iter < resp.end(); ++iter) {    
      sock->write_int(*iter);
    }
    
  }
}


// QM
void node_server::measure_load(int *cpu_util_out, int *idle_time_out) {

  double jiffy = sysconf(_SC_CLK_TCK);

  //printf("here, jiffy = 1/%f sec\n",jiffy);

  timeval tv;
  gettimeofday(&tv, NULL);

  float time_diff = 0;
  time_diff += (tv.tv_sec-last_jiff.tv_sec);
  time_diff += (tv.tv_usec-last_jiff.tv_usec) / 1e6;
  last_jiff.tv_sec = tv.tv_sec;
  last_jiff.tv_usec = tv.tv_usec;
  //printf("-- time difference     : %5.2f seconds\n", time_diff);

  float jiff_sec = 0;

  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {

    thread_info *info = *iter;
    if (info->is_active()) jiff_sec += info->usage;
  }

  //printf("-- streamit jiffy usage: %6.2f jiffies/second\n", jiff_sec);

  unsigned long utime, nice, stime, idle, sum;

  FILE *stat = fopen("/proc/stat", "r");
  fscanf(stat, "cpu  %lu %lu %lu %lu",&utime,&nice,&stime,&idle);
  fclose(stat);

  sum = utime + nice + stime + idle;
  //printf("-- sum: %lu    old_sum: %lu    sum-old_sum: %lu\n",sum,old_sum, sum-old_sum);
  //printf("-- idle: %lu    old_idle: %lu    idle-old_idle: %lu\n",idle,old_idle_time, idle-old_idle_time);
  
  if (sum - old_sum != 0) {  // Else, divides by 0

    *cpu_util_out = (int)((100 * jiff_sec) / (sum - old_sum));
    *idle_time_out = (int)((100 * ( idle - old_idle_time ) ) / (sum - old_sum));

  } else {

    *cpu_util_out = 0;
    *idle_time_out = 0;

  }

  if (*cpu_util_out < 0) *cpu_util_out = 0;
  if (*cpu_util_out > 100) *cpu_util_out = 100;

  if (*idle_time_out < 0) *idle_time_out = 0;
  if (*idle_time_out > 100) *idle_time_out = 100;

  if (*cpu_util_out + *idle_time_out > 100) 
    *idle_time_out = 100 - *cpu_util_out;

  //printf("-- cpu util: %d idle: %d\n", *cpu_util_out, *idle_time_out);

  old_sum = sum;
  old_idle_time = idle;
  

  /*
  struct rusage usage;
  int ret;

  int idle_time, cpu_util;

  char file[256], buf[128], b;
  unsigned long c, cutime, cstime;
  unsigned long utime, nice, stime, idle, sum;
  int pid, a, i;


  FILE *stat = fopen("/proc/stat", "r");
  fscanf(stat, "cpu  %lu %lu %lu %lu",&utime,&nice,&stime,&idle);
  fclose(stat);
  

  pid = getpid();
  sprintf(file, "/proc/%d/stat", pid);
  FILE *statp = fopen(file, "r");
  fscanf(statp, "%d %s %c", &a, buf, &b);
  for (i = 0; i < 5; i++) {
    fscanf(statp, "%d ", &a);
  }
  for (i = 0; i < 7; i++) {
    fscanf(statp, "%lu ", &c);
  }
  fscanf(statp, "%lu %lu", &cutime, &cstime);
  fclose(statp);

  ret = getrusage(RUSAGE_SELF, &usage);
  cutime = (unsigned long) ((double)usage.ru_utime.tv_usec * 1000 / jiffy);
  cstime = (unsigned long) ((double)usage.ru_stime.tv_usec * 1000 / jiffy);

  printf("-- ru_utime: %d   ru_stime: %d  \n", usage.ru_utime.tv_usec, usage.ru_stime.tv_usec);

  ret = getrusage(RUSAGE_CHILDREN, &usage);
  cutime += (unsigned long) ((double)usage.ru_utime.tv_usec * 1000 / jiffy);
  cstime += (unsigned long) ((double)usage.ru_stime.tv_usec * 1000 / jiffy);

  printf("-- ru_utime: %d   ru_stime: %d  \n", usage.ru_utime.tv_usec, usage.ru_stime.tv_usec);

  sum = utime + nice + stime + idle;

  printf("-- pid: %d\n", pid);

  printf("-- utime: %lu  nice: %lu  stime: %lu  idle: %lu\n",utime,nice,stime,idle);
  printf("-- cutime: %lu    cstime: %lu\n",cutime,cstime);

  printf("-- sum: %lu    old_sum: %lu    sum-old_sum: %lu\n",sum,old_sum, sum-old_sum);

  printf("[sum, cutime, cstime] = [%lu,%lu,%lu]\n",sum,cutime,cstime);

  if (sum > old_sum) {

    printf(" sum > old_sum \n");

    idle_time = (int)((100 * ( idle - old_idle_time ) ) / (sum - old_sum));

    cpu_util = (int)((100 * (cutime + cstime - old_cpu_utilization)) / (sum - old_sum));

  old_sum = sum;
  old_idle_time = idle;
  old_cpu_utilization = cutime + cstime;

  } else if (old_sum <= 0) {
    printf(" sum <= old_sum && old_sum <= 0 \n");
    idle_time = 0;
    cpu_util = 0;
  } else {

    printf(" sum <= old_sum \n");

    idle_time = (int)((100 * ( old_idle_time ) / (old_sum)));
    
    cpu_util = (int)(100 * (old_cpu_utilization) / (old_sum));

  }

  printf("cpu_util: %d  idle_time: %d\n",cpu_util,idle_time);

  //assert(idle_time < 110);
  //assert(cpu_util < 110);

  if (idle_time < 0) idle_time = 0;
  if (cpu_util < 0) cpu_util = 0;
  if (idle_time > 100) idle_time = 100;
  if (cpu_util > 100) cpu_util = 100;

  printf("CPU utilization: %d  Idle Time: %d\n",cpu_util,idle_time);

  *cpu_util_out = cpu_util;
  *idle_time_out = idle_time;
  */
}


// QM
//int node_server::idle_time() {
//  int a,b,c, idle;
//  FILE *stat = fopen("/proc/stat", "r");
//  fscanf(stat, "cpu  %d %d %d %d",&a,&b,&c,&idle);
//  fclose(stat);
//  a = idle - old_idle_time;
//  old_idle_time = idle;
//  return a;
//}
//
// QM
//int node_server::cpu_utilization() {
//  char file[256], buf[128], b;
//  int pid, a, i;
//  unsigned long c, cutime, cstime;
//  pid = getpid();
//  sprintf(file, "/proc/%d/stat", pid);
//  FILE *stat = fopen(file, "r");
//  fscanf(stat, "%d %s %c", &a, buf, &b);
//  for (i = 0; i < 5; i++) {
//    fscanf(stat, "%d ", &a);
//  }
//  for (i = 0; i < 7; i++) {
//    fscanf(stat, "%lu ", &c);
//  }
//  fscanf(stat, "%lu %lu", &cutime, &cstime);
//  fclose(stat);
//  c = cutime + cstime - old_cpu_utilization;
//  old_cpu_utilization = cutime + cstime;
//  return (int)c;
//}

int node_server::find_latest_checkpoint() {
  
  int resp = 0;

  bool any = false;

  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {

    thread_info *info = *iter;
    
    if (info->is_active()) {
  
      int latest = *info->get_latest_checkpoint();

      //fprintf(stderr,"(%d->%d)", info->get_thread_id(), latest);
      //fflush(stderr);

      if (latest < resp || !any) resp = latest;
      
      any = true;
    }
  }

  return resp;
}


vector<int> node_server::stop_all() {
  
  vector<int> resp;

  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {

    thread_info *info = *iter;
    
    if (info->is_active()) {
  
      int *state = info->get_state_flag();
      *state = EXIT_THREAD;
    }
  }

  resp.push_back(1);
  return resp;
}


vector<int> node_server::list() {
  
  vector<int> resp;

  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {

    thread_info *info = *iter;
    
    if (info->is_active()) resp.push_back( info->get_thread_id() );
  }

  resp.push_back(-1);
  return resp;
}


vector<int> node_server::pause_proper(int thread_id) {

  vector<int> resp;

  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {
    
    thread_info *info = *iter;

    if ( info->get_thread_id() == thread_id && info->is_active() ) {

      *(info->get_state_flag()) = PAUSE_PROPER_REQUEST;

      //fprintf(stderr, "node_server::pause_proper..."); fflush(stderr); 
      for (;;) {

	usleep(10000); // sleep 1/100th of a second
	if (*(info->get_state_flag()) == PAUSE_PROPER_ENTERED) break;

      }
      //fprintf(stderr, "node_server::pause_proper: done\n"); fflush(stderr);
	    
      resp.push_back(0);
      return resp;
    }
  }

  resp.push_back(-1);
  return resp;
}


vector<int> node_server::pause_any(int thread_id) {

  vector<int> resp;

  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {
    
    thread_info *info = *iter;

    if ( info->get_thread_id() == thread_id && info->is_active() ) {

      *(info->get_state_flag()) = PAUSE_ANY_REQUEST;

      //fprintf(stderr, "node_server::pause_any..."); fflush(stderr); 
      for (;;) {

	usleep(10000); // sleep 1/100th of a second

	if (*(info->get_state_flag()) == PAUSE_PROPER_ENTERED) {
	  resp.push_back(0);
	  break;
	}

	if (*(info->get_state_flag()) == PAUSE_IO_ENTERED) {
	  resp.push_back(1);
	  break;
	}

      }
      //fprintf(stderr, "node_server::pause_any: done\n"); fflush(stderr);
	    
      return resp;
    }
  }

  resp.push_back(-1);
  return resp;
}


vector<int> node_server::resume(int thread_id) {

  vector<int> resp;
  
  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {
    
    thread_info *info = *iter;
    
    if ( info->get_thread_id() == thread_id && info->is_active() ) {
      
      *(info->get_state_flag()) = RUN_STATE;
      
      resp.push_back(0);
      return resp;
    }
  }

  resp.push_back(-1);
  return resp;
}


vector<int> node_server::list_incoming_data_links(int thread_id) {

  vector<int> resp;

  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {
    
    thread_info *info = *iter;
    
    if (info->get_thread_id() == thread_id && info->is_active() ) {
      
      vector<connection_info*> list = 
	info->get_incoming_data_connections();
      
      for (vector<connection_info*>::iterator list_i = list.begin(); 
	   list_i < list.end(); 
	   ++list_i) {
	
	resp.push_back((*list_i)->get_from());    
      }
    }
  }

  resp.push_back(-1);
  return resp;
}


vector<int> node_server::list_outgoing_data_links(int thread_id) {

  vector<int> resp;

  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {
    
    thread_info *info = *iter;

    if (info->get_thread_id() == thread_id && info->is_active() ) {

      vector<connection_info*> list = 
	info->get_outgoing_data_connections();
      
      for (vector<connection_info*>::iterator list_i = list.begin(); 
	   list_i < list.end(); 
	   ++list_i) {
	
	resp.push_back((*list_i)->get_to());    
      }
    }
  }

  resp.push_back(-1);
  return resp;
}



