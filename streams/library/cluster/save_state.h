#ifndef __SAVE_STATE__H
#define __SAVE_STATE__H

#include <mysocket.h>
#include <init_instance.h>
#include <save_manager.h>
#include <object_write_buffer.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>

#define PATH "/u/janiss/checkpoints/"

class save_state {
  
 public:

  static void save_to_file(int thread, 
			   int steady_iter, 
			   void (*write_object)(object_write_buffer *)) {

    /*

    object_write_buffer *buf = new object_write_buffer();
    write_object(buf);
    //save_buffer(thread, steady_iter, buf);
    
    save_manager::push_item(new checkpoint_info(thread, steady_iter, buf));
    */
  }

  static void save_buffer(int thread, int steady_iter, object_write_buffer *buf) {

    char fname[256];
    sprintf(fname, "%s%d.%d", PATH, thread, steady_iter);

    int fd = creat(fname, S_IRUSR|S_IWUSR|S_IRGRP|S_IROTH);
    mysocket file_sock(fd);
    
    int size = buf->get_size() / 4;
    int offset = 0;

    buf->set_read_offset(0);

    char tmp[1024];

    for (;;) {
      
      if (size > 1024) {
		
	file_sock.write_chunk((char*)buf->get_ptr(offset), 1024);
	offset += 1024;
	size -= 1024;

      } else {
		
	file_sock.write_chunk((char*)buf->get_ptr(offset), size);
	break;

      }

    }

    close(fd);
  } 


  static void load_from_file(int thread, 
			   int steady_iter, 
			   void (*read_object)(object_write_buffer *)) {

    object_write_buffer buf;

    char fname[256];
    sprintf(fname, "%s%d.%d", PATH, thread, steady_iter);

    printf("thread: %d file: %s\n", thread, fname);

    int fd = open(fname, O_RDONLY);
    mysocket file_sock(fd);
    
    for (;;) {
      char tmp[4];
      int retval = file_sock.read_chunk(tmp, 4);
      if (retval == -1) break;
      buf.write(tmp, 4);
      printf("read data (4 bytes)\n");
    }

    close(fd);

    buf.set_read_offset(0);

    read_object(&buf);
  }


  static void load_state(int thread, int *steady, void (*read_object)(object_write_buffer *)) {

    unsigned iter = init_instance::get_thread_start_iter(thread);

    printf("thread: %d iteration: %d\n", thread, iter);

    if (iter > 0) {
      *steady = iter;
      save_state::load_from_file(thread, iter, read_object);
    }
  }

  static int test_iter(int iter, int n_threads) {
        
    char fname[256];

    for (int t = 0; t < n_threads; t++) {
          sprintf(fname, "%s%d.%d", PATH, t, iter);
	  int fd = open(fname, O_RDONLY);
	  if (fd == -1) return -1;
	  close(fd);
    }
    
    return 0;
  }

};


#endif
