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

int FileWriter_open(char *pathname);

void FileWriter_close(int fs_ptr);

int FileWriter_flush(int fs_ptr);

int FileWriter_getpos(int fs_ptr);

void FileWriter_setpos(int fs_ptr, int pos);

template<class T>
static inline void FileWriter_write(int fs_ptr, T data) {
    FileWriter_state *fs = (FileWriter_state*)fs_ptr;

    assert((sizeof(T) % 4) == 0);

    // Flush if adding data to the buffer would overflow the buffer
    if (fs->buf_index + sizeof(T) > BUF_SIZE) FileWriter_flush(fs_ptr);

    // RMR { note this code assume that the data is placed in 
    // consecutive words; which is the case for the current
    // defintion of the <complex> data type
    for (int i = 0; i < sizeof(T); i += 4) {
        *(int*)(fs->file_buf + fs->buf_index) = *((int*)((&data)+i));
        fs->buf_index += 4;
    }
    // } RMR
}
