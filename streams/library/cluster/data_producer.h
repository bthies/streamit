
#ifndef __DATA_PRODUCER_H
#define __DATA_PRODUCER_H

#include <socket_holder.h>
#include <serializable.h>
#include <mysocket.h>

#include <stdlib.h>

#define BUFFER_SIZE 1400

class data_producer : public socket_holder, public serializable {

  char *data_buffer;
  int buf_offset;

  int items_sent;

  mysocket *socket;

 public:

  data_producer();

  virtual void write_object(object_write_buffer *);
  virtual void read_object(object_write_buffer *);

  virtual mysocket *get_socket();
  virtual void set_socket(mysocket *);

  void write_item(void *buf, int size);

  void write_int(int);
  void write_float(float);

};


#endif
