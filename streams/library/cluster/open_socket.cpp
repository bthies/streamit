
#include <open_socket.h>

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


netsocket *open_socket::listen(short port) {

  int listenfd;
  int retval;
  int flag;
  int sock;

  struct sockaddr_in serveraddr;

  listenfd = ::socket(AF_INET, SOCK_STREAM, 0);
  if (listenfd == -1) {
    
    return NULL;
  }

  retval = setsockopt(listenfd, SOL_SOCKET, SO_REUSEADDR,
			  (char*)&flag, sizeof flag);
  if (retval == -1) {
  
    return NULL;
  }

  bzero((char *)&serveraddr, sizeof(serveraddr));
  serveraddr.sin_family = AF_INET;
  serveraddr.sin_addr.s_addr = htonl(INADDR_ANY);
  serveraddr.sin_port = htons(port);

  retval = bind(listenfd, (struct sockaddr *)&serveraddr,
                sizeof(serveraddr));

  if (retval == -1) {
  
    return NULL;
  }

  retval = ::listen(listenfd, 10);

  if (retval == -1) {

    return NULL;
  }

  retval = fcntl(listenfd, F_SETFL, O_NONBLOCK);

  if (retval == -1) {

    return NULL;
  }

  //printf("Socket bound and listening....done\n");
  

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

      //printf("Incomming connection from : %d.%d.%d.%d %d ", 
      //     (ip % 256), 
      //     ((ip>>8) % 256), 
      //     ((ip>>16) % 256), 
      //     ((ip>>24) % 256), 
      //     cliaddr.sin_port );

      ::close(listenfd); // closing the listen socket


      if (sock == -1) {	

	printf("failed\n");

	return NULL;

      } else {
	
	if ( fcntl(sock, F_SETFL, O_NONBLOCK) == -1 ) {

	  return NULL;
	}

	//printf("done (%d)\n", sock);

	return new netsocket(sock);
      }
    }
  }
}



netsocket *open_socket::connect(unsigned ipaddr, short port) {

  int sock;
  int retval;

  struct sockaddr_in sa;

  sock = ::socket(AF_INET, SOCK_STREAM, 0);
  if (sock == -1) {

    return NULL; // Could not create socket
  }

  int w_size = 256 * 1000;
  setsockopt(sock, SOL_SOCKET, SO_SNDBUF, (char*)&w_size, sizeof w_size);
  setsockopt(sock, SOL_SOCKET, SO_RCVBUF, (char*)&w_size, sizeof w_size);

  sa.sin_family = AF_INET;
  sa.sin_port = htons(port);
  sa.sin_addr.s_addr = ipaddr;

  //printf("Tyring to connect....");
  fflush(stdout);

  if (::connect(sock, (struct sockaddr *)&sa, sizeof(sa))) {

    printf("failed to connect to ");
    print_ip(stdout, ipaddr);
    printf("\n");
    return NULL; // Could not connect to the host
  }

  retval = fcntl(sock, F_SETFL, O_NONBLOCK);

  if (retval == -1) {

    return NULL;
  }

  //printf("done\n");
 
  return new netsocket(sock);
}




