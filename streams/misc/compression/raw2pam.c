/**
 * Raw video to pam (image files) converter.
 *
 * Usage: raw2pam <width> <height> <input-file> <output-file-prefix>
 *
 * It will write to output-file-prefix1.pam, output-file-prefix2.pam, output-file-prefix3.pam, etc.
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
TUPLTYPE RGB_ALPHA
ENDHDR
*/

int main(int argc, char** argv) {
    if (argc!=5) {
        printf("Usage: raw2pam <width> <height> <input-file> <output-file-prefix>\n");
    } else {
        int width = atoi(argv[1]);
        int height = atoi(argv[2]);
        FILE* input = fopen(argv[3], "r");
        // frame number (int and string)
        int frame_no = 0;
        char frame_str[1000];
        // whether we're done
        int eof = 0;
        // check for input file
        if (input == 0) {
            printf("Input file %s does not exist.\n", argv[3]);
            exit(1);
        }
        while (!eof) {
            int i;
            FILE* output;
            // copy frame data
            for (i=0; i<width*height*4; i++) {
                int val = fgetc(input);
                if (val==EOF) {
                    eof = 1;
                    break;
                } 
                if (i==0) {
                    // start new file when we have something to write to it
                    sprintf(frame_str, "%s%d.pam", argv[4], ++frame_no);
                    output = fopen(frame_str, "w");
                    // insert headers between frames
                    fprintf(output, "P7\nWIDTH %d\nHEIGHT %d\nDEPTH 4\nMAXVAL 255\nTUPLTYPE RGB_ALPHA\nENDHDR\n", width, height);
                }
                fputc((char)val, output);
                if (i==width*height*4-1) {
                    // close file when we're done
                    // (this needs to be inside the loop to avoid closing an empty file the last time through)
                    fflush(output);
                    fclose(output);
                }
            }
        }
        fclose(input);
        printf("wrote %d frames\n", frame_no);
    }
}
