
#include <delete_chkpts.h>
#include <save_state.h>
#include <unistd.h>
#include <dirent.h>
#include <stdio.h>

int delete_chkpts::max_iter = 0;
int delete_chkpts::iter_done = 0;

void delete_chkpts::set_max_iter(int iter) {
  max_iter = iter;
}

void delete_chkpts::run() {

  for (;;) {
    
    if (iter_done < max_iter) {
      save_state::delete_checkpoints(max_iter);
      iter_done = max_iter;

    } else {
      usleep(10000); // 1/100th of a second
    }
  }
}
