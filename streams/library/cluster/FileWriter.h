
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <assert.h>

#define BUF_SIZE 4000

class FileWriter_state {
public:
  FileWriter_state() { file_handle = -1; buf_index = 0; }
  int file_handle;
  int file_offset, file_length;
  char file_buf[BUF_SIZE];
  int buf_index;
};

int FileWriter_open(char *pathname) {
  FileWriter_state *fs = new FileWriter_state();
  fs->file_handle = open(pathname, O_RDWR | O_CREAT, S_IRWXU);
  fs->file_offset = 0;
  if (fs->file_handle == -1) {
    printf("ABORT! Failed to open file [%s]\n", pathname);
    exit(1);
  }
  struct stat buf;
  fstat(fs->file_handle, &buf);
  fs->file_length = buf.st_size;
  printf("FileWriter.cpp: length of file [%s] is %d bytes.\n", pathname, fs->file_length); 
  return (int)fs;
}

void FileWriter_close(int fs_ptr) {
  FileWriter_state *fs = (FileWriter_state*)fs_ptr;
  close(fs->file_handle);
}

int FileWriter_getpos(int fs_ptr) {
  FileWriter_state *fs = (FileWriter_state*)fs_ptr;
  return fs->file_offset + fs->buf_index;
}

int FileWriter_flush(int fs_ptr);

void FileWriter_setpos(int fs_ptr, int pos) {
  FileWriter_state *fs = (FileWriter_state*)fs_ptr;
  FileWriter_flush(fs_ptr); // Flush so that cached data is saved

  // Update file length information & make sure pos is legal
  struct stat buf;
  fstat(fs->file_handle, &buf);
  fs->file_length = buf.st_size;
  assert(pos >= 0 && pos <= fs->file_length);

  int seek_pos = lseek(fs->file_handle, pos, SEEK_SET);
  assert(seek_pos == pos);
  fs->file_offset = pos;
}

template<class T>
int FileWriter_write(int fs_ptr, T data) {
  FileWriter_state *fs = (FileWriter_state*)fs_ptr;

  assert(sizeof(T) == 4);

  // Flush if adding data to the buffer would overflow the buffer
  if (fs->buf_index + 4 > BUF_SIZE) FileWriter_flush(fs_ptr); 

  *(T*)(fs->file_buf + fs->buf_index) = data;
  fs->buf_index += 4;
}

int FileWriter_flush(int fs_ptr) {
  FileWriter_state *fs = (FileWriter_state*)fs_ptr;

  int pos = 0;

  while (pos < fs->buf_index) {
    int ret_val = write(fs->file_handle, 
			fs->file_buf + pos, 
			fs->buf_index - pos);
     
      
    if (ret_val > 0) {
      fs->file_offset += ret_val;
      pos += ret_val;
    }

    else if (ret_val == -1) {
      if (errno != EINTR) {
	printf("ABORT! File-write\n");
	perror("Error Message");
	exit(1);
      }
    }
  }

  fs->buf_index = 0;
}

