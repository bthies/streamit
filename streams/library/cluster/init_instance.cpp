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
#include <assert.h>
#include <netinet/tcp.h>
#include <init_instance.h>
#include <open_socket.h>
#include <netsocket.h>
#include <memsocket.h>

#ifdef ARM

void init_instance::add_incoming(int from, int to, int type) {assert(1==0);}
void init_instance::add_outgoing(int from, int to, int type) {assert(1==0);}

mysocket* init_instance::get_incoming_socket(int from, int to, int type) {assert(1==0);}
mysocket* init_instance::get_outgoing_socket(int from, int to, int type) {assert(1==0);}

unsigned init_instance::get_thread_start_iter(int thread) {assert(1==0);}

#else //ARM

#include <pthread.h>
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

static bool debugging = false;

int init_instance::start_iter = 0;

pthread_mutex_t init_instance::accept_lock = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t init_instance::bind_lock = PTHREAD_MUTEX_INITIALIZER;

vector<sock_dscr> init_instance::in_connections;
vector<sock_dscr> init_instance::out_connections;

map<sock_dscr, bool> init_instance::in_done;
map<sock_dscr, bool> init_instance::out_done;

map<sock_dscr, mysocket*> init_instance::in_sockets;
map<sock_dscr, mysocket*> init_instance::out_sockets;

short init_instance::listen_port = 22222;

map<int, unsigned> init_instance::thread_machines;
map<int, unsigned> init_instance::thread_start_iter;

init_instance::Thread_info init_instance::threadInfo;

void *accept_thread(void *param) {

  // this now locked my main thread
  //LOCK(&init_instance::accept_lock);

  if (init_instance::listen() == -1) {
    UNLOCK(&init_instance::accept_lock);
    fflush(stdout);
    fflush(stderr);
    exit(-1);
  }

  //fprintf(stderr, "init_instance:1 %s\n", "UNLOCK(&init_instance::accept_lock);");
  UNLOCK(&init_instance::accept_lock);

  return NULL;
}

void init_instance::reset_all() {

  thread_machines.clear();
  thread_start_iter.clear();
  threadInfo.clear();

  in_connections.clear();
  out_connections.clear();

  in_done.clear();
  out_done.clear();
 
  in_sockets.clear();
  out_sockets.clear();
}


void init_instance::read_config_file() {
  
  int node;
  char name[1024];

  FILE *f = fopen("cluster-config.txt", "r");
  if (debugging) {
    fprintf(stderr,"Reading cluster config file...\n");
  }
  for (;;) {
  
    fscanf(f, "%d %s", &node, name);

    if (feof(f)) break;

    string s(name);
    thread_machines[node] = lookup_ip(s.c_str());
    threadInfo[node].t_host_ip = lookup_ip(s.c_str());
    if (debugging) {
      fprintf(stderr,"thread:%d machine:%s\n", node, name);
    }
  }

  if (debugging) {
      fprintf(stderr,"\n");
  }
  
  fclose(f);
}



void init_instance::set_thread_ip(int thread, unsigned ip) {
  thread_machines[thread] = ip;
  threadInfo[thread].t_host_ip = ip;
}

void init_instance::set_thread_start_iter(int thread, unsigned iter) {
  thread_start_iter[thread] = iter;
}


unsigned init_instance::get_thread_ip(int thread) {
  // Here I should make a change also
  map<int, unsigned>::iterator i = thread_machines.find(thread);

  if (i == thread_machines.end()) {
    return 0;
  } else {
    return (unsigned)(*i).second;
  }
}


void init_instance::set_start_iter(int iter) {
  start_iter = iter;
}


unsigned init_instance::get_thread_start_iter(int thread) {

  map<int, unsigned>::iterator i = thread_start_iter.find(thread);

  if (i == thread_start_iter.end()) {
    return start_iter;
  } else {
    return (unsigned)(*i).second;
  }
}


void init_instance::add_incoming(int from, int to, int type) {

  sock_dscr sd(from, to, type);
  in_connections.push_back(sd);
}


void init_instance::add_outgoing(int from, int to, int type) {

  sock_dscr sd(from, to, type);
  out_connections.push_back(sd);
}


void init_instance::initialize_sockets() {

  for (vector<sock_dscr>::iterator i = in_connections.begin(); i < in_connections.end(); ++i) {
  
    sock_dscr sd = *i;
    in_done[sd] = false;
  } 

  for (vector<sock_dscr>::iterator i = out_connections.begin(); i < out_connections.end(); ++i) {
  
    sock_dscr sd = *i;
    out_done[sd] = false;
  }


  // create local socket pair where applicable

  if (debugging) {
    fprintf(stderr,"Creating shared memory sockets...\n");
    fflush(stderr);
  }

  for (vector<sock_dscr>::iterator i = out_connections.begin(); i < out_connections.end(); ++i) {
  
    sock_dscr sd = *i;

    if (sd.type != DATA_SOCKET) continue;

    map<sock_dscr, bool>::iterator i = in_done.find(sd);

    if (i != in_done.end()) {
    
      // connection is both in the list of 'out connections' 
      // and in the list of 'in connections'

      // create pipe

      if (debugging) {
	fprintf(stderr,"Creating memory socket %d->%d type:%d\n", sd.from, sd.to, sd.type);
	fflush(stderr);
      }
      
      //int pfd[2];
      //int retval = pipe(pfd);
      //if (retval != 0) {
      //  perror("pipe");
      //  exit(-1);
      //}
      //out_sockets[sd] = pfd[0];
      //in_sockets[sd] = pfd[1];


#ifdef CONSUMER_BUFFER_SIZE 
 
      memsocket *ms = new memsocket();

      out_sockets[sd] = ms;
      in_sockets[sd] = ms;
      
      out_done[sd] = true;
      in_done[sd] = true;

#else

      int sockets[2];
      if (socketpair(AF_UNIX, SOCK_STREAM, 0, sockets) < 0) {
        perror("init_instance: opening stream socket pair");
	fflush(stderr);
	fflush(stdout);
        exit(-1);
      }

      out_sockets[sd] = new netsocket(sockets[0]);
      in_sockets[sd] = new netsocket(sockets[1]);
      
      out_done[sd] = true;
      in_done[sd] = true;

#endif
 
    }
  }

  if (debugging) {
    fprintf(stderr,"done\n");
  }

  for (vector<sock_dscr>::iterator i = in_connections.begin(); i < in_connections.end(); ++i) {
  
    sock_dscr sd = *i;
    map<sock_dscr, bool>::iterator i2 = in_done.find(sd);

    if (i2 != in_done.end() && (*i2).second == true) {
      in_connections.erase(i);
      i--;
    }
  }

  for (vector<sock_dscr>::iterator i = out_connections.begin(); i < out_connections.end(); ++i) {
  
    sock_dscr sd = *i;
    map<sock_dscr, bool>::iterator i2 = out_done.find(sd);

    if (i2 != out_done.end() && (*i2).second == true) { 
      out_connections.erase(i);
      i--;
    }
  }
  
  // create & run accept thread

  if (in_connections.size() > 0) {

    pthread_t id;
  
    //fprintf(stderr, "init_instance:2 %s\n", "LOCK(&accept_lock);");
    LOCK(&accept_lock);
    //fprintf(stderr, "init_instance:3 %s\n", "LOCK(&bind_lock);");
    LOCK(&bind_lock);

    pthread_create(&id, NULL, accept_thread, (void*)"Thread");
  
  }  

  //fprintf(stderr, "init_instance:4 %s\n", "LOCK(&bind_lock);");
  LOCK(&bind_lock);
  //fprintf(stderr, "init_instance:5 %s\n", "UNLOCK(&bind_lock);");
  UNLOCK(&bind_lock);

  // make connections to other hosts etc.

  int num = out_connections.size();

  vector<sock_dscr>::iterator i1 = out_connections.begin();

  for (int t = 0; t < num; t++) {
  
    sock_dscr sd = *i1;
    unsigned ip_addr = init_instance::get_thread_ip(sd.to);

    int data[3];
    
    data[0] = sd.from;
    data[1] = sd.to;
    data[2] = sd.type;

    netsocket *sock = NULL;

    while (sock == NULL) {
      sock = open_socket::connect(ip_addr, 22222);

      if (sock == NULL) {
	fprintf(stderr,"init_instance: Sleeping and retrying ...\n"); fflush(stderr);
	sleep(1);     
      }
    }

    //fprintf(stderr,"socket connected !!\n");

    sock->write_chunk((char*)data, 12);
    sock->read_chunk((char*)data, 12);

    //fprintf(stderr,"socket done: recieved reply %d %d !!\n", data[0], data[1]);

    out_sockets[sd] = new netsocket(sock->get_fd());

    //fprintf(stderr,"Out Socket Added from:%d to:%d socket:%d\n", pair.from, pair.to, sock->get_fd());

    ++i1;

  }

  if (debugging) {
    fprintf(stderr,"All outgoing connections created.\n");
  }
  // wait for accept thread to finish

  //fprintf(stderr, "init_instance:6 %s\n", "LOCK(&accept_lock);");
  LOCK(&accept_lock);
  //fprintf(stderr, "init_instance:7 %s\n", "UNLOCK(&accept_lock);");
  UNLOCK(&accept_lock);
}

mysocket* init_instance::get_incoming_socket(int from, int to, int type) {

  sock_dscr sd(from, to, type);

  map<sock_dscr, mysocket*>::iterator i = in_sockets.find(sd);

  if (i == in_sockets.end()) {
    return NULL;
  } else {
    return (*i).second;
  }
}

mysocket* init_instance::get_outgoing_socket(int from , int to, int type) {

  sock_dscr sd(from, to, type);

  map<sock_dscr, mysocket*>::iterator i = out_sockets.find(sd);

  if (i == out_sockets.end()) {
    return NULL;
  } else {
    return (*i).second;
  }
}

/*
 * Seems to be entered with bind_lock LOCK'ed...
 */

int init_instance::listen() {

  int socks_accepted = 0;

  int listenfd;
  int retval;
  int flag;
  int sock;

  struct sockaddr_in serveraddr;

  listenfd = ::socket(AF_INET, SOCK_STREAM, 0);
  if (listenfd == -1) {

    perror("init_instance: socket()");
    UNLOCK(&bind_lock);
    return -1;
  }

  int w_size = 256 * 1000;
  setsockopt(listenfd, SOL_SOCKET, SO_SNDBUF, (char*)&w_size, sizeof w_size);
  setsockopt(listenfd, SOL_SOCKET, SO_RCVBUF, (char*)&w_size, sizeof w_size);

  flag = 1;

  retval = setsockopt(listenfd, SOL_SOCKET, SO_REUSEADDR,
			  (char*)&flag, sizeof flag);
  if (retval == -1) {
  
    perror("init_instance: setsockopt()");
    UNLOCK(&bind_lock);
    return -1;
  }

  bzero((char *)&serveraddr, sizeof(serveraddr));
  serveraddr.sin_family = AF_INET;
  serveraddr.sin_addr.s_addr = htonl(INADDR_ANY);
  serveraddr.sin_port = htons(listen_port);

  retval = bind(listenfd, (struct sockaddr *)&serveraddr,
                sizeof(serveraddr));

  if (retval == -1) {
  
    perror("init_instance: bind()");
    UNLOCK(&bind_lock);
    return -1;
  }

  retval = ::listen(listenfd, 10);

  if (retval == -1) {

    perror("init_instance: listen()");
    UNLOCK(&bind_lock);
    return -1;
  }

  retval = fcntl(listenfd, F_SETFL, O_NONBLOCK);

  if (retval == -1) {

    perror("init_instance: fcntl()");
    UNLOCK(&bind_lock);
    return -1;
  }

  if (debugging) {
      fprintf(stderr,"Socket bound and listening... done.\n");
  }
  
  //fprintf(stderr, "init_instance:8 %s\n", "UNLOCK(&bind_lock);");
  UNLOCK(&bind_lock);


  ///////////////////////////////////////////
  // listening on the socket
  ///////////////////////////////////////////

  struct timeval rwait;

  fd_set set;

  for (;;) {

    FD_ZERO(&set);
    FD_SET(listenfd, &set);

    rwait.tv_sec = 1;
    rwait.tv_usec = 0;

    if (select(listenfd + 1, &set, NULL, NULL, NULL) > 0) {

      struct sockaddr_in cliaddr;
      unsigned clilen = sizeof(cliaddr);

      //fprintf(stderr,"Accepting connection....");
      //fflush(stderr);

      sock = accept(listenfd, (struct sockaddr *)&cliaddr, (socklen_t *)&clilen);

      unsigned ip = cliaddr.sin_addr.s_addr;

      //fprintf(stderr,"Incomming connection from : %d.%d.%d.%d %d ", (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256), cliaddr.sin_port );

      if (sock == -1) {	

	fprintf(stderr,"init_instance: failed to accept socket\n");

      } else {
	
	if ( fcntl(sock, F_SETFL, O_NONBLOCK) != -1 ) {

	  flag = 1;
	  retval = setsockopt(sock,            
			      IPPROTO_TCP,     
			      TCP_NODELAY,     
			      &flag,  
			      sizeof(int));    
	  
	  if (retval == -1) { assert(false); }

	  //fprintf(stderr,"have connection on socket (%d)\n", sock);
	
	  netsocket socket(sock);

	  int data[3];

	  socket.read_chunk((char*)data, 12);
	  socket.write_chunk((char*)data, 12);

	  sock_dscr sd(data[0], data[1], data[2]);

	  map<sock_dscr, bool>::iterator i = in_done.find(sd);

	  if (i == in_done.end()) {
	  
	    fprintf(stderr,"Error: socket data is undefined. %d %d %d\n", data[0], data[1], data[2]);
	    close(sock);
	    
	  } else {

	    if ((*i).second == false) {
	  
	      //fprintf(stderr,"int pair FOUND!\n");
	      in_done[sd] = true;

	      in_sockets[sd] = new netsocket(sock);
	      //fprintf(stderr,"In Socket Added from:%d to:%d socket:%d\n", pair.from, pair.to, sock);

	      socks_accepted++;
	    } else {

	      fprintf(stderr,"Warning: socket data already seen.\n");
	      close(sock);
	      
	    }

	  }

	  if (socks_accepted >= in_connections.size()) { 
              
            if (debugging) {
              fprintf(stderr, "All incoming connections created.\n");
            }
                  
	    close(listenfd);

	    return 0;
	  }
	}
      }
    }
  }
}

void init_instance::close_sockets() {

  map<sock_dscr, mysocket*>::iterator i;

  fprintf(stderr,"Closing sockets...\n");

  for (i = in_sockets.begin(); i != in_sockets.end(); ++i) {
    ((*i).second)->close();
  }

  for (i = out_sockets.begin(); i != out_sockets.end(); ++i) {
    ((*i).second)->close();
  }

}

void init_instance::set_thread_usage(int thread, int usage)
{
  threadInfo[thread].t_usage = usage;
}

int init_instance::get_thread_usage(int thread)
{
  return threadInfo[thread].t_usage;
}

init_instance::Thread_info init_instance::return_thread_map()
{
  return threadInfo;
}

#endif // ARM

