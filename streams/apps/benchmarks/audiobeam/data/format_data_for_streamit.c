/* compile with gcc, does not require any special libraries
 *
 * run with the following arguments:
 * argv[1] = input file name
 * argv[2] = number of lines
 * argv[3] = number of samples (mics) per lines
 * argv[4] = output file name
 */

#include <stdio.h>

main(int argc, char* argv[])
{
	int    i, lines, mics;
	float* buf;
	FILE*  f = fopen(argv[1], "r");

	sscanf(argv[2], "%d", &lines);
	sscanf(argv[3], "%d", &mics);
     	buf = (float*) malloc(lines * mics * sizeof(float));
 
        for (i = 0; i < lines * mics; i++) fscanf(f, "%f", &buf[i]);
	fclose(f);
	f = fopen(argv[4], "w");
	fwrite(&buf[0], lines * mics * sizeof(float), 1, f);
	fclose(f);
}
