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
#ifndef __FILEREADER_H
#define __FILEREADER_H
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

/* Routines that are independent of <T>, make them not be in class */

extern int FileReader_open(char *pathname);

extern void FileReader_close(int fs_ptr);

extern int FileReader_getpos(int fs_ptr);

extern void FileReader_setpos(int fs_ptr, int pos);

template<class T>
static inline T FileReader_read(int fs_ptr) {
  
    FileReader_state *fs = (FileReader_state*)fs_ptr;

    assert((sizeof(T) % 4) == 0);

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

    // RMR { note this code assume that the data is stored to
    // consecutive words; which is the case for the current
    // defintion of the <complex> data type
    T res;
    for (int i = 0; i < sizeof(T); i += 4) {
        *((int*)((&res)+i)) = *(int*)(fs->file_buf + fs->buf_index);
        fs->buf_index += 4;
    }
  
    // Increment the offset (the virtual data pointer)
    fs->file_offset = (fs->file_offset + sizeof(T)) % fs->file_length; 
    // } RMR

    return res;
}
#endif
