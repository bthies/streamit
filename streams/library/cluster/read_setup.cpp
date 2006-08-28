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

#include <read_setup.h>
#include <stdio.h>
#include <string.h>

int read_setup::freq_of_chkpts = 1000;
int read_setup::out_data_buffer = 1400;
int read_setup::max_iteration = 10000;

void read_setup::read_setup_file() {

    char line[256];
    char arg1[128];
    int i;
  
    FILE *f = fopen("cluster-setup.txt", "r");

    if (f == NULL) return;

#ifdef VERBOSE
    fprintf(stderr,"Reading cluster setup file...\n");
#endif

    for (;;) {

      line[0] = 0;
      arg1[0] = 0;

      fgets(line, 256, f);
      sscanf(line, "%s", arg1);

      if (strcmp(arg1, "frequency_of_checkpoints") == 0) {
	sscanf(line, "%s %d", arg1, &i);

#ifdef VERBOSE
	fprintf(stderr,"frequency of checkpoints: %d\n", i);
#endif
	freq_of_chkpts = i;
      }

      if (strcmp(arg1, "outbound_data_buffer") == 0) {
	sscanf(line, "%s %d", arg1, &i);

#ifdef VERBOSE
	fprintf(stderr,"outbound data buffer: %d\n", i);
#endif
	out_data_buffer = i;

      }

      if (strcmp(arg1, "number_of_iterations") == 0) {
	sscanf(line, "%s %d", arg1, &i);

#ifdef VERBOSE
	fprintf(stderr,"number of iterations: %d\n", i);
#endif
	max_iteration = i;

      }

      if (feof(f)) break;
    }

    fclose(f);
}


