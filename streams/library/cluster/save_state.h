/*
 * Copyright 2006 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */
#ifndef __SAVE_STATE__H
#define __SAVE_STATE__H

#include <thread_info.h>
#include <object_write_buffer.h>

#define PATH "/u/janiss/checkpoints/"

class save_state {

  object_write_buffer owb_buf;
  
 public:

  static void observe_jiffies(thread_info *t_info);
  static void save_to_file(thread_info *t_info, 
			   int steady_iter, 
			   void (*write_object)(object_write_buffer *));

  static void save_buffer(int thread, int steady_iter, object_write_buffer *buf);

  static int load_from_file(int thread, 
			   int steady_iter, 
			     void (*read_object)(object_write_buffer *));

  static int load_state(int thread, int *steady, void (*read_object)(object_write_buffer *));

  static int find_max_iter(int n_threads);

  static void delete_checkpoints(int max_iter); // deletes all checkpints
                                                // with iteration less than max_iter
};


#endif
