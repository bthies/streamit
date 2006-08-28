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

#include <save_state.h>
#include <netsocket.h>
#include <init_instance.h>
#include <save_manager.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#ifdef RHEL3
#include <linux/unistd.h>
_syscall0(pid_t,gettid)
#else
#include <unistd.h>
int gettid() { return 0; }
#endif // RHEL3

#ifndef ARM

#include <dirent.h>

#endif

static bool debugging = false;


void save_state::observe_jiffies(thread_info *t_info) {
  
  timeval tv;
  gettimeofday(&tv, NULL);

  float time_diff = 0;
  time_diff += (tv.tv_sec-t_info->last_jiff.tv_sec);
  time_diff += (tv.tv_usec-t_info->last_jiff.tv_usec) / 1e6;

  // make sure we dont gather data too frequently
  if (time_diff < 0.8) return; 

  int pid = gettid();
  char fname[128], tmp[512];
  sprintf(fname, "/proc/%d/stat", pid);
  FILE *f = fopen(fname, "r");

  for (int i = 0; i < 13; i++) fscanf(f, "%s", tmp);
  int u, s;
  fscanf(f, "%d %d", &u, &s);
  fclose(f);

  int jiff_diff = (u+s - t_info->last_count);
  float usage = jiff_diff / time_diff;

  //printf("Thread-%d Usage is %5.2f jiffies/sec\n", 
  //	 t_info->get_thread_id(), usage);
  
  t_info->usage = usage;
  t_info->last_count = u+s;
  t_info->last_jiff.tv_sec = tv.tv_sec;
  t_info->last_jiff.tv_usec = tv.tv_usec;
}


void save_state::save_to_file(thread_info *t_info, 
			      int steady_iter, 
			      void (*write_object)(object_write_buffer *)) {

  observe_jiffies(t_info);
  
  object_write_buffer *buf = new object_write_buffer();
  write_object(buf);
  //save_buffer(thread, steady_iter, buf);

  checkpoint_info *c_info = new checkpoint_info(t_info, steady_iter, buf);
  save_manager::push_item(c_info);
}


void save_state::save_buffer(int thread, int steady_iter, object_write_buffer *buf) {
  
  char fname[256];
  sprintf(fname, "%s%d.%d", PATH, thread, steady_iter);
  
  int fd = creat(fname, S_IRUSR|S_IWUSR|S_IRGRP|S_IROTH);
  netsocket file_sock(fd);
  
  int size = buf->get_size();
  int offset = 0;
  
  buf->set_read_offset(0);
  
  char tmp[1024];
  
  for (;;) {
    
    if (size > 1024) {
      
      buf->read(tmp, 1024);
      file_sock.write_chunk(tmp, 1024);
      offset += 1024;
      size -= 1024;
      
    } else {
      
      buf->read(tmp, size);
      file_sock.write_chunk(tmp, size);
      break;
    }
    
  }
  
  close(fd);
} 


int save_state::load_from_file(int thread, 
				int steady_iter, 
				void (*read_object)(object_write_buffer *)) {
  
  object_write_buffer *buf = new object_write_buffer();
  
  char fname[256];
  sprintf(fname, "%s%d.%d", PATH, thread, steady_iter);
  
  //fprintf(stderr,"thread: %d file: %s\n", thread, fname);
  
  int fd = open(fname, O_RDONLY);
  
  if (fd == -1) {
    perror("load_checkpoint");
    return -1;
  }
  
  netsocket file_sock(fd);

  int count = 0;
  
  for (;;) {
    char tmp[4];
    int retval = file_sock.read_chunk(tmp, 4);
    if (retval == -1) break;
    buf->write(tmp, 4);

    count += 4;

    //fprintf(stderr,"read data (4 bytes)\n");
  }

  if (debugging) {
    fprintf(stderr,"thread: %d file: %s size: %d bytes\n", thread, fname, count);
  }
  close(fd);
  
  buf->set_read_offset(0);
  read_object(buf);

  delete buf;

  return 0;
}


int save_state::load_state(int thread, int *steady, void (*read_object)(object_write_buffer *)) {
  
  unsigned iter = init_instance::get_thread_start_iter(thread);
  if (debugging) {
    fprintf(stderr,"thread: %d iteration: %d\n", thread, iter);
  }

  if (iter > 0) {
    *steady = iter;
    if (save_state::load_from_file(thread, iter, read_object) == -1) return -1;
  }

  return 0;
}


int save_state::find_max_iter(int n_threads) {

#ifdef ARM  

  return 0;

#else // ARM

  int *max_iter = (int*)malloc(n_threads * sizeof(int));
  memset(max_iter, 0, n_threads * sizeof(int));

  struct dirent *dent;
  DIR *dir;
  
  dir = opendir(PATH);
  if (!dir) return 0;

  while (NULL != (dent = readdir(dir))) {
      
    int thread;
    int iter;
    
    char *name = dent->d_name;
    
    int len = strlen(name);
    bool correct = true;
      
    if (len < 3) correct = false; // eliminate "." and ".."
    
    for (int i = 0; i < len; i++) {
      if (!((name[i] >= '0' && name[i] <= '9') || name[i] == '.')) {
	correct = false;
	break;
      }
    }
    
    if (correct) {
      
      sscanf(dent->d_name, "%d.%d", &thread, &iter);
      if (max_iter[thread] < iter) max_iter[thread] = iter;
      
    }      
  }
  
  closedir(dir);

  // now find the smallest of the maximum iterations for each thread

  int result = max_iter[0];
  
  for (int i = 1; i < n_threads; i++) {
    if (max_iter[i] < result) result = max_iter[i];
  }

  return result;


#endif // ARM
}

/*
int save_state::test_iter(int iter, int n_threads) {
  
  char fname[256];
  
  for (int t = 0; t < n_threads; t++) {
    sprintf(fname, "%s%d.%d", PATH, t, iter);
    int fd = open(fname, O_RDONLY);
    if (fd == -1) return -1;
    close(fd);
  }
    
  return 0;
}
*/

void save_state::delete_checkpoints(int max_iter) {

#ifndef ARM  
  
  struct dirent *dent;
  DIR *dir;

  dir = opendir(PATH);
  if (!dir) return;

  while (NULL != (dent = readdir(dir))) {
      
    int thread;
    int iter;
    
    char *name = dent->d_name;
    
    int len = strlen(name);
    bool correct = true;
    
    if (len < 3) correct = false; // eliminate "." and ".."
    
    for (int i = 0; i < len; i++) {
      if (!((name[i] >= '0' && name[i] <= '9') || name[i] == '.')) {
	correct = false;
	break;
      }
    }
      
    if (correct) {
      
      sscanf(dent->d_name, "%d.%d", &thread, &iter);
      if (iter < max_iter) {
	
	char fname[128];
	sprintf(fname, "%s%s", PATH, name);
	unlink(fname);
	
	//fprintf(stderr,"deleted (%s)", fname);
      }
    }
      
  }
   
  closedir(dir);

#endif //ARM

}
