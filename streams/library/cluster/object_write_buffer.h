#ifndef __OBJECT_WRITE_BUFFER_H
#define __OBJECT_WRITE_BUFFER_H

class object_write_buffer {

  char buf[20000];

  int size;

  int read_offset;

 public:

  object_write_buffer();
  
  void erase();
  void set_read_offset(int offset);

  int get_size();
  
  void write(void *ptr, int size);
  void write_int(int data);
  void write_float(float data);
  
  void *get_ptr(int offset);
  void read(void *ptr, int size);
  int read_int();
  float read_float();

};

#endif
