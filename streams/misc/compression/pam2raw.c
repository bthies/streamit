/**
 * Pam (image files) to raw video converter.
 *
 * Usage: pam2raw <width> <height> <input-file-prefix> <output-file>
 *
 * It will read from input-file-prefix1.pam, input-file-prefix2.pam,
 * input-file-prefix3.pam, etc., and write to output-file.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

/* PAM headers look like this (120x96 image):
P7
WIDTH 120
HEIGHT 96
DEPTH 4
MAXVAL 255
TUPLETYPE RGB_ALPHA
ENDHDR
*/

int main(int argc, char** argv) {
    if (argc!=5) {
        printf("Usage: pam2raw <width> <height> <input-file-prefix> <output-file>\n");
    } else {
        int width = atoi(argv[1]);
        int height = atoi(argv[2]);
        FILE *input, *output = fopen(argv[4], "w");
        // frame number (int and string)
        int frame_no = 0;
        char frame_str[1000];
        char dummy[1000];
        int i;

        // loop over input files
        while (1) {
            int j=0;
            // open new input file
            sprintf(frame_str, "%s%d.pam", argv[3], ++frame_no);
            input = fopen(frame_str, "r");
            // if input doesn't exist, we're done
            if (input == 0) {
                if (frame_no == 1) {
                    // if the first file, flag an error
                    printf("Input file %s does not exist.\n", frame_str);
                    exit(1);
                } else {
                    // otherwise exit normally
                    break;
                }
            }

            // skip over the header (7 lines)
            for (i=0; i<7; i++) {
                fgets(dummy, 100, input);
            }

            // copy rest of file to output
            while (1) {
                int val = fgetc(input);
                if (val == EOF) break;
                fputc((char)val, output);
            }
            fclose(input);
        }
        fflush(output);
        fclose(output);
        printf("wrote %d frames\n", (frame_no-1));
    }
}
