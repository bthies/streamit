
#include <master_server.h>
  
node_info::node_info(unsigned ip, mysocket *socket) {
  this->ip = ip;
  this->socket = socket;
}

unsigned node_info::get_ip() { 
  return ip; 
}

mysocket *node_info::get_socket() { 
  return socket; 
}


void master_server::print_commands() {
  printf("\n");
  printf("commands:\n");
  printf("  connect   <hostname>\n");
  printf("\n");
  printf("  pause     <thread_id> - pause a thread between iterations (might\n");
  printf("                          deadlock if pause more than one thread)\n");
  printf("  pause-any <thread_id> - pause a thread between iterations or\n");
  printf("                          during an I/O operation\n");
  printf("  resume    <thread_id> - resume a thread\n");
  printf("\n");
  printf("  indata <thread_id>    - lists incoming data links\n");
  printf("  outdata <thread_id>   - lists outbound data links\n");
  printf("\n");
}

node_info *master_server::connect(unsigned ip) {
  
  mysocket *socket = open_socket::connect(ip, 22223);  
  if (socket == NULL) { return NULL; } 
  node_info *node = new node_info(ip, socket);

  vector<int> threads = list(node);

  for (vector<int>::iterator i = threads.begin();
       i < threads.end();
       ++i) {
	
    int thread_id = *i;	
    node_map[thread_id] = node; 	
  }

  nodes.push_back(node);
  return node;
}

vector<int> master_server::list(node_info *node) {
  
  vector<int> threads;
  mysocket *socket = node->get_socket();
  socket->write_int(LIST_COMMAND); // list threads
  
  for (;;) {
    int id;
    id = socket->read_int();
    if (id == -1) break;
    threads.push_back(id);
  }
  
  return threads;
}


int master_server::pause_proper(node_info *node, int id) {
  
  mysocket *socket = node->get_socket();  
  socket->write_int(PAUSE_PROPER_COMMAND);
  socket->write_int(id);

  return socket->read_int();
}


int master_server::pause_any(node_info *node, int id) {
  
  mysocket *socket = node->get_socket();  
  socket->write_int(PAUSE_ANY_COMMAND);
  socket->write_int(id);

  return socket->read_int();
}


int master_server::resume(node_info *node, int id) {
  
  mysocket *socket = node->get_socket();  
  socket->write_int(RESUME_COMMAND);
  socket->write_int(id);

  return socket->read_int();
}

vector<int> master_server::indata(node_info *node, int id) {

  vector<int> threads;

  mysocket *socket = node->get_socket();  
  socket->write_int(LIST_INCOMING_DATA_LINKS);
  socket->write_int(id);

  for (;;) {
    int a = socket->read_int();
    if (a == -1) break;
    threads.push_back(a);
  }

  return threads;
}


vector<int> master_server::outdata(node_info *node, int id) {

  vector<int> threads;

  mysocket *socket = node->get_socket();  
  socket->write_int(LIST_OUTGOING_DATA_LINKS);
  socket->write_int(id);

  for (;;) {
    int a = socket->read_int();
    if (a == -1) break;
    threads.push_back(a);
  }

  return threads;
}

  
void master_server::process_command(char *cmd) {
  
  int clen = strlen(cmd);
  
  if (clen >= 8 && strncmp("connect ", cmd, 8) == 0) {
    
    char *host = cmd + 8;
    unsigned ip = lookup_ip(host);
    node_info *node = connect(ip);

    if (node == NULL) {
      
      printf("ERROR: Connection failed.\n");
      
    } else {
      
      printf("Connected to %s\n", host);
      
      vector<int> threads = list(node);
      
      printf("  threads found: ");
      
      for (vector<int>::iterator i = threads.begin();
	   i < threads.end();
	   ++i) {
	
	printf("%d ", *i);
      }
      
      printf("\n");      
    }
  } 

  if (clen >= 6 && strncmp("pause ", cmd, 6) == 0) {
    
    char *rest = cmd + 6; 
    int id;
    sscanf(rest, "%d", &id);
    
    node_info *node = node_map[id];
    
    if (node == NULL) {
      
      printf("ERROR: Thread not found.\n");
      
    } else {

      int retval;
      retval = pause_proper(node, id);
      printf("retval: %d\n", retval);	
      
    }      
  }


  if (clen >= 10 && strncmp("pause-any ", cmd, 10) == 0) {
    
    char *rest = cmd + 10; 
    int id;
    sscanf(rest, "%d", &id);
    
    node_info *node = node_map[id];
    
    if (node == NULL) {
      
      printf("ERROR: Thread not found.\n");
      
    } else {

      int retval;
      retval = pause_any(node, id);
      printf("retval: %d\n", retval);	
      
    }      
  }

  if (clen >= 7 && strncmp("resume ", cmd, 7) == 0) {
    
    char *rest = cmd + 7;
    int id;
    sscanf(rest, "%d", &id);
    
    node_info *node = node_map[id];
    
    if (node == NULL) {
      
      printf("ERROR: Thread not found.\n");
      
    } else {
      
      int retval;
      retval = resume(node, id);
      printf("retval: %d\n", retval);	
      
    }      
  }  


  if (clen >= 7 && strncmp("indata ", cmd, 7) == 0) {
  
    char *rest = cmd + 7;
    int id;
    sscanf(rest, "%d", &id);

    node_info *node = node_map[id];

    if (node == NULL) {
      
      printf("ERROR: Thread not found.\n");
      
    } else {

      vector<int> threads = indata(node, id);
      
      printf("  incoming links from: ");
      
      for (vector<int>::iterator i = threads.begin();
	   i < threads.end();
	   ++i) {
	
	printf("%d ", *i);
      }
      
      printf("\n");      
      
    }
  }



  if (clen >= 8 && strncmp("outdata ", cmd, 8) == 0) {
  
    char *rest = cmd + 7;
    int id;
    sscanf(rest, "%d", &id);

    node_info *node = node_map[id];

    if (node == NULL) {
      
      printf("ERROR: Thread not found.\n");
      
    } else {

      vector<int> threads = outdata(node, id);
      
      printf("  outbound links to: ");
      
      for (vector<int>::iterator i = threads.begin();
	   i < threads.end();
	   ++i) {
	
	printf("%d ", *i);
      }
      
      printf("\n");      
      
    }
  }


}
