
#ifndef __READ_SETUP_H
#define __READ_SETUP_H

/*
  setup file format:

frequency_of_checkpoints 1000 // must be a multiple of 1000 or 0 for disabled.
outbound_data_buffer 1400     // <= 1400 or 0 for disabled.
number_of_iterations 10000 // number of steady state iterations

 */

class read_setup {

 public:

  static int freq_of_chkpts;
  static int out_data_buffer;
  static int max_iteration;

  static void read_setup_file();
};

#endif

