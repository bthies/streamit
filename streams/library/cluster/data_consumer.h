
#ifndef __DATA_CONSUMER_H
#define __DATA_CONSUMER_H

#include <socket_holder.h>
#include <serializable.h>
#include <mysocket.h>

class data_consumer : public socket_holder, public serializable {

  int items_read;

  mysocket *socket;

 public:

  data_consumer();

  virtual void write_object(object_write_buffer *);
  virtual void read_object(object_write_buffer *);

  virtual mysocket *get_socket();
  virtual void set_socket(mysocket *);

  void read_item(void *buf, int size);
  
  int read_int();
  float read_float();


};


#endif
