#ifndef __OBJECT_WRITE_BUFFER_H
#define __OBJECT_WRITE_BUFFER_H

class object_write_buffer {

  char buf[512];

  int size;

 public:

  object_write_buffer();
  
  void reset();
  
  void write(void *ptr, int size);

  void write_int(int data);
  void write_float(float data);

  int get_size();
  int get_data_int(int offset);

};



#endif
