
#include <producer2.h>
#include <memsocket.h>
#include <netsocket.h>

void producer2<int>::init() {

  if (is_mem_socket) {

    ((memsocket*)sock)->set_buffer_size(PRODUCER_BUFFER_SIZE * sizeof(int));
    buf = (int*)((memsocket*)sock)->get_free_buffer();

  } else {
  
    buf = (int*)malloc(PRODUCER_BUFFER_SIZE * sizeof(int));
    
  }  
}

void producer2<float>::init() {

  if (is_mem_socket) {

    ((memsocket*)sock)->set_buffer_size(PRODUCER_BUFFER_SIZE * sizeof(float));
    buf = (float*)((memsocket*)sock)->get_free_buffer();

  } else {
  
    buf = (float*)malloc(PRODUCER_BUFFER_SIZE * sizeof(float));
    
  }  
}


void producer2<int>::send_buffer() {

  if (is_mem_socket) {

    while (((memsocket*)sock)->queue_full()) {
      ((memsocket*)sock)->wait_for_space();
    }
    ((memsocket*)sock)->push_buffer(buf);
    buf = (int*)((memsocket*)sock)->get_free_buffer();
    offs = 0;

  } else {
    
    ((netsocket*)sock)->write_chunk((char*)buf, PRODUCER_BUFFER_SIZE * sizeof(int));
    offs = 0;
    
  }
}

void producer2<float>::send_buffer() {

  if (is_mem_socket) {

    while (((memsocket*)sock)->queue_full()) {
      ((memsocket*)sock)->wait_for_space();
    }
    ((memsocket*)sock)->push_buffer(buf);
    buf = (float*)((memsocket*)sock)->get_free_buffer();
    offs = 0;

  } else {
    
    ((netsocket*)sock)->write_chunk((char*)buf, PRODUCER_BUFFER_SIZE * sizeof(float));
    offs = 0;
    
  }
}

