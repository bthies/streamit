
#include <save_state.h>
#include <netsocket.h>
#include <init_instance.h>
#include <save_manager.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <dirent.h>

void save_state::save_to_file(thread_info *t_info, 
			      int steady_iter, 
			      void (*write_object)(object_write_buffer *)) {

  
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


void save_state::load_from_file(int thread, 
				int steady_iter, 
				void (*read_object)(object_write_buffer *)) {
  
  object_write_buffer buf;
  
  char fname[256];
  sprintf(fname, "%s%d.%d", PATH, thread, steady_iter);
  
  printf("thread: %d file: %s\n", thread, fname);
  
  int fd = open(fname, O_RDONLY);
  netsocket file_sock(fd);
  
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


void save_state::load_state(int thread, int *steady, void (*read_object)(object_write_buffer *)) {
  
  unsigned iter = init_instance::get_thread_start_iter(thread);
  
  printf("thread: %d iteration: %d\n", thread, iter);
  
  if (iter > 0) {
    *steady = iter;
    save_state::load_from_file(thread, iter, read_object);
  }
}


int save_state::find_max_iter(int n_threads) {

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
	
	//printf("deleted (%s)", fname);
      }
    }
      
  }
   
  closedir(dir);
}
