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

#include <save_manager.h>
#include <save_state.h>

pthread_mutex_t save_manager::queue_lock = PTHREAD_MUTEX_INITIALIZER;
queue <checkpoint_info*> save_manager::checkpoints;  

void save_manager::push_item(checkpoint_info *info) {

  //fprintf(stderr, "save_manager:1 %s\n", "LOCK(&queue_lock);");
  LOCK(&queue_lock);
  checkpoints.push(info);
  //fprintf(stderr, "save_manager:2 %s\n", "UNLOCK(&queue_lock);");
  UNLOCK(&queue_lock);
}

checkpoint_info *save_manager::pop_item() {

  checkpoint_info *res;

  //fprintf(stderr, "save_manager:3 %s\n", "LOCK(&queue_lock);");
  LOCK(&queue_lock);

  if (!checkpoints.empty()) {

    res = checkpoints.front();
    checkpoints.pop();

  } else {

    res = NULL;
  }    

  //fprintf(stderr, "save_manager:4 %s\n", "UNLOCK(&queue_lock);");
  UNLOCK(&queue_lock);

  return res;
}

void save_manager::run() {

  checkpoint_info *info;

  for (;;) {
    
    info = pop_item();

    if (info == NULL) {
      usleep(10000); // 1/100th of a second

    } else {
      
      thread_info *t_info = info->t_info; 

      int thread_id = t_info->get_thread_id();
      
      save_state::save_buffer(thread_id, info->steady_iter, info->buf);

      (*t_info->get_latest_checkpoint()) = info->steady_iter;

      //fprintf(stderr,"save(%d.%d)", thread_id, info->steady_iter);
      //fflush(stderr);

      delete info->buf;
      delete info;
    }
  }
}



