#ifndef __SAVE_STATE__H
#define __SAVE_STATE__H

#include <thread_info.h>
#include <object_write_buffer.h>

#define PATH "/u/janiss/checkpoints/"

class save_state {

  object_write_buffer owb_buf;
  
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
