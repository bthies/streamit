
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
  printf("commands:\n");
  printf("  connect <hostname>\n");
  printf("  pause   <thread_id>\n");
  printf("  restart <thread_id>\n");
  printf("\n");
}

node_info *master_server::connect(unsigned ip) {
  
  mysocket *socket = open_socket::connect(ip, 22223);  
  if (socket == NULL) { return NULL; } 
  node_info *node = new node_info(ip, socket);
  return node;
}

vector<int> *master_server::list(node_info *node) {
  
  vector<int> *threads = new vector<int>();
  mysocket *socket = node->get_socket();
  socket->write_int(LIST_COMMAND); // list threads
  
  for (;;) {
    int id;
    id = socket->read_int();
    if (id == -1) break;
    threads->push_back(id);
  }
  
  return threads;
}


void master_server::pause(node_info *node, int id) {
  
  mysocket *socket = node->get_socket();  
  socket->write_int(PAUSE_COMMAND);
  socket->write_int(id);
}


void master_server::restart(node_info *node, int id) {
  
  mysocket *socket = node->get_socket();  
  socket->write_int(RESTART_COMMAND);
  socket->write_int(id);
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
      
      vector<int> *threads = list(node);
      
      printf("  threads found: ");
      
      for (vector<int>::iterator i = threads->begin();
	   i < threads->end();
	   ++i) {
	
	int thread_id = *i;
	
	printf("%d ", thread_id);
	node_map[thread_id] = node; 
	
      }
      
      printf("\n");      
      nodes.push_back(node);
      delete threads;
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
      
      pause(node, id);
      printf("Ok\n");	
      
    }      
  }

  if (clen >= 8 && strncmp("restart ", cmd, 8) == 0) {
    
    char *rest = cmd + 8;
    int id;
    sscanf(rest, "%d", &id);
    
    node_info *node = node_map[id];
    
    if (node == NULL) {
      
      printf("ERROR: Thread not found.\n");
      
    } else {
      
      restart(node, id);
      printf("Ok\n");	
      
    }      
  }  
}
