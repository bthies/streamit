
#include <ccp.h>

#include <node_server.h>
#include <save_state.h>
#include <delete_chkpts.h>

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

ccp::ccp() {
  machines_in_partition = 0;
  number_of_threads = 0;
  initial_iteration = 0;
  waiting_to_start_execution = true;
}

void ccp::set_init_iter(int iter) {
  initial_iteration = iter;
}

void ccp::read_config_file() {

  int id, m_id, max;
  char buf[128];

  FILE *f = fopen("cluster-config.txt", "r");

  number_of_threads = 0;
  max = 0;
  partition.clear();

  for (;;) {
    fscanf(f, "%d %s", &id, buf);
    if (feof(f)) break;

    number_of_threads++; // increase number of threads

    sscanf(buf, "machine-%d", &m_id);
    if (m_id > max) max = m_id;

    m_id--; // adjust from 1..n to 0..(1-n) 
    
    partition[id] = m_id;
    printf("thread %d -> %s (%d)\n", id, buf, m_id);
  }

  fclose(f);

  printf("Number of nodes in partition: (%d)\n", max);
  machines_in_partition = max;
}


int ccp::run_ccp() {

  int listenfd;
  int retval;
  int flag;
  int fd;

  read_config_file();

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

  printf("Socket bound and listening....done\n");
  
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
	if (fd > maxfd) maxfd = fd;
	FD_SET(fd, &set);	
      }

    rwait.tv_sec = 0;
    rwait.tv_usec = 1000000 / 4; // 1/4th second

    retval = select(maxfd + 1, &set, NULL, NULL, &rwait);

    if (retval > 0) {

      if (FD_ISSET(listenfd, &set)) {

	struct sockaddr_in cliaddr;
	unsigned clilen = sizeof(cliaddr);
	
	fd = accept(listenfd, (struct sockaddr *)&cliaddr, &clilen);
	
	if (fd != -1) {
	  
	  unsigned ip = cliaddr.sin_addr.s_addr;
	  
	  /*
	  printf("Incomming connection from : %d.%d.%d.%d %d \n",
		 (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256),
		 cliaddr.sin_port );
	  */

	  if ( fcntl(fd, F_SETFL, O_NONBLOCK) == -1 ) {
	    
	    close(fd);
	    printf("Failed to set non-blocking!\n");	  
	    
	  } else {
	  
	    netsocket *sock = new netsocket(fd);
	  
	    ccp_session *s = new ccp_session(ip, sock);
	    sessions.push_back(s);
	    
	    printf("new connection from (%d.%d.%d.%d)\n", 
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
	    
	    printf("connection closed by (%d.%d.%d.%d)\n", 
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

    //printf("latest chkpt is: [%d]", latest_chkpt);
    //fflush(stdout);

    delete_chkpts::set_max_iter(latest_chkpt);

    /*** afters select querry threads if they are alive ***/

    
    
    for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i)
      {

	bool alive = (*i)->is_alive();
	unsigned ip  = (*i)->get_ip();

	if (!alive) {

	  printf("connection closed from (%d.%d.%d.%d) no longer ALIVE\n", 
		 (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256));

	  sessions.erase(i, i+1); // remove session
	  handle_change_in_number_of_nodes();

	}
      }

    

  }
}



void ccp::handle_change_in_number_of_nodes() {

  int count = sessions.size();
  
  printf("Number of nodes available: %d\n", 
	 count);

  if ( waiting_to_start_execution ) {

    if (count == machines_in_partition) {

      printf("Enough nodes to start cluster execution!\n");
      
      assign_nodes_to_partition();
      
      printf("Assignement of threads to nodes...\n");
      
      for (int t = 0; t < number_of_threads; t++) {
	unsigned ip = machines[partition[t]];
	printf("thread: %d ip: (%d.%d.%d.%d)\n", t, (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256)); 
      }
      
      send_cluster_config(initial_iteration);

      for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
	(*i)->extend_alive_limit();
      }

      waiting_to_start_execution = false;
    }

  } else {
    
    if (count == 0) {
    
      printf("All nodes have disconnected!");

    } else {
    
      
      /* send STOP_ALL_THREADS to all active sessions */

      for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {

	(*i)->get_socket()->write_int(STOP_ALL_THREADS);
	int ret = (*i)->get_socket()->read_int();

	unsigned ip = (*i)->get_ip();
      
	printf("Node: (%d.%d.%d.%d) ", (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256)); 
	if (ret == 1) {
	  printf("All threads stoped\n");
	} else {
	  printf("Unexpected error!\n");
	}

      }


      execute_partitioner(count);

      read_config_file();

      assign_nodes_to_partition();
      
      printf("Assignement of threads to nodes...\n");
      
      for (int t = 0; t < number_of_threads; t++) {
	unsigned ip = machines[partition[t]];
	printf("thread: %d ip: (%d.%d.%d.%d)\n", t, (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256)); 
      }

      int iter = save_state::find_max_iter(number_of_threads);

      printf("Latest checkpoint found is: %d\n", iter);

      send_cluster_config(iter);

      for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
	(*i)->extend_alive_limit();
      }

    }
  }
}


void ccp::send_cluster_config(int iter) {

  printf("Sending cluster configuration to cluster nodes... ");
  fflush(stdout);

  for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {

    (*i)->get_socket()->write_int(CLUSTER_CONFIG);
    (*i)->get_socket()->write_int(number_of_threads);

    for (int t = 0; t < number_of_threads; t++) {
      unsigned ip = machines[partition[t]];

      (*i)->get_socket()->write_int(t);
      (*i)->get_socket()->write_chunk((char*)&ip, sizeof(unsigned));
      (*i)->get_socket()->write_int(iter);
    }
  }
  printf("done.\n");

  for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
    
    (*i)->wait_until_configuration_read();

  }
  
  printf("All nodes have received configuration and have initialized!\n"); 

}


void ccp::execute_partitioner(int number_of_nodes) {

  char cmd[128];

  sprintf(cmd, "./do_part %d", number_of_nodes);

  printf("Executing partitioner for %d nodes...\n ", number_of_nodes);
  printf("=============================================================\n");

  FILE *f = popen(cmd, "w");

  pclose(f);

  printf("============================================================= done.\n");
}


void ccp::assign_nodes_to_partition() {

  int m = 0;
  machines.clear();

  printf("Assigning nodes to partition...\n");

  for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
  
    unsigned ip = (*i)->get_ip();

    printf("machine (%d) -> node (%d.%d.%d.%d)\n", 
	   m, (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256));

    // machines is a map (machine id -> ip address)

    machines[m] = ip;

    m++;    
  }
}

