#ifndef __SERIALIZABLE_H
#define __SERIALIZABLE_H

#include <object_write_buffer.h> 

class serializable {

 public:
  
  virtual void write_object(object_write_buffer *) = 0;

};

#endif
