#ifndef __SAVE_STATE__H
#define __SAVE_STATE__H

#include <object_write_buffer.h>

class save_state {
  
 public:

  static void save_to_file(int thread, 
			   int steady_iter, 
			   void (*write_object)(object_write_buffer *)) {

    object_write_buffer buf;
    buf.reset();
    write_object(&buf);

    char fname[256];
    sprintf(fname, "/tmp/data/thread%d.%d", thread, steady_iter);
    FILE *f = fopen(fname, "w");

    int size = buf.get_size() / 4;
    for (int i = 0; i < size; i++) {

      fprintf(f, "%d\n", buf.get_data_int(i * 4));

    }

    fclose(f);
  }

};


#endif
