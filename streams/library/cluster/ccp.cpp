
#include <ccp.h>

ccp::ccp() {
  machines_in_partition = 0;
  number_of_threads = 0;
  initial_iteration = 0;
}

void ccp::set_init_iter(int iter) {
  initial_iteration = iter;
}

void ccp::read_config_file() {

  int id, m_id, max = 0;
  char buf[128];

  FILE *f = fopen("cluster-config.txt", "r");

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

    for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
      
      int fd = (*i)->get_socket()->get_fd();
      
      if (fd > maxfd) maxfd = fd;

      FD_SET((*i)->get_socket()->get_fd(), &set);

    }

    rwait.tv_sec = 1;
    rwait.tv_usec = 0;

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
	  
	    mysocket *sock = new mysocket(fd);
	  
	    ccp_session *s = new ccp_session(ip, sock);
	    sessions.push_back(s);
	    
	    printf("new connection from (%d.%d.%d.%d) number of nodes (%d)\n", 
		   (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256),
		   sessions.size());

	    if (sessions.size() == machines_in_partition) {
	      start_execution();

	    }
	  }
	}
      }

      
      for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
	
	if (FD_ISSET((*i)->get_socket()->get_fd(), &set)) {
	
	  printf("data available in session...");
  
	  int res = (*i)->read_data();
	  if (res == -1) {
	    unsigned ip = (*i)->get_ip();
	    
	    sessions.erase(i, i+1); // remove session

	    printf("connection closed by (%d.%d.%d.%d) number of nodes (%d)\n", 
		   (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256),
		   sessions.size());
	  }
	}
      }
    }
  }
}


void ccp::start_execution() {

  printf("Enough nodes to start cluster execution!\n");

  int m = 0;

  for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {
  
    unsigned ip = (*i)->get_ip();

    printf("machine (%d) -> node (%d.%d.%d.%d)\n", 
	   m, (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256));

    // machines is a map (machine id -> ip address)

    machines[m] = ip;

    m++;
    
  }

  for (int t = 0; t < number_of_threads; t++) {
    unsigned ip = machines[partition[t]];
    printf("thread: %d ip: (%d.%d.%d.%d)\n", t, (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256)); 
  }


  for (vector<ccp_session*>::iterator i = sessions.begin(); i < sessions.end(); ++i) {

    (*i)->get_socket()->write_int(60);
    (*i)->get_socket()->write_int(number_of_threads);

    for (int t = 0; t < number_of_threads; t++) {
      unsigned ip = machines[partition[t]];

      (*i)->get_socket()->write_int(t);
      (*i)->get_socket()->write_chunk((char*)&ip, sizeof(unsigned));
      (*i)->get_socket()->write_int(initial_iteration);
    }
  }
}
 
