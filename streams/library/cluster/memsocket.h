
#ifndef __MEMSOCKET_H
#define __MEMSOCKET_H

#include <mysocket.h>

#include <pthread.h>

#define LOCK(var)   pthread_mutex_lock(var)
#define UNLOCK(var) pthread_mutex_unlock(var)

// unidirectional communication mechanism with reader and writer

#define BUFFER_SIZE_BYTES 100000

class memsocket : public mysocket {

  char *data_buffer;
  int tail, head;
  
  int item_size;
  int size;

 public:

  memsocket();

  virtual void set_item_size(int size);
  
  virtual int eof();
  virtual int get_fd();
  virtual void close();
  
  virtual bool data_available();

  virtual int read_chunk(char *buf, int len); 
  virtual int write_chunk(char *buf, int len);

  inline int write_item(char *buf);
  inline int read_item(char *buf);

  inline void peek_item(char *buf, int offset);

};

#endif


