
#include <init_instance.h>
#include <open_socket.h>
#include <mysocket.h>

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

pthread_mutex_t init_instance::accept_lock = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t init_instance::bind_lock = PTHREAD_MUTEX_INITIALIZER;

vector<sock_dscr> init_instance::in_connections;
vector<sock_dscr> init_instance::out_connections;

map<sock_dscr, bool> init_instance::in_done;
map<sock_dscr, bool> init_instance::out_done;

map<sock_dscr, int> init_instance::in_sockets;
map<sock_dscr, int> init_instance::out_sockets;

short init_instance::listen_port = 22222;

map<int, string> init_instance::thread_machines;


static void *accept_thread(void *param);


static void *accept_thread(void *param) {

  // this now locked my main thread
  //LOCK(&init_instance::accept_lock);

  if (init_instance::listen() == -1) {
    exit(-1);
  }

  UNLOCK(&init_instance::accept_lock);

}


void init_instance::read_config_file() {
  
  int node;
  char name[1024];

  FILE *f = fopen("cluster-config.txt", "r");

  printf("Reading cluster config file...\n");

  for (;;) {
  
    fscanf(f, "%d %s", &node, name);

    if (feof(f)) break;

    string s(name);
    thread_machines[node] = s;
    printf("thread:%d machine:%s\n", node, name);

  }

  printf("\n");
  
  fclose(f);
}

char* init_instance::get_node_name(int node) {

  map<int, string>::iterator i = thread_machines.find(node);

  if (i == thread_machines.end()) {
    return NULL;
  } else {
    return (char*)(*i).second.c_str();
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

  // create pipes where applicable

  printf("Creating kernel level pipes");
  fflush(stdout);
  
  for (vector<sock_dscr>::iterator i = out_connections.begin(); i < out_connections.end(); ++i) {
  
    sock_dscr sd = *i;

    map<sock_dscr, bool>::iterator i = in_done.find(sd);

    if (i != in_done.end()) {
    
      // create pipe

      //printf("Creataing pipe for socket %d %d %d\n", sd.from, sd.to, sd.type);
      printf(".");
      fflush(stdout);
      
      int pfd[2];
      int retval = pipe(pfd);
      if (retval != 0) perror("pipe");

      out_sockets[sd] = pfd[1];
      in_sockets[sd] = pfd[0];
      
      out_done[sd] = true;
      in_done[sd] = true;
    }
  }

  printf("done\n");

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
  
    LOCK(&accept_lock);
    LOCK(&bind_lock);

    pthread_create(&id, NULL, accept_thread, (void*)"Thread");
  
  }  

  LOCK(&bind_lock);

  // make connections to other hosts etc.

  int num = out_connections.size();

  vector<sock_dscr>::iterator i1 = out_connections.begin();

  for (int t = 0; t < num; t++) {
  
    sock_dscr sd = *i1;
    unsigned ip_addr = lookup_ip(init_instance::get_node_name(sd.to));

    int data[3];
    
    data[0] = sd.from;
    data[1] = sd.to;
    data[2] = sd.type;

    mysocket *sock = NULL;

    while (sock == NULL) {
      sock = open_socket::connect(ip_addr, 22222);

      if (sock == NULL) {
	printf("Trying again ...\n");
	sleep(1);     
      }
    }

    //printf("socket connected !!\n");

    sock->write_chunk((char*)data, 12);
    sock->read_chunk((char*)data, 12);

    //printf("socket done: recieved reply %d %d !!\n", data[0], data[1]);

    out_sockets[sd] = sock->get_fd();

    //printf("Out Socket Added from:%d to:%d socket:%d\n", pair.from, pair.to, sock->get_fd());

    ++i1;

  }

  printf("All outgoing connections created!\n");

  // wait for accept thread to finnish

  LOCK(&accept_lock);
  UNLOCK(&accept_lock);

  printf("\n");
  
}

int init_instance::get_incoming_socket(int from, int to, int type) {

  sock_dscr sd(from, to, type);

  map<sock_dscr, int>::iterator i = in_sockets.find(sd);

  if (i == in_sockets.end()) {
    return -1;
  } else {
    return (*i).second;
  }
}

int init_instance::get_outgoing_socket(int from , int to, int type) {

  sock_dscr sd(from, to, type);

  map<sock_dscr, int>::iterator i = out_sockets.find(sd);

  if (i == out_sockets.end()) {
    return -1;
  } else {
    return (*i).second;
  }
}


int init_instance::listen() {

  int socks_accepted = 0;

  int listenfd;
  int retval;
  int flag;
  int sock;

  struct sockaddr_in serveraddr;

  listenfd = ::socket(AF_INET, SOCK_STREAM, 0);
  if (listenfd == -1) {

    perror("socket()");
    return -1;
  }

  flag = 1;

  retval = setsockopt(listenfd, SOL_SOCKET, SO_REUSEADDR,
			  (char*)&flag, sizeof flag);
  if (retval == -1) {
  
    perror("setsockopt()");
    return -1;
  }

  bzero((char *)&serveraddr, sizeof(serveraddr));
  serveraddr.sin_family = AF_INET;
  serveraddr.sin_addr.s_addr = htonl(INADDR_ANY);
  serveraddr.sin_port = htons(listen_port);

  retval = bind(listenfd, (struct sockaddr *)&serveraddr,
                sizeof(serveraddr));

  if (retval == -1) {
  
    perror("bind()");
    return -1;
  }

  retval = ::listen(listenfd, 10);

  if (retval == -1) {

    perror("listen()");
    return -1;
  }

  retval = fcntl(listenfd, F_SETFL, O_NONBLOCK);

  if (retval == -1) {

    perror("fcntl()");
    return -1;
  }

  printf("Socket bound and listening....done\n");
  
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

      //printf("Accepting connection....");
      fflush(stdout);

      sock = accept(listenfd, (struct sockaddr *)&cliaddr, &clilen);

      unsigned ip = cliaddr.sin_addr.s_addr;

      //printf("Incomming connection from : %d.%d.%d.%d %d ", (ip % 256), ((ip>>8) % 256), ((ip>>16) % 256), ((ip>>24) % 256), cliaddr.sin_port );

      if (sock == -1) {	

	printf("failed to accept socket\n");

      } else {
	
	if ( fcntl(sock, F_SETFL, O_NONBLOCK) != -1 ) {

	  //printf("have connection on socket (%d)\n", sock);
	
	  mysocket socket(sock);

	  int data[3];

	  socket.read_chunk((char*)data, 12);
	  socket.write_chunk((char*)data, 12);

	  sock_dscr sd(data[0], data[1], data[2]);

	  map<sock_dscr, bool>::iterator i = in_done.find(sd);

	  if (i == in_done.end()) {
	  
	    printf("error: socket data is undefined! %d %d %d\n", data[0], data[1], data[2]);
	    close(sock);
	    
	  } else {

	    if ((*i).second == false) {
	  
	      //printf("int pair FOUND!\n");
	      in_done[sd] = true;

	      in_sockets[sd] = sock;
	      //printf("In Socket Added from:%d to:%d socket:%d\n", pair.from, pair.to, sock);

	      socks_accepted++;
	    } else {

	      printf("Warning! socket data already seen!\n");
	      close(sock);
	      
	    }

	  }

	  if (socks_accepted >= in_connections.size()) { 
	    
	    printf("All incoming connections created!\n");

	    close(listenfd);

	    return 0;
	  }
	}
      }
    }
  }
}

void init_instance::close_sockets() {

  map<sock_dscr, int>::iterator i;

  printf("Closing sockets...\n");

  for (i = in_sockets.begin(); i != in_sockets.end(); ++i) {
    close((*i).second);
  }

  for (i = out_sockets.begin(); i != out_sockets.end(); ++i) {
    close((*i).second);
  }

}


