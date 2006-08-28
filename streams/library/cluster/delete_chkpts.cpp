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

#include <delete_chkpts.h>
#include <save_state.h>
#include <unistd.h>
//#include <dirent.h>
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
