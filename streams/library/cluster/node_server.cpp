
#include <node_server.h>

#include <mysocket.h>
#include <open_socket.h>
#include <thread_info.h>


node_server::node_server(vector <thread_info*> list) {
  thread_list = list;
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

	for (vector<thread_info*>::iterator iter = thread_list.begin();
	     iter < thread_list.end();
	     ++iter) {

	  thread_info *info = *iter;

	  sock->write_int( info->get_thread_id() );

	}

	sock->write_int(-1);
      }

      if (cmd == PAUSE_COMMAND) {
	int thread_id = sock->read_int();

	for (vector<thread_info*>::iterator iter = thread_list.begin();
	     iter < thread_list.end();
	     ++iter) {

	  thread_info *info = *iter;

	  if ( info->get_thread_id() == thread_id ) {

	    *(info->get_state_flag()) = PAUSE_STATE;
	  }
	}
      }

      if (cmd == RESTART_COMMAND) {
	int thread_id = sock->read_int();

	for (vector<thread_info*>::iterator iter = thread_list.begin();
	     iter < thread_list.end();
	     ++iter) {

	  thread_info *info = *iter;

	  if ( info->get_thread_id() == thread_id ) {

	    *(info->get_state_flag()) = RUN_STATE;
	  }
	}
      }
      
    }
  }

}




