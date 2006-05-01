
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <assert.h>

#define BUF_SIZE 4000

class FileReader_state {
public:
  FileReader_state() { file_handle = -1; buf_index = BUF_SIZE; }
  int file_handle;
  int file_offset, file_length;
  char file_buf[BUF_SIZE];
  int buf_index;
};

int FileReader_open(char *pathname) {
  FileReader_state *fs = new FileReader_state();
  fs->file_handle = open(pathname, O_RDONLY);
  fs->file_offset = 0;
  if (fs->file_handle == -1) {
    printf("ABORT! Failed to open file [%s]\n", pathname);
    exit(1);
  }
  struct stat buf;
  fstat(fs->file_handle, &buf);
  fs->file_length = buf.st_size;
  printf("FileReader.cpp: length of file [%s] is %d bytes.\n", pathname, fs->file_length); 
  return (int)fs;
}

int FileReader_getpos(int fs_ptr) {
  FileReader_state *fs = (FileReader_state*)fs_ptr;
  return fs->file_offset;
}

void FileReader_setpos(int fs_ptr, int pos) {
  FileReader_state *fs = (FileReader_state*)fs_ptr;
  assert(pos >= 0 && pos < fs->file_length);
  int seek_pos = lseek(fs->file_handle, pos, SEEK_SET);
  assert(seek_pos == pos);
  fs->file_offset = pos;
  fs->buf_index = BUF_SIZE; // Invalidate cached data
  return;
}

template<class T>
T FileReader_read(int fs_ptr) {
  
  FileReader_state *fs = (FileReader_state*)fs_ptr;

  assert(sizeof(T) == 4);

  if (fs->buf_index >= BUF_SIZE) {
    assert(fs->file_handle > -1);
    fs->buf_index = 0;
    while (fs->buf_index < BUF_SIZE) {
      int ret_val = read(fs->file_handle, 
			 fs->file_buf + fs->buf_index, 
			 BUF_SIZE - fs->buf_index);
     
      if (ret_val == 0) {
	lseek(fs->file_handle, 0, SEEK_SET);
      } 
      
      else if (ret_val > 0) {
	fs->buf_index += ret_val;
      }

      else if (ret_val == -1) {
	if (errno != EINTR) {
	  printf("ABORT! File-read\n");
	  perror("Error Message");
	  exit(1);
	}
      }
    }	
    fs->buf_index = 0;      
  }

  T res = *(int*)(fs->file_buf + fs->buf_index);
  fs->buf_index += 4;
  
  // Increment the offset (the virtual data pointer)
  fs->file_offset = (fs->file_offset + 4) % fs->file_length; 
  
  return res;
}


