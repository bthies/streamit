
#include <node_server.h>

#include <mysocket.h>
#include <open_socket.h>
#include <thread_list_element.h>


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
      
      if (cmd == LIST_COMMAND) {
 	for (thread_list_element *tmp = thread_top;
	     tmp != NULL; 
	     tmp = tmp->get_next()) {
	  
	  sock->write_int(tmp->get_thread_id());
	}
	sock->write_int(-1);
      }

      if (cmd == PAUSE_COMMAND) {
	int thread_id = sock->read_int();

	for (thread_list_element *tmp = thread_top;
	     tmp != NULL; 
	     tmp = tmp->get_next()) {

	  if (tmp->get_thread_id() == thread_id) {

	    (*tmp->get_state_flag()) = PAUSE_STATE;
	  }
	}
      }

      if (cmd == RESTART_COMMAND) {
	int thread_id = sock->read_int();

	for (thread_list_element *tmp = thread_top;
	     tmp != NULL; 
	     tmp = tmp->get_next()) {

	  if (tmp->get_thread_id() == thread_id) {

	    (*tmp->get_state_flag()) = RUN_STATE;
	  }
	}
      }
      
    }
  }

}
