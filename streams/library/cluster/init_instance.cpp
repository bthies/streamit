
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

vector<int_pair> init_instance::in_connections;
vector<int_pair> init_instance::out_connections;
vector<unsigned> init_instance::out_ip_addrs;

map<int_pair, bool> init_instance::in_done;

map<int_pair, int> init_instance::in_sockets;
map<int_pair, int> init_instance::out_sockets;

short init_instance::listen_port = 22222;

map<int, string> init_instance::thread_machines;


static void *accept_thread(void *param);


static void *accept_thread(void *param) {

  // this now locked my main thread
  //LOCK(&init_instance::accept_lock);

  init_instance::listen();

  UNLOCK(&init_instance::accept_lock);

}


void init_instance::read_config_file() {
  
  int node;
  char name[1024];

  FILE *f = fopen("cluster-config.txt", "r");

  for (;;) {
  
    fscanf(f, "%d %s", &node, name);

    if (feof(f)) break;

    string s(name);
    thread_machines[node] = s;
    printf("node:%d name:%s\n", node, name);

  }

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


void init_instance::add_incoming(int from, int to) {

  int_pair pair(from, to);
  in_connections.push_back(pair);
}


void init_instance::add_outgoing(int from, int to, unsigned to_ip_addr) {

  int_pair pair(from, to);
  out_connections.push_back(pair);
  out_ip_addrs.push_back(to_ip_addr);
}

void init_instance::initialize_sockets() {

  for (vector<int_pair>::iterator i = in_connections.begin(); i < in_connections.end(); ++i) {
  
    int_pair pair = *i;

    in_done[pair] = false;
    
  } 

  
  // create & run accept thread

  if (in_connections.size() > 0) {

    pthread_t id;
  
    LOCK(&accept_lock);

    pthread_create(&id, NULL, accept_thread, (void*)"Thread");
  
  }  


  // make connections to other hosts etc.

  int num = out_connections.size();

  vector<int_pair>::iterator i1 = out_connections.begin();
  vector<unsigned>::iterator i2 = out_ip_addrs.begin();

  for (int t = 0; t < num; t++) {
  
    int_pair pair = *i1;
    unsigned ip_addr = *i2;

    int data[2];
    
    data[0] = pair.from;
    data[1] = pair.to;

    mysocket *sock = NULL;

    while (sock == NULL) {
      sock = open_socket::connect(ip_addr, 22222);
      sleep(1);
    }

    //printf("socket connected !!\n");

    sock->write_chunk((char*)data, 8);
    sock->read_chunk((char*)data, 8);

    //printf("socket done: recieved reply %d %d !!\n", data[0], data[1]);

    out_sockets[pair] = sock->get_fd();

    printf("Out Socket Added from:%d to:%d socket:%d\n", pair.from, pair.to, sock->get_fd());

    ++i1;
    ++i2;

  }

  // wait for accept thread to finnish

  LOCK(&accept_lock);
  UNLOCK(&accept_lock);

  
}

int init_instance::get_incoming_socket(int from , int to) {

  int_pair pair(from, to);

  map<int_pair, int>::iterator i = in_sockets.find(pair);

  if (i == in_sockets.end()) {
    return -1;
  } else {
    return (*i).second;
  }
}

int init_instance::get_outgoing_socket(int from , int to) {

  int_pair pair(from, to);

  map<int_pair, int>::iterator i = out_sockets.find(pair);

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
  serveraddr.sin_port = htons(listen_port);

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

	  int data[2];

	  socket.read_chunk((char*)data, 8);
	  socket.write_chunk((char*)data, 8);

	  int_pair pair(data[0], data[1]);

	  map<int_pair, bool>::iterator i = in_done.find(pair);

	  if (i == in_done.end()) {
	  
	    printf("int pair not found!\n");
	    close(sock);
	    
	  } else {

	    if ((*i).second == false) {
	  
	      //printf("int pair FOUND!\n");
	      in_done[pair] = true;

	      in_sockets[pair] = sock;
	      printf("In Socket Added from:%d to:%d socket:%d\n", pair.from, pair.to, sock);

	      socks_accepted++;
	    } else {

	      printf("int pair already seen!\n");
	      close(sock);
	      
	    }

	  }

	  if (socks_accepted >= in_connections.size()) { 
	    
	    printf("All connections accepted!\n");
	    return 0;
	  }
	}
      }
    }
  }
}



