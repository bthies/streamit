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

#ifndef ARM

#include <ccp.h>

#include <node_server.h>
#include <save_state.h>
#include <delete_chkpts.h>

#include <assert.h>
#include <signal.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <ctype.h>
#include <unistd.h>
#include <strings.h>
#include <stdlib.h>
#include <fcntl.h>
#include <stdio.h>
//#include <string>

#include <queue>

#define DBG 0 
 
ccp::ccp(vector <thread_info*> list, int init_n) {
  this->init_nodes = init_n;
  this->thread_list = list;
  machines_in_partition = 0;
  number_of_threads = 0;
  initial_iteration = 0;
  waiting_to_start_execution = true;
  total_work_load = 0;
  cTime.tv_sec = 0;
}

void ccp::set_init_iter(int iter) {
  initial_iteration = iter;
}

int ccp::read_config_file(char *file_name) {

  int id, m_id, max;
  char buf[128];

  FILE *f = fopen(file_name, "r");

  if (f == NULL) return -1;

  number_of_threads = 0;
  max = 0;
  cur_partition.clear();
  new_partition.clear();

  for (;;) {
    fscanf(f, "%d %s", &id, buf);
    if (feof(f)) break;

    number_of_threads++; // increase number of threads

    sscanf(buf, "machine-%d", &m_id);
    if (m_id > max) max = m_id;

    m_id--; // adjust from 1..n to 0..(1-n) 
    
    cur_partition[id] = m_id;
    fprintf(stderr,"thread %d -> %s (%d)\n", id, buf, m_id);
  }

  fclose(f);

  fprintf(stderr,"Number of nodes in partition: (%d)\n", max);
  machines_in_partition = max;

  return 0;
}

// read work estimate from file
int ccp::read_work_estimate_file(char *file_name) {

  FILE *fp = fopen(file_name,"r");
  int thread_number = 0;
  int thread_usage =0;
  threadusage.clear();

  map<int, int>::iterator l_iter;

  if(fp == NULL) return -1;

  for (;;) {

    fscanf(fp, "%i %i", &thread_number, &thread_usage);

    if (feof(fp)) break;


    l_iter = cur_partition.find(thread_number);
   
    if(l_iter == cur_partition.end()) {
	fprintf(stderr, "thread %i doesn't exist in the cluster-config.txt \n", thread_number);
    }

    threadusage[thread_number] = thread_usage;
    
    total_work_load = total_work_load + thread_usage;
  }
  
#if DBG
  typedef map<int,int>::iterator l_d_iter;
  
  for (l_d_iter p=threadusage.begin(); p != threadusage.end(); ++p)
	fprintf(stderr, "read_work_estimate_file: thread number = %i \t usage = %i \n", p->first, p->second);  
#endif

  fclose(fp);
}


thread_info *ccp::get_thread_info(int id) {
  vector<thread_info*>::iterator i;
  for (i = thread_list.begin(); i < thread_list.end(); ++i)
    if ((*i)->get_thread_id() == id) return *i;
  assert(false);
}


void ccp::find_new_partition(int num_p) {
  char name[64];
  sprintf(name, "cluster-config.txt.%d", num_p);
  fprintf(stderr,"Trying to open file [%s]...\n", name); 
  
  int res = read_config_file(name);
  if (res == -1) {

    fprintf(stderr,"Failed to open [%s]\n", name); 

    float *targets = (float*)malloc(num_p * sizeof(float)); 
    for (int i = 0; i < num_p; i++) targets[i] = 1.0 / (float)num_p; 
    find_partition(num_p, targets);
    materialize_new_partition();

  } else {
    fprintf(stderr,"Success to open [%s]\n", name);
  }
}

void ccp::find_partition(int num_p, float *targets) {

  printf("find_partition: [ #threads=%d  thred_list.size=%d ]\n",
	 number_of_threads, thread_list.size());

  assert(number_of_threads == thread_list.size());

  int sum = 0, target;
  int added[number_of_threads];

  for (int i = 0; i < num_p; i++) { 
    new_weights[i] = targets[i];
  }

  for (int a = 0; a < number_of_threads; a++) {
    added[a] = 0;
    sum += threadusage[a];
  }

  printf("find_partition: sum of work est = %d\n", sum);

  printf("find_partition: targets [");
  for (int i = 0; i < num_p; i++) { 
    printf("%d=%4.2f", i, targets[i]);
    if (i < num_p-1) printf(",");
  }
  printf("]\n");

  vector<thread_info*>::iterator i;
  queue<int> q;

  for (int p = 0; p < num_p; p++) {

    target = (int)(sum * targets[p]);

    printf("find_partition: ================ Partition %d ================\n", p+1);

    int cur_sum = 0;

    for (i = thread_list.begin(); i < thread_list.end(); ++i) {
      
      vector<connection_info*> in = (*i)->get_incoming_data_connections();
      vector<connection_info*> out = (*i)->get_outgoing_data_connections();
      int id = (*i)->get_thread_id();
      int work = threadusage[id];
      
      vector<connection_info*>::iterator yy;
      bool all_prev_added = true;
      for (yy = in.begin(); yy < in.end(); ++yy) {
	if (!added[(*yy)->get_from()]) all_prev_added = false;
      }
      
      if (!added[id] && all_prev_added && 
	  (cur_sum + work < target || p == num_p-1 || cur_sum == 0)) {
	
	printf("find_partition: thread %d CAN start the partition (work est %d)\n",
	       id, work);
	
	new_partition[id] = p;
	q.push(id);
	cur_sum += work;
      }
    }
    
    while (q.size() > 0) {
      
      int id = q.front();
      q.pop();
      added[id] = 1;
      
      thread_info *i = get_thread_info(id);
      
      vector<connection_info*> out = i->get_outgoing_data_connections();
      vector<connection_info*>::iterator y;
      for (y = out.begin(); y < out.end(); ++y) {
	
	int to = (*y)->get_to();
	int work = threadusage[to];
	
	thread_info *ii = get_thread_info(to);
	vector<connection_info*> in = ii->get_incoming_data_connections();
	vector<connection_info*>::iterator yy;
	
	bool all_prev_added = true;
	for (yy = in.begin(); yy < in.end(); ++yy) {
	  if (!added[(*yy)->get_from()]) all_prev_added = false;
	}
	
	if (!added[to] && all_prev_added &&
	    (cur_sum + work < target || p == num_p-1)) {
	  
	  new_partition[to] = p;
	  q.push(to);
	  cur_sum += work;

	  long long p = cur_sum;
	  p *= 1000;
	  p /= target;
	  int pp = p;

	  printf("find_partition: ADDING thread %3d cur_sum=%9d/%9d (%2d.%d\%)\n", 
		 to, cur_sum, target, pp/10, pp%10);
	} 
      }
    }
  } 

  machines_in_partition = num_p;

}

// Returns partition "distance" as a percentage
int ccp::partition_distance() {

  float epsilon = 0;
  float sigma = 0;

  for(map<int,int>::iterator p = threadusage.begin();
      p != threadusage.end();
      ++p) {

    sigma += p->second;

    if (cur_partition[p->first] != new_partition[p->first])
      epsilon += p->second;

  }

  return (int)(epsilon * 100.0 / sigma);

}

int ccp::run_ccp() {

  signal(SIGPIPE, SIG_IGN);

  int listenfd;
  int retval, res;
  int flag;
  int fd;

  int last_latest_chkpt = 0;

  number_of_threads = thread_list.size();
  printf("Number of threads = %d\n", number_of_threads); 
  cur_partition.clear();
  new_partition.clear();
  for (int i = 0; i < number_of_threads; i++) cur_partition[i] = 0;

  fprintf(stderr,"Reading files.\n");
  //res = read_config_file("cluster-config.txt");
  //assert (res != -1);  
  
  res = read_work_estimate_file("work-estimate.txt");
  assert (res != -1);
  
  fprintf(stderr,"finish Reading files.\n");

  fprintf(stderr,"Deleting old checkpoints...");fflush(stderr);
  save_state::delete_checkpoints(2000000000);
  fprintf(stderr,"done.\n");

  find_thread_per_machine_mapping(); 
  cpu_utilization_per_thread();
  find_cpu_work_estimate();

  struct sockaddr_in serveraddr;

  listenfd = ::socket(AF_INET, SOCK_STREAM, 0);
  if (listenfd == -1) {

    return -1;
  }
  
  retval = setsockopt(listenfd, SOL_SOCKET, SO_REUSEADDR,
		      (char*)&flag, sizeof flag);
  if (retval == -1) {

    return -1;
  }

  bzero((char *)&serveraddr, sizeof(serveraddr));
  serveraddr.sin_family = AF_INET;
  serveraddr.sin_addr.s_addr = htonl(INADDR_ANY);
  serveraddr.sin_port = htons(3000);
  
  retval = bind(listenfd, (struct sockaddr *)&serveraddr,
                sizeof(serveraddr));

  if (retval == -1) {

    return -1;
  }
  
  retval = ::listen(listenfd, 10);

  if (retval == -1) {

    return -1;
  }

  retval = fcntl(listenfd, F_SETFL, O_NONBLOCK);

  if (retval == -1) {

    return -1;
  }

  fprintf(stderr,"ccp: Socket bound and listening....done\n");
  
  struct timeval rwait;

  fd_set set;

  for (;;) {

    int maxfd;

    FD_ZERO(&set);
    FD_SET(listenfd, &set);
    maxfd = listenfd;

    for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i)
      {
	int fd = (*i)->get_socket()->get_fd();
	if ((*i)->is_socket_open()) {
	  if (fd > maxfd) maxfd = fd;
	  FD_SET(fd, &set);	
	}
      }

    rwait.tv_sec = 0;
    rwait.tv_usec = 1000000 / 10; // 1/10th second

    retval = select(maxfd + 1, &set, NULL, NULL, &rwait);

    if (retval > 0) {

      if (FD_ISSET(listenfd, &set)) {

	struct sockaddr_in cliaddr;
	unsigned clilen = sizeof(cliaddr);
	
	fd = accept(listenfd, (struct sockaddr *)&cliaddr, (socklen_t *)&clilen);
	
	if (fd != -1) {
	  
	  unsigned ip = cliaddr.sin_addr.s_addr;
	  
	  /*
	  fprintf(stderr,"Incomming connection from : %d.%d.%d.%d %d \n",
		 (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256),
		 cliaddr.sin_port );
	  */

	  if ( fcntl(fd, F_SETFL, O_NONBLOCK) == -1 ) {
	    perror("ccp: Failed to set non-blocking! fcntl(): ");
	    close(fd);
	    //fprintf(stderr,"Failed to set non-blocking!\n");	  
	    
	  } else {
	  
	    netsocket *sock = new netsocket(fd);
	  
	    ccp_session *s = new ccp_session(ip, sock);
	    sessions.push_back(s);
	    
	    fprintf(stderr,"new connection from (%d.%d.%d.%d)\n", 
		   (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256));

	    handle_change_in_number_of_nodes();

	  }
	}
      }

      
      for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
	
	if (FD_ISSET((*i)->get_socket()->get_fd(), &set)) {
	
	  int res = (*i)->read_data();
	  if (res == -1) {
	    unsigned ip = (*i)->get_ip();
	    
	    fprintf(stderr,"connection closed by (%d.%d.%d.%d)\n", 
		   (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256));

	    sessions.erase(i, i+1); // remove session	    
	    handle_change_in_number_of_nodes();

	  }
	}
      }
    }

    int latest_chkpt = 0;

    bool any = false;

    for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {

      int tmp = (*i)->get_latest_checkpoint();

      if (tmp < latest_chkpt || !any) latest_chkpt = tmp;

      any = true;
    }

    if (latest_chkpt > last_latest_chkpt) {

      fprintf(stderr,"latest chkpt is: [%d]\n", latest_chkpt);
      fflush(stderr);

      delete_chkpts::set_max_iter(latest_chkpt);

    }

    last_latest_chkpt = latest_chkpt;

    /*** afters select querry threads if they are alive ***/

    
    
    for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i)
      {

	bool alive = (*i)->is_alive();
	bool open = (*i)->is_socket_open();
	unsigned ip  = (*i)->get_ip();

	if (!alive || !open) {

	  fprintf(stderr,"connection closed from (%d.%d.%d.%d) no longer ALIVE\n", 
		 (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256));

	  sessions.erase(i, i+1); // remove session
	  handle_change_in_number_of_nodes();

	}
      }

    //fprintf(stderr,"[End of the LOOP]\n");
    //fflush(stderr);

    if (waiting_to_start_execution) gettimeofday(&cTime,NULL);    

    timeval now;
    gettimeofday(&now,NULL);    
    float *weights = new float[machines_in_partition];

    if( (now.tv_sec - cTime.tv_sec) > 40)
    {
      find_thread_per_machine_mapping(); 
      find_cpu_work_estimate();

      float sum_work_load = 0;     
      for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i)
	{
	  
	  int tmp_cpu_util = (*i)->get_cpu_util();
	  int cpu_avg_util = (*i)->get_avg_cpu_util();
	  int cpu_avg_idle_time = (*i)->get_avg_idle_time();
	  
	  for(int j = 0; j < machines_in_partition; j++)
	    {
	      if(machines[j] == (*i)->get_ip())
		{
		  if(cpu_avg_util != 0) {
		    workToCpuUtil[j] = ( ((double)workPerCpu[j])/((double)cpu_avg_util) ) *(cpu_avg_util+cpu_avg_idle_time);
		    
		    printf("machine = %i workPerCpu = %i, cpu_avg_util = %i, cpu_avg_idle_time = %i, workToCpuUtil = %5.2f\n", j, workPerCpu[j], cpu_avg_util, cpu_avg_idle_time, workToCpuUtil[j]);
		    
		    sum_work_load = sum_work_load + workToCpuUtil[j];
#if DBG
		    printf("machine = %i workPerCpu = %i, cpu_avg_util = %i, workToCpuUtil = %5.2f\n", j, workPerCpu[j], cpu_avg_util, workToCpuUtil[j]);
#endif
		  }
		  else
		    {
		      workToCpuUtil[j] = 0;
		    }
		} 
	    }     
	  
	  
	  for (int tmp_m = 0; tmp_m < machines_in_partition; tmp_m++)
	    {
	      if (machines[tmp_m]==(*i)->get_ip())
		{
		  
		  typedef multimap<int, int>::const_iterator I;
		  
		  pair<I,I> b=machineTothread.equal_range(tmp_m);
		  
		  for (I i=b.first; i !=b.second; ++i)
		    { 
		      threadCpuUtil[i->second] = tmp_cpu_util;
		    }
		}
	    }
	}


      if( sum_work_load != 0 )
	{

	  for(int i=0; i < machines_in_partition ; i++) {
	    //           tmp_cpu_idle = (((double)workToCpuUtil[i])/(double)(cpu_avg_util)) *(   
	    weights[i] = workToCpuUtil[i]/sum_work_load; 
	    printf("machine id = %i , workToCpuUtil = %5.2f (current = %5.2f)\n",i, weights[i], cur_weights[i]);
	    
	  }
	  
	  printf("**** doing smothing ****\n");
	  
	  for(int i=0; i < machines_in_partition ; i++) {
	    weights[i] = (weights[i]*4+cur_weights[i])/5; 
	    printf("machine id = %i , workToCpuUtil = %5.2f (current = %5.2f)\n",i, weights[i], cur_weights[i]);
	    
	  }

	  
	  cTime.tv_sec = now.tv_sec;
	  
	  find_partition(machines_in_partition, weights);
	  
	  int dist = partition_distance();
	  printf("========== Partition distance is: %d ===========\n", dist);

	  if (dist > 4) {
            printf("CHANGE THE CONFIGURATION = %i \n", dist);
	    materialize_new_partition();
	    reconfigure_cluster();
	  }

	}
    } 

    delete weights;

  }
}



void ccp::handle_change_in_number_of_nodes() {

  int count = sessions.size();
  
  fprintf(stderr,"Number of nodes available: %d\n", 
	 count);

  if ( waiting_to_start_execution ) {

    if (count == init_nodes /*machines_in_partition*/) {

      fprintf(stderr,"Enough nodes to start cluster execution!\n");
      
      find_new_partition(count);

      assign_nodes_to_partition();
      
      find_thread_per_machine_mapping(); 
      find_cpu_work_estimate();

      fprintf(stderr,"Assignement of threads to nodes...\n");
      
      for (int t = 0; t < number_of_threads; t++) {
	unsigned ip = machines[cur_partition[t]];
	fprintf(stderr,"thread: %d ip: (%d.%d.%d.%d)\n", t, (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256)); 
      }
      
      send_cluster_config(initial_iteration);

      for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
	(*i)->extend_alive_limit();
      }

      waiting_to_start_execution = false;
    }

  } else {
    
    if (count == 0) {
    
      fprintf(stderr,"All nodes have disconnected!");

    } else {
    
 
      /* send STOP_ALL_THREADS to all active sessions */

      for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {

	(*i)->get_socket()->write_int(STOP_ALL_THREADS);
	int ret = (*i)->get_socket()->read_int();

	unsigned ip = (*i)->get_ip();
      
	fprintf(stderr,"Node: (%d.%d.%d.%d) ", (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256)); 
	if (ret == 1) {
	  fprintf(stderr,"All threads stopped\n");
	} else {
	  fprintf(stderr,"ccp: Unexpected error!\n");
	}

      }

      fprintf(stderr,"cpp: Sleep 5 seconds..."); fflush(stderr);
      sleep(5);
      fprintf(stderr,"Done.\n");

      //execute_partitioner(count);

      find_new_partition(count);

//      find_thread_per_machine_mapping(); 
      
      assign_nodes_to_partition();
      
      find_thread_per_machine_mapping(); 
      find_cpu_work_estimate();
      fprintf(stderr,"Assignment of threads to nodes...\n");
      
#if DBG
      for (int t = 0; t < number_of_threads; t++) {
	unsigned ip = machines[cur_partition[t]];
	fprintf(stderr,"thread: %d ip: (%d.%d.%d.%d)\n", t, (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256)); 
      }
#endif

      int iter = save_state::find_max_iter(number_of_threads);

      fprintf(stderr,"Latest checkpoint found is: %d\n", iter);

      send_cluster_config(iter);

      for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
	(*i)->extend_alive_limit();
      }

    }
  }
}


void ccp::reconfigure_cluster() {
 
  /* send STOP_ALL_THREADS to all active sessions */

  for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {

    (*i)->get_socket()->write_int(STOP_ALL_THREADS);
    int ret = (*i)->get_socket()->read_int();

    unsigned ip = (*i)->get_ip();
      
    fprintf(stderr,"Node: (%d.%d.%d.%d) ", (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256)); 
    if (ret == 1) {
      fprintf(stderr,"All threads stopped\n");
    } else {
      fprintf(stderr,"ccp: Unexpected error!\n");
    }

  }

  fprintf(stderr,"cpp: Sleep 2 seconds..."); fflush(stderr);
  sleep(2);
  fprintf(stderr,"Done.\n");

  fprintf(stderr,"Assignment of threads to nodes...\n");
      
  for (int t = 0; t < number_of_threads; t++) {
    unsigned ip = machines[cur_partition[t]];
    fprintf(stderr,"thread: %d ip: (%d.%d.%d.%d)\n", t, (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256)); 
  }

  int iter = save_state::find_max_iter(number_of_threads);

  fprintf(stderr,"Latest checkpoint found is: %d\n", iter);

  send_cluster_config(iter);

  for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
    (*i)->extend_alive_limit();
  }
}

void ccp::send_cluster_config(int iter) {

  fprintf(stderr,"Sending cluster configuration to cluster nodes... ");
  fflush(stderr);

  for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {

    (*i)->get_socket()->write_int(CLUSTER_CONFIG);
    (*i)->get_socket()->write_int(number_of_threads);

    for (int t = 0; t < number_of_threads; t++) {
      unsigned ip = machines[cur_partition[t]];

      (*i)->get_socket()->write_int(t);
      (*i)->get_socket()->write_chunk((char*)&ip, sizeof(unsigned));
      (*i)->get_socket()->write_int(iter);
    }
  }
  fprintf(stderr,"done.\n");
  fprintf(stderr,"Wait until config read.\n");
  fflush(stderr);

  for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
    
    (*i)->wait_until_configuration_read();

  }
  
  fprintf(stderr,"All nodes have received configuration and have initialized!\n"); 

}


void ccp::execute_partitioner(int number_of_nodes) {

  char cmd[128];

  sprintf(cmd, "./do_part %d", number_of_nodes);

  fprintf(stderr,"Executing partitioner for %d nodes...\n ", number_of_nodes);
  fprintf(stderr,"=============================================================\n");

  FILE *f = popen(cmd, "w");

  pclose(f);

  fprintf(stderr,"============================================================= done.\n");
}


void ccp::assign_nodes_to_partition() {

  int m = 0;
  machines.clear();

  fprintf(stderr,"Assigning nodes to partition...\n");

  for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
  
    unsigned ip = (*i)->get_ip();

    fprintf(stderr,"machine (%d) -> node (%d.%d.%d.%d)\n", 
	   m, (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256));

    // machines is a map (machine id -> ip address)

    machines[m] = ip;

    m++;    
  }
}

void ccp::materialize_new_partition() {

  for (int i = 0; i < number_of_threads; i++) cur_partition[i] = new_partition[i];
  for (int i = 0; i < machines_in_partition; i++) cur_weights[i] = new_weights[i];
}

// calculate the CPU ultilization per thread
void ccp::cpu_utilization_per_thread() {

    int nThread = 0;

    typedef map<int,unsigned>::const_iterator CI;

    for(CI p=machines.begin(); p !=(machines.end()); ++p)
    {
      typedef multimap<int, int>::const_iterator I;

      pair<I,I> b=machineTothread.equal_range(p->first);

      for (I i=b.first; i !=b.second; ++i)
      { 
         threadCpuUtil_n[i->second] = threadusage[i->second];
         nThread = nThread+ threadCpuUtil_n[i->second];          
      }

      for (I i=b.first; i !=b.second; ++i)
      {
         threadCpuUtil_n[i->second] = (int)(((double)threadCpuUtil_n[i->second]/(double)nThread) * 100.0);
#if DBG
	 fprintf(stderr, "cpu_utilization_per_thread, thread = %i cpu utilization = %i \n", i->second, threadCpuUtil_n[i->second]);
#endif
      }

    }

}

    
void ccp::find_thread_per_machine_mapping() 
{
  machineTothread.clear();

  for(map<int,int>::iterator j=cur_partition.begin(); j!=cur_partition.end(); ++j)
  {
      machineTothread.insert(make_pair(j->second,j->first));
#if DBG
      printf("find_thread_per_machine_mapping machine = %i,  Thread = %i \n",  j->first, j->second);
#endif
  }
  
#if DBG
  typedef multimap<int, int>::const_iterator I;
  for (I i=machineTothread.begin(); i !=machineTothread.end(); ++i){
      printf("find_thread_per_machine_mapping machine = %i,  Thread = %i \n",  i->first, i->second);
  }
#endif
}

void
ccp::find_cpu_work_estimate()
{

  for(int i = 0 ; i<machines_in_partition ; i++)
      workPerCpu[i] = 0;


  typedef multimap<int, int>::const_iterator I;
  for (I i=machineTothread.begin(); i !=machineTothread.end(); ++i){
      workPerCpu[i->first] = workPerCpu[i->first] + threadusage[i->second];

#if DBG
      printf("find_thread_per_machine_mapping machine = %i,  Thread = %i, workPerCpu =%i \n",  i->first, i->second, workPerCpu[i->first]);
#endif
  }

  printf("*************************WorkPerCPU**********************\n");
  printf("machines_in_partition %i \n", machines_in_partition);
  for(int i = 0 ; i < machines_in_partition ; i++)
      printf(" Cpu = %i,  workPerCPU = %i \n",  i, workPerCpu[i]);

}

#endif //ARM
