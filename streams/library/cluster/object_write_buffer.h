#ifndef __OBJECT_WRITE_BUFFER_H
#define __OBJECT_WRITE_BUFFER_H

#define OWB_BUFFER_SIZE 65000    // 65,000 bytes

class object_write_buffer {

  char buf[OWB_BUFFER_SIZE];

  int size;
  int read_offset;

 public:

  // =========== constructor

  object_write_buffer();

  // =========== erase, set read_offset, get_size
  
  void erase();
  void set_read_offset(int offset);
  int get_size();
  
  // =========== read and write

  void write(void *ptr, int size);
  void read(void *ptr, int size);

  // =========== convenience functions

  void write_int(int data);
  void write_float(float data);  
  int read_int();
  float read_float();

};

#endif
