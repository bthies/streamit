/*
 * Apple Animation to Rawvideo converter
 * Based on decoder in ffmpeg project
 *
 * Only decodes RGBA (32-bit) formats.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define BE_16(x)  ((((unsigned char*)(x))[0] << 8) | ((unsigned char*)(x))[1])

// the input buffer
unsigned char *buf;
// the size of the input buffer
int buf_size;
// width and height of image
int width, height;
// file handles on input and output files
FILE *input_file, *output_file;
// running buffer of output frame
unsigned char* frame;
// whether this is the first time through the decoder
int firsttime = 1;

// returns how many bytes decoded
int qtrle_decode_32bpp() {
    int stream_ptr;
    int header;
    int start_line;
    int lines_to_change;
    int rle_code;
    int row_ptr, pixel_ptr;
    unsigned char a, r, g, b;
    unsigned int argb;

    //for (a=4; a<64; a++) {
    //    fprintf(stderr, " %d", buf[a]);
    //}

    /* check if this frame is even supposed to change */
    /* HAZARD: we don't have frame sizes in coming from raw video
    if (s->size < 8)
        return;
    */

    /* start after the chunk size */
    stream_ptr = 4;

    /* fetch the header */
    header = BE_16(&(buf[stream_ptr]));
    stream_ptr += 2;

    /* if a header is present, fetch additional decoding parameters */
    if (header & 0x0008) {
        start_line = BE_16(&(buf[stream_ptr]));
        stream_ptr += 4;
        lines_to_change = BE_16(&(buf[stream_ptr]));
        stream_ptr += 4;
    } else {
        start_line = 0;
        lines_to_change = height;
    }

    //printf("start_line=%d, lines_to_change=%d\n", start_line, lines_to_change);

    //av_log (s->avctx, AV_LOG_INFO, "\n start_line=%d, lines_to_change=%d", start_line, lines_to_change);
    row_ptr = width * start_line * 4;
    while (lines_to_change--) {
        int i;
        //av_log (s->avctx, AV_LOG_INFO, "\n start of line (%d):", lines_to_change);
        //for (i=0; i<720*4; i++) fprintf(stderr, " %d", (signed char)buf[stream_ptr+i]);
        pixel_ptr = row_ptr + (buf[stream_ptr++] - 1) * 4;
        //printf("1 pixel_ptr=%d, row_ptr=%d, width=%d\n", pixel_ptr, row_ptr, width);

        while ((rle_code = (signed char)buf[stream_ptr++]) != -1) {
            if (rle_code == 0) {
                //av_log (s->avctx, AV_LOG_INFO, "\n skip %d pixels", buf[stream_ptr]-1);
                /* there's another skip code in the stream */
                pixel_ptr += (buf[stream_ptr++] - 1) * 4;
                //printf("2 pixel_ptr=%d\n", pixel_ptr);
            } else if (rle_code < 0) {
                /* decode the run length code */
                //av_log (s->avctx, AV_LOG_INFO, "\n rle %d pixels", -rle_code);
                rle_code = -rle_code;
                a = buf[stream_ptr++]; // REVERSE SINCE WE'RE GOING TO PAM
                b = buf[stream_ptr++]; // REVERSE SINCE WE'RE GOING TO PAM
                g = buf[stream_ptr++]; // REVERSE SINCE WE'RE GOING TO PAM
                r = buf[stream_ptr++]; // REVERSE SINCE WE'RE GOING TO PAM
                argb = (a << 24) | (r << 16) | (g << 8) | (b << 0);

                while (rle_code--) {
                    *(unsigned int *)(&frame[pixel_ptr]) = argb;
                    pixel_ptr += 4;
                    //printf("3 pixel_ptr=%d\n", pixel_ptr);
                }
            } else {
                //av_log (s->avctx, AV_LOG_INFO, "\n new %d pixels", rle_code);

                /* copy pixels directly to output */
                while (rle_code--) {
                    a = buf[stream_ptr++]; // REVERSE SINCE WE'RE GOING TO PAM
                    b = buf[stream_ptr++]; // REVERSE SINCE WE'RE GOING TO PAM
                    g = buf[stream_ptr++]; // REVERSE SINCE WE'RE GOING TO PAM
                    r = buf[stream_ptr++]; // REVERSE SINCE WE'RE GOING TO PAM
                    //fprintf(stderr, "\n argb=%d,%d,%d,%d", a, r, g, b);
                    argb = (a << 24) | (r << 16) | (g << 8) | (b << 0);
                    //printf("4 pixel_ptr=%d\n", pixel_ptr);
                    *(unsigned int *)(&frame[pixel_ptr]) = argb;
                    pixel_ptr += 4;
                }
            }
        }
        row_ptr += width * 4;
    }
    return stream_ptr+1;
}

// shift the current frame down by <shift> bytes and fill the buffer
// with data.  returns how much data is in the buffer.
int eof = 0;
int read_data(int shift) {
    int i;
    // shift data down
    for (i=0; i<buf_size-shift; i++) {
        buf[i] = buf[i+shift];
    }
    // fill rest of buffer from file
    for (i=buf_size-shift; i<buf_size && !eof; i++) {
        int val = fgetc(input_file); 
        if (val==EOF) {
            eof = 1;
            break;
        } else {
            buf[i] = (unsigned char)val;
        }
    }
    return i;
}

// writes the current frame to a file
int write_frame() {
    int i;
    for (i=0;i<width*height*4; i++) {
        fputc(frame[i], output_file);
    }
}

int main(int argc, char** argv) {
    if (argc != 5) {
        printf("Usage: aa2raw <width> <height> <input-file> <output-file> %d\n", argc);
        return 1;
    }
    // get argv
    width = (int)atoi(argv[1]);
    height = (int)atoi(argv[2]);
    input_file = fopen(argv[3], "r+b");
    output_file = fopen(argv[4], "w+b");

    // check for input file
    if (input_file == 0) {
        printf("Input file %s does not exist.\n", argv[3]);
        exit(1);
    }

    // init buffers
    frame = calloc(width*height*4, 1);
    buf_size = 2*width*height*4; // a safe buffering amount
    buf = (unsigned char*)malloc(buf_size);

    // main loop
    {
        int consumed = buf_size; int frame_no = 0;
        do {
            buf_size = read_data(consumed);
            consumed = qtrle_decode_32bpp();
            write_frame();
            frame_no++;
        } while (consumed < buf_size);

        printf("wrote %d frames\n", frame_no);
    }

    // close output files
    fclose(input_file);
    fclose(output_file);
}
