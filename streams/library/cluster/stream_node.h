/*
 * Copyright 2006 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */
#ifndef __STREAM_NODE_H
#define __STREAM_NODE_H

#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <pthread.h>
#include <vector>

#include <init_instance.h>
#include <object_write_buffer.h>
#include <thread_info.h>
#include <consumer3.h>
#include <producer3.h>
#include <netsocket.h>
#include <message.h>
#include <timer.h>
#include <sdep.h>

using namespace std;

template <class IN, class OUT>
class stream_node {

 protected:

  vector<int> dummy;

  int node_id, node_count;
  int pop_rate, peek_rate, push_rate;

  vector<int> inputs, outputs;
  vector<int> msg_in, msg_out;

  consumer3<IN> **consumer_array;
  producer3<OUT> **producer_array;
  
  int *pop_rates;
  int *push_rates;

  netsocket **msg_in_socks;
  netsocket **msg_out_socks;

  int *credits_sent;

  message *msg_stack;

  int credit;
  bool need_credit;

  thread_info *info;

  // ==== checkpoint & rollback

  object_write_buffer cpoint;
  int cpoint_iter;
  int cpoint_credit;

  bool need_rollback;
  int max_src_iter;
  int last_max_src_iter;

  // ==== filter

  IN pop_buf[10000];
  int pop_head;
  int pop_tail;

  consumer3<IN> *cons;
  producer3<OUT> *prod;

 public:

  int iteration;

  stream_node(int id, 
	      int node_count, 
	      int pop_rate,
	      int peek_rate,
	      int push_rate) 
    { 
      this->node_id = id; 
      this->node_count = node_count; 
      this->pop_rate = pop_rate;
      this->peek_rate = peek_rate;
      this->push_rate = push_rate;
      this->info = NULL;
      msg_stack = NULL;
      iteration = 0;
      credit = 0;
      need_credit = false;

      max_src_iter = 0;
      last_max_src_iter = 0;
      need_rollback = false;

      // ==== filter
      pop_head = 0;
      pop_tail = 0;
      cons = NULL;
      prod = NULL;

      pop_rates = (int*)malloc(sizeof(int)*node_count);
      push_rates = (int*)malloc(sizeof(int)*node_count);
      credits_sent = (int*)malloc(sizeof(int)*node_count);
  }

  void init_stream() {
    int size = sizeof(int*)*node_count;
    consumer_array = (consumer3<IN>**)malloc(size);
    producer_array = (producer3<OUT>**)malloc(size);
    msg_in_socks = (netsocket**)malloc(size);
    msg_out_socks = (netsocket**)malloc(size);

  }

  void clone_stream(stream_node<IN, OUT> *orig) {
    // creates an incomplete clone that can be used for 
    // sending messages from a historic state
    
    msg_out_socks = orig->msg_out_socks;
    iteration = orig->iteration;
  }

  void set_need_credit() { need_credit = true; }

  void add_input(int id) { 
    inputs.push_back(id); 
    pop_rates[id] = pop_rate;
  }

  void add_output(int id) { 
    outputs.push_back(id); 
    push_rates[id] = push_rate;
  }

  void add_input_rate(int id, int rate) { 
    inputs.push_back(id); 
    pop_rates[id] = rate;
  }

  void add_output_rate(int id, int rate) { 
    outputs.push_back(id); 
    push_rates[id] = rate;
  }
  
  void add_msg_in(int id) 
    { msg_in.push_back(id); }

  void add_msg_out(int id) 
    { 
      msg_out.push_back(id); 
      credits_sent[id] = 0;
    }

  void declare_sockets() 
    {
      vector<int>::iterator i;
      for (i = inputs.begin(); i != inputs.end(); ++i) {
	init_instance::add_incoming(*i, node_id, DATA_SOCKET);
      }
      for (i = outputs.begin(); i != outputs.end(); ++i) {
	init_instance::add_outgoing(node_id , *i, DATA_SOCKET);
      }
      for (i = msg_in.begin(); i != msg_in.end(); ++i) {
	init_instance::add_incoming(*i, node_id, MESSAGE_SOCKET);
      }
      for (i = msg_out.begin(); i != msg_out.end(); ++i) {
	init_instance::add_outgoing(node_id , *i, MESSAGE_SOCKET);
      }
    }

  void init_sockets() 
    {
      mysocket *sock;

      vector<int>::iterator i;

      //printf("[%d]outputs.size()=%d\n", node_id, outputs.size());

      for (i = inputs.begin(); i != inputs.end(); ++i) {
	consumer3<IN> *c = new consumer3<IN>();
	sock = init_instance::get_incoming_socket(*i, node_id, DATA_SOCKET);
	c->set_socket(sock);
	c->init();
	consumer_array[*i] = c;
      }
      for (i = outputs.begin(); i != outputs.end(); ++i) {
	producer3<OUT> *p = new producer3<OUT>();
	sock = init_instance::get_outgoing_socket(node_id, *i, DATA_SOCKET);
	p->set_socket(sock);
	p->init();
	producer_array[*i] = p;
      }
      
      for (i = msg_in.begin(); i != msg_in.end(); ++i) {
	msg_in_socks[*i] = (netsocket*)
	  init_instance::get_incoming_socket(*i, node_id, MESSAGE_SOCKET);
      }
      for (i = msg_out.begin(); i != msg_out.end(); ++i) {
	msg_out_socks[*i] = (netsocket*)
	  init_instance::get_outgoing_socket(node_id , *i, MESSAGE_SOCKET);
      }
    }

  inline consumer3<IN> *get_consumer(int id) 
    { return consumer_array[id]; }
  
  inline producer3<OUT> *get_producer(int id) 
    { return producer_array[id]; }

  inline netsocket *get_msg_out_socket(int id) 
    { return msg_out_socks[id]; }
  
  void peek_sockets() 
    {
      vector<int>::iterator i;
      for (i = inputs.begin(); i != inputs.end(); ++i) {
	consumer_array[*i].peek(0);
      }
    }
  
  void flush_sockets() 
    {
      vector<int>::iterator i;
      for (i = inputs.begin(); i != inputs.end(); ++i) {
	consumer_array[*i]->delete_socket_obj();
      }
      for (i = outputs.begin(); i != outputs.end(); ++i) {
	producer3<OUT> *p = producer_array[*i];
	p->flush();
	p->get_socket()->close();
	p->delete_socket_obj();
      }
    }

  thread_info *get_thread_info() 
    {
      if (this->info != NULL) return this->info;
      this->info = new thread_info(node_id, NULL);
      // should add data & msg connections to info
      // should save state flag
      return this->info;
    }

  inline void check_messages() 
    {

      vector<int>::iterator i;
      for (i = msg_in.begin(); i != msg_in.end(); ++i) {
	netsocket *sock = msg_in_socks[*i];
	while (sock->data_available()) {
	  int size = sock->read_int();

	  //printf("==================== msg size: %d ===========\n", size);

	  if (size == -1) { // credit message
	    int c = sock->read_int();

	    //printf("got credit [%d]\n", c);

	    credit = c;
 
	  } else {
	    int index = sock->read_int();
	    int src_iter = sock->read_int();
	    int target = sock->read_int();

	    if (src_iter > max_src_iter) max_src_iter = src_iter;

	    //printf("got message! iter: %d src_iter: %d target: %d\n", iteration, src_iter, target);

	    //printf("last_max = %d max_iter = %d\n", last_max_src_iter, max_src_iter);

	    message *msg = new message(size, index, target);
	    msg->read_params(sock, 16);

	    //printf("read params done!\n");

	    if (iteration > 0) {
	      msg_stack = msg->push_on_stack(msg_stack);
	    } else {
	      exec_message(msg);
	      delete msg;
	    }
	  }
	}
      }

      message *msg, *last = NULL;
      for (msg = msg_stack; msg != NULL; msg = msg->next) {
	if (msg->execute_at < iteration) {

	  //if (need_rollback == false) printf("Need rollback i:%d\n", iteration);
	  
	  need_rollback = true;

	} else if (msg->execute_at == iteration) {
	  //printf("exec message at iter: %d\n", iteration);

	  exec_message(msg);
	  if (last != NULL) { 
	    last->next = msg->next;
	  } else {
	    msg_stack = msg->next;
	  }
	  delete msg;
	} else { last = msg; }
      } 
    }

  inline void send_credit(int to, int credit) {

    //get_msg_out_socket(to)->write_int(-1);
    // get_msg_out_socket(to)->write_int(credit);

    //printf("call send credit [%d]\n", credit);

    if (credit >= credits_sent[to]) {

      //credit += 1023; // speculative extra credit

      get_msg_out_socket(to)->write_int(-1);
      get_msg_out_socket(to)->write_int(credit);
      
      //printf("[%d] sent credit: %d iter: %d\n", node_id, credit, iteration);
      
      credits_sent[to] = credit;
    }
  }

  /*
  IN pop_buf[1024];
  int head, tail;

  inline IN filter_peek(int offs) { 
    return pop_buf[(tail+offs)&1023]; 
  }
  inline IN filter_pop() {
    IN res = pop_buf[tail];
    tail=(tail+1)&1023;
    return res;
  }

  consumer3<IN> *input;
  */

  void do_cpoint() {
    cpoint.erase();
    save_state(&cpoint);
    cpoint_iter = iteration;
    cpoint_credit = credit;
    //printf("[%d] do-checkpoint iter=%d\n", node_id, iteration);
  }

  void restore_cpoint() {
    cpoint.set_read_offset(0);
    load_state(&cpoint);
    iteration = cpoint_iter;
    credit = cpoint_credit;
  }

  virtual void work() = 0;
  virtual void work_n(int) = 0;
  virtual void init_state() = 0;

  virtual int state_size() = 0;
  virtual void save_state(object_write_buffer *buf) = 0;
  virtual void load_state(object_write_buffer *buf) = 0;

  virtual void send_credits() {}
  virtual void exec_message(message *msg) {}

  inline IN pop() { 
    return cons->pop();
    //return pop_buf[pop_tail++]; 
  }
  inline IN peek(int offs) { 
    return cons->peek(offs);
    //return pop_buf[pop_tail+offs]; 
  }
  inline void push(OUT data) { prod->push(data); }

  inline void init_pop_buf_simple() {}
  inline void update_pop_buf_simple() {
    cons->pop_items((IN*)pop_buf, pop_rate);
    pop_tail = 0;
  }

  inline void update_pop_buf_simple(int n) {
    cons->pop_items((IN*)pop_buf, pop_rate * n);
    pop_tail = 0;
  }

  virtual void init_filter() {
    if (inputs.size() == 1) cons = get_consumer(*inputs.begin());
    if (outputs.size() == 1) prod = get_producer(*outputs.begin());
  }

  /*
  void run(int num) {
    timer t;

    init_sockets();
    init_filter();
    init_state();

    send_credits();

    init_pop_buf_simple();
    //update_pop_buf_simple();
    
    //printf("need credit: %d\n", need_credit);

    if (need_credit) { 
      while (credit == 0) check_messages();
    }
    
    //work_1();iteration++;num--;
    
    t.start();

    int saved_credit = -1;

    max_src_iter = 0;
    last_max_src_iter = 0;

    if (inputs.size() == 0) {

      for (; iteration <= num;) {

	check_messages();

	while (need_credit && credit <= iteration) check_messages();

	saved_credit = -1;

	need_rollback = false;
	check_messages();

	if (need_rollback && max_src_iter > last_max_src_iter) {
	  saved_credit = credit; // save the maximum credit!!
	  restore_cpoint();
	  //printf("restoring state from a checkpoint! iter=%d\n", iteration);
	  producer_array[*outputs.begin()]->dec_frame();
	} else {
	  do_cpoint();

	  // get enough input!
	}

	last_max_src_iter = max_src_iter; 
	max_src_iter = 0;

	while (credit > iteration && iteration <= num) {
	  //send_credits();
	  update_pop_buf_simple();
	  check_messages();

	  work();
	  iteration++;
	}

	//printf("saved_credit = %d\n", saved_credit);
	if (saved_credit > 0) credit = saved_credit; // restore credit!

	producer_array[*outputs.begin()]->flush();
	producer_array[*outputs.begin()]->inc_frame();


      }
    } 

    if (inputs.size() == 1) {

      int curr_frame = -1;

      //do_cpoint();

      for (; iteration <= num;) {

	check_messages();
	while (need_credit && credit <= iteration) check_messages();

	consumer3<IN> *cons = consumer_array[*inputs.begin()];
	cons->read_frame();

	if (cons->get_frame_id() == curr_frame) {
	  restore_cpoint();
	} else {
	  curr_frame = cons->get_frame_id();
	  do_cpoint();
	}

	int nn = cons->get_size() / pop_rate;
	//printf("[nn = %d]\n", nn); 

	for (int y = 0; y < nn; y++) {
	  send_credits();
	  update_pop_buf_simple();
	  
	  work();
	  iteration++;
	}

	//printf("[done nn]\n", nn); 

	//send_credits();
      }
    } 

    t.stop();

    printf("Thread-%d %s %s\n", 
	   node_id, 
	   t.get_str(),
	   outputs.size() == 0 ? "<== NO OUTPUT" : "");

    flush_sockets();
    pthread_exit(NULL);
  }
  */

  
  void run_send(int num) {
    timer t;

    init_sockets();
    init_filter();
    init_state();

    init_pop_buf_simple();
    //update_pop_buf_simple();
    
    if (need_credit) { 
      while (credit == 0) check_messages();
    }
    
    t.start();

    int curr_frame = -1;

    int nn = -1;

    while (iteration < num) {

      //check_messages();
      //while (need_credit && credit <= iteration) check_messages();
      
      send_credits();
      cons->read_frame();
      
      int id = cons->get_frame_id();

      if (id == -1) {
	// do nothing!
	//assert(false);


      } else if (id == curr_frame) {

	//assert(nn > 0);
	//if (prod != NULL) prod->rollback(nn * push_rate);

	restore_cpoint();
      } else {

	curr_frame = cons->get_frame_id();
	do_cpoint();
      }

      nn = cons->get_size() / pop_rate;
      //printf("[nn = %d]\n", nn); 

      /*
      if (prod != NULL) {
	if (prod->get_size() + push_rate * nn > PRODUCER_BUFFER_SIZE) {
	  prod->flush();
	  prod->inc_frame();
	}
      }
      */

      for (int y = 0; y < nn; y++) {
	//send_credits();
	//update_pop_buf_simple();
	  
	work();
	iteration++;
	if (iteration > num) break;
      }

      if (prod != NULL) {
	prod->flush();
	prod->inc_frame();
      }

      //printf("send.iter = %d\n", iteration);

    }

    t.stop();
    printf("[run-simple] Thread-%d %s %s\n", 
	   node_id, 
	   t.get_str(),
	   outputs.size() == 0 ? "<== NO OUTPUT " : "");

    flush_sockets();
    pthread_exit(NULL);
  }


  void run_rcv(int num) {
    timer t;

    init_sockets();
    init_filter();
    init_state();

    send_credits();

    init_pop_buf_simple();
    //update_pop_buf_simple();
    
    if (need_credit) { 
      while (credit == 0) check_messages();
    }
    
    t.start();

    int saved_credit = -1;
    

    max_src_iter = 0;
    last_max_src_iter = 0;

    while (iteration < num) {

      check_messages();

      //while (need_credit && credit <= iteration) check_messages();
      
      saved_credit = -1;
      need_rollback = false;
      
      //check_messages();

      int extra = iteration + 400; //credit;
      if (extra > num) extra = num;
      extra -= iteration; 

      if (need_rollback && max_src_iter > last_max_src_iter) {

	saved_credit = credit; // save the maximum credit!!

	if (cons != NULL) { // rollback consumer
	  cons->rollback(pop_rate * (iteration - cpoint_iter));
	}
	
	restore_cpoint();

	//printf("[run-rcv] restoring state from a checkpoint! iter=%d\n", iteration);
	prod->dec_frame();

      } else {

	do_cpoint();

	if (cons != NULL) { // get enough input!

	  //int max;
	  //if (num < credit) max = num; else max = credit;
	  
	  //printf("read_data %d %d\n", cons->get_size(), pop_rate * (max - iteration));

	  while (cons->get_size() < pop_rate * extra) {
	    cons->read_frame();
	  }
	}
	
      }

      last_max_src_iter = max_src_iter; 
      max_src_iter = 0;

      int step = extra;// / 5;

      for (int y = 0; y < step; y++) {
	check_messages();
	work();
	iteration++;
	if (iteration > num) break;
      }
      prod->flush();
      prod->inc_frame();

      /*
      for (int f = 0; f < 4; f++) {
	for (int y = 0; y < step; y++) {
	  check_messages();
	  work();
	  iteration++;
	  if (iteration > num) break;
	}
	prod->flush_pseudo(); // frame_id will be set to -1;
	if (iteration > num) break;
      }
      */

      //prod->inc_frame();
      if (saved_credit > 0) credit = saved_credit; // restore credit!

      /*
      while (credit > iteration && iteration <= num) {
	check_messages();

	work();
	iteration++;
      }

      //printf("saved_credit = %d\n", saved_credit);

      //printf("[run-rcv] prod->flush() [id=%d size=%d]\n", prod->get_frame(), prod->get_size());
      
      prod->flush();
      prod->inc_frame();
      if (saved_credit > 0) credit = saved_credit; // restore credit!
      */
    }

    t.stop();
    printf("[run-simple] Thread-%d %s %s\n", 
	   node_id, 
	   t.get_str(),
	   outputs.size() == 0 ? "<== NO OUTPUT " : "");

    flush_sockets();
    pthread_exit(NULL);
  }


  void run_simple(int num) {
    timer t;
    int n;

    init_sockets();
    init_filter();
    init_state();

    //send_credits();
    //init_pop_buf_simple();

    while (iteration < num) {

      n = 100000;

      // BEGIN: modify for joiner !!

      vector<int>::iterator i;
      for (i = inputs.begin(); i != inputs.end(); ++i) {
	int id = *i;
	int _pop = pop_rates[id];
	consumer3<IN> *c = consumer_array[id];

	int size = c->get_size();
	while (size < _pop) {
	  c->read_frame();
	  size = c->get_size();
	}

	int nn = size / _pop;
	if (nn < n) n = nn;
      }

      // END: modify for joiner !!

      for (i = outputs.begin(); i != outputs.end(); ++i) {
	int id = *i;
	int nn = (PRODUCER_BUFFER_SIZE - producer_array[id]->get_size()) / push_rates[id];
	if (nn < n) n = nn;
      }

      //update_pop_buf_simple(n1);

      if (iteration == 0) t.start();      
      if (iteration + n > num) n = num - iteration;

      printf("Thread [%d] n=%d\n", node_id, n);

      //work_n(n);
      //iteration+=n;

      for (int y = 0; y < n; y++) {
	work();
	iteration++;
      }

      for (i = outputs.begin(); i != outputs.end(); ++i) {
	int id = *i;
	if (producer_array[id]->get_size() + push_rates[id]  
	    >= PRODUCER_BUFFER_SIZE) {
	  producer_array[id]->flush();
	  producer_array[id]->inc_frame();
	}
      }

    }
    
    t.stop();
    printf("[run-simple] Thread-%d %s %s\n", 
	   node_id, 
	   t.get_str(),
	   outputs.size() == 0 ? "<== NO OUTPUT " : "");

    flush_sockets();
    pthread_exit(NULL);
  }


  void run_sync(int num) {
    timer t;
    int n;
    int curr_frame = -1;
    int old_iter = -1;

    init_sockets();
    init_filter();
    init_state();

    //assert(inputs.size() == 1); // must have 1 input!
    //cons = consumer_array[*inputs.begin()];

    //assert(cons != NULL); // must have an input!
    //assert(prod != NULL); // must have an output!

    //send_credits();
    //init_pop_buf_simple();

    vector<int>::iterator i;

    while (iteration < num) {

      //assert (cons->get_size() < pop_rate);

      int id;

      i = inputs.begin();

      consumer3<IN> *c = consumer_array[*i]; 
      c->read_frame();
      id = c->get_frame_id();
      n = c->get_size() / pop_rates[*i];
      i++;
      
      while (i != inputs.end()) {
	c = consumer_array[*i]; 
	c->read_frame();
	assert(id == c->get_frame_id());
	assert(n == c->get_size() / pop_rates[*i]);
	i++;
      }

      //printf("sync: [size=%d] [n=%d] id=%d\n", c->get_size(), n, id);

      //cons->read_frame();
      //n = cons->get_size() / pop_rate;

      //printf("sync[%d f:%d]", node_id, id);

      if (id == -1) {
	//assert(false);

	// do nothing !!

      } else if (id > curr_frame) {
	
	curr_frame = id;
	do_cpoint();

      } else {

	for (i = outputs.begin(); i != outputs.end(); ++i) {
	  producer_array[*i]->dec_frame();
	}
	restore_cpoint();
	
      }

      if (iteration == 0) t.start();      

      for (int y = 0; y < n; y++) {
	work();
	iteration++;
      }
      
      // modify for splitter !!!

      for (i = outputs.begin(); i != outputs.end(); ++i) {
	producer3<OUT> *p = producer_array[*i];
	
	if (id == -1) {
	  p->flush_pseudo(); // frame_id will be -1
	} else {
	  p->flush();
	  p->inc_frame();
	}
      }
      
      //prod->flush();
      //prod->inc_frame();

    }
    
    t.stop();
    printf("[run-simple] Thread-%d %s %s\n", 
	   node_id, 
	   t.get_str(),
	   outputs.size() == 0 ? "<== NO OUTPUT " : "");

    flush_sockets();
    pthread_exit(NULL);
  }


};

#endif
