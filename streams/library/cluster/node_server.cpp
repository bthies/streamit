
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

      vector<int> resp;
     
      if (cmd == LIST_COMMAND) {

	resp = list();

      }

      if (cmd == PAUSE_COMMAND) {

	int thread_id = sock->read_int();
	resp = pause(thread_id);

      }

      if (cmd == RESTART_COMMAND) {
      
	int thread_id = sock->read_int();
	resp = restart(thread_id);

      }

      if (cmd == LIST_INCOMING_DATA_LINKS) {
      
	int thread_id = sock->read_int();
	resp = list_incoming_data_links(thread_id);

      }

      if (cmd == LIST_OUTGOING_DATA_LINKS) {
      
	int thread_id = sock->read_int();
	resp = list_outgoing_data_links(thread_id);
      }

      for (vector<int>::iterator iter = resp.begin();
	   iter < resp.end();
	   ++iter) {
	
	sock->write_int(*iter);
      }

    }
  }
}



vector<int> node_server::list() {
  
  vector<int> resp;

  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {

    thread_info *info = *iter;
    resp.push_back( info->get_thread_id() );
  }

  resp.push_back(-1);
  return resp;
}

vector<int> node_server::pause(int thread_id) {

  vector<int> resp;

  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {
    
    thread_info *info = *iter;

    if ( info->get_thread_id() == thread_id ) {

      *(info->get_state_flag()) = PAUSE_STATE;
	    
      resp.push_back(0);
      return resp;
    }
  }

  resp.push_back(-1);
  return resp;
}


vector<int> node_server::restart(int thread_id) {

  vector<int> resp;
  
  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {
    
    thread_info *info = *iter;
    
    if ( info->get_thread_id() == thread_id ) {
      
      *(info->get_state_flag()) = RUN_STATE;
      
      resp.push_back(0);
      return resp;
    }
  }

  resp.push_back(-1);
  return resp;
}


vector<int> node_server::list_incoming_data_links(int thread_id) {

  vector<int> resp;

  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {
    
    thread_info *info = *iter;
    
    if (info->get_thread_id() == thread_id) {
      
      vector<int> list = info->get_incoming_data_connections();
      
      for (vector<int>::iterator list_i = list.begin(); 
	   list_i < list.end(); 
	   ++list_i) {
	
	resp.push_back(*list_i);    
      }
    }
  }

  resp.push_back(-1);
  return resp;
}



vector<int> node_server::list_outgoing_data_links(int thread_id) {

  vector<int> resp;

  for (vector<thread_info*>::iterator iter = thread_list.begin();
       iter < thread_list.end();
       ++iter) {
    
    thread_info *info = *iter;

    if (info->get_thread_id() == thread_id) {

      vector<int> list = info->get_outgoing_data_connections();
      
      for (vector<int>::iterator list_i = list.begin(); 
	   list_i < list.end(); 
	   ++list_i) {
	
	resp.push_back(*list_i);    
      }
    }
  }

  resp.push_back(-1);
  return resp;
}



