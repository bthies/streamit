
#ifndef __NODE_SERVER_H
#define __NODE_SERVER_H

#include <thread_info.h>

#include <vector>
#include <unistd.h>

#define LIST_COMMAND 10

// parameters: none
// sample response "1", "2", "-1"

#define PAUSE_PROPER_COMMAND 20 // pause between iterations

// parameters: <thread_id>
// sample response: "0" - OK, "-1" - error

#define PAUSE_ANY_COMMAND 21 // pause between iterations or during I/O

// parameters: <thread_id>
// sample response: "0" - pasued between iterations
//                  "1" - paused during I/O operation
//                  "-1" - error

#define RESUME_COMMAND 30

// parameters: <thread_id>
// sample response: "0" - OK, "-1" - error

#define LIST_INCOMING_DATA_LINKS 40

// parameters: <thread_id>
// sample response "1", "2", "-1"

#define LIST_OUTGOING_DATA_LINKS 50

// parameters: <thread_id>
// sample response "1", "2", "-1"


class node_server {

  vector <thread_info*> thread_list;

  vector<int> list();
  vector<int> pause_proper(int id);
  vector<int> pause_any(int id);
  vector<int> resume(int id);
  vector<int> list_incoming_data_links(int id);
  vector<int> list_outgoing_data_links(int id);

 public:
  
  node_server(vector <thread_info*> list);

  void run_server();

};


#endif




