
#include <node_server.h>

#include <mysocket.h>
#include <open_socket.h>

#define LIST_COMMAND 1

// parameters: NONE
// response "1", "2", "-1"

#define PAUSE_COMMAND 2

// parameters: "1"
// response: "0" - OK, "-1" - error

#define RESTART_COMMAND 3

// parameters: "1"
// response: "0" - OK, "-1" - error


node_server::node_server(thread_list_element *thread_list_top) {
  thread_top = thread_list_top;
}

void node_server::run_server() {

  mysocket *sock;
  int cmd, par1;
  
  for (;;) {

    //printf("open_socket::listen()\n");

    sock = open_socket::listen(22223);
    if (sock == NULL) return;
    
    for (;;) {
      cmd = sock->read_int();
      
      if (sock->eof()) break;
      
      if (cmd == 1) { // LIST_THREADS
	for (thread_list_element *tmp = thread_top;
	     tmp != NULL; 
	     tmp = tmp->get_next()) {
	  
	  sock->write_int(tmp->get_thread_id());
	}
	sock->write_int(-1);
      }
      
    }
  }

}
