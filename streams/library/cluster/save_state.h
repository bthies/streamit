#ifndef __SAVE_STATE__H
#define __SAVE_STATE__H

#include <mysocket.h>
#include <init_instance.h>
#include <save_manager.h>
#include <object_write_buffer.h>
#include <thread_info.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <dirent.h>

#define PATH "/u/janiss/checkpoints/"

class save_state {
  
 public:

  static void save_to_file(thread_info *t_info, 
			   int steady_iter, 
			   void (*write_object)(object_write_buffer *));

  static void save_buffer(int thread, int steady_iter, object_write_buffer *buf);

  static void load_from_file(int thread, 
			   int steady_iter, 
			     void (*read_object)(object_write_buffer *));

  static void load_state(int thread, int *steady, void (*read_object)(object_write_buffer *));

  static int find_max_iter(int n_threads);

  static void delete_checkpoints(int max_iter); // deletes all checkpints
                                                // with iteration less than max_iter
};


#endif
