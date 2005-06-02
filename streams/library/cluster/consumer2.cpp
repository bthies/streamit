
#include <consumer2.h>
#include <memsocket.h>
#include <netsocket.h>

void consumer2<int>::init() {

  buf = NULL;

  if (is_mem_socket) {

#ifndef ARM
  
    ((memsocket*)sock)->set_buffer_size(CONSUMER_BUFFER_SIZE * sizeof(int));

#endif //ARM
    
  } else {
    
    buf =  (int*)malloc(CONSUMER_BUFFER_SIZE * sizeof(int));
    
  }
}

void consumer2<float>::init() {

  buf = NULL;

  if (is_mem_socket) {

#ifndef ARM
  
    ((memsocket*)sock)->set_buffer_size(CONSUMER_BUFFER_SIZE * sizeof(float));

#endif //ARM
    
  } else {
    
    buf =  (float*)malloc(CONSUMER_BUFFER_SIZE * sizeof(float));
    
  }
}


void consumer2<int>::recv_buffer() {
   
  if (is_mem_socket) {

#ifndef ARM

    if (buf != NULL) ((memsocket*)sock)->release_buffer(buf);
    //while (((memsocket*)sock)->queue_empty()) {
    //  ((memsocket*)sock)->wait_for_data();
    //}
    buf = (int*)((memsocket*)sock)->pop_buffer();
    offs = 0;

#endif //ARM
    
  } else {

    ((netsocket*)sock)->read_chunk((char*)buf, CONSUMER_BUFFER_SIZE * sizeof(int));
    offs = 0;
  }
}


void consumer2<float>::recv_buffer() {
   
  if (is_mem_socket) {

#ifndef ARM

    if (buf != NULL) ((memsocket*)sock)->release_buffer(buf);
    //while (((memsocket*)sock)->queue_empty()) {
    //  ((memsocket*)sock)->wait_for_data();
    //}
    buf = (float*)((memsocket*)sock)->pop_buffer();
    offs = 0;

#endif //ARM
    
  } else {

    ((netsocket*)sock)->read_chunk((char*)buf, CONSUMER_BUFFER_SIZE * sizeof(float));
    offs = 0;
  }
}




