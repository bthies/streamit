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

