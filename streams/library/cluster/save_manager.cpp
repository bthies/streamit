
#include <save_manager.h>
#include <save_state.h>

pthread_mutex_t save_manager::queue_lock = PTHREAD_MUTEX_INITIALIZER;
queue <checkpoint_info*> save_manager::checkpoints;  

void save_manager::push_item(checkpoint_info *info) {

  LOCK(&queue_lock);
  checkpoints.push(info);
  UNLOCK(&queue_lock);
}

checkpoint_info *save_manager::pop_item() {

  checkpoint_info *res;

  LOCK(&queue_lock);

  if (!checkpoints.empty()) {

    res = checkpoints.front();
    checkpoints.pop();

  } else {

    res = NULL;
  }    

  UNLOCK(&queue_lock);

  return res;
}

void save_manager::run() {

  pthread_t id;
  pthread_create(&id, NULL, run_save_manager, NULL);
}

void *run_save_manager(void *) {
  
  checkpoint_info *info;

  for (;;) {
    
    info = save_manager::pop_item();

    if (info == NULL) {
      usleep(10000); // 1/100th of a second

    } else {
      
      save_state::save_buffer(info->thread, info->steady_iter, info->buf);
      
      delete info->buf;
      delete info;
    }
  }
}

