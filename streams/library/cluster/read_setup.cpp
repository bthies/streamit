
#include <read_setup.h>
#include <stdio.h>

int read_setup::freq_of_chkpts = 1000;
int read_setup::out_data_buffer = 1400;
int read_setup::max_iteration = 10000;

void read_setup::read_setup_file() {

    char line[256];
    char arg1[128];
    int i;
  
    FILE *f = fopen("cluster-setup.txt", "r");

    if (f == NULL) return;

    printf("Reading cluster setup file...\n");
    
    for (;;) {

      line[0] = 0;
      arg1[0] = 0;

      fgets(line, 256, f);
      sscanf(line, "%s", arg1);

      if (strcmp(arg1, "frequency_of_checkpoints") == 0) {
	sscanf(line, "%s %d", arg1, &i);

	printf("frequency of checkpoints: %d\n", i);
	freq_of_chkpts = i;

      }

      if (strcmp(arg1, "outbound_data_buffer") == 0) {
	sscanf(line, "%s %d", arg1, &i);

	printf("outbound data buffer: %d\n", i);
	out_data_buffer = i;

      }

      if (strcmp(arg1, "number_of_iterations") == 0) {
	sscanf(line, "%s %d", arg1, &i);

	printf("number of iterations: %d\n", i);
	max_iteration = i;

      }

      if (feof(f)) break;
    }

    fclose(f);
}


