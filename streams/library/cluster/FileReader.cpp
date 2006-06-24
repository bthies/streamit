#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <assert.h>
#include <FileReader.h>

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
    // RMR { comment out printf 
    // printf("FileReader.h: length of file [%s] is %d bytes.\n", pathname, fs->file_length); 
    // } RMR
    return (int)fs;
}

void FileReader_close(int fs_ptr) {
    FileReader_state *fs = (FileReader_state*)fs_ptr;
    close(fs->file_handle);
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
