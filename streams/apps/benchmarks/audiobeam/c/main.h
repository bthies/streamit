#ifndef _MAIN_H
#define _MAIN_H

#include "stdio.h"
#include "stdlib.h"
#include "math.h"

#define SOUND_SPEED 342
#define SAMPLING_RATE 16000
#define MAX_LINE 1024
#define CARTESIAN_DISTANCE(x1,y1,z1,x2,y2,z2) (sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2)+(z1-z2)*(z1-z2)));


// Queue to store the samples coming in from the data stream. The
// queue is implemented via an array of double arrays. The head and
// tail are kept track of using the head and tail indices.
struct DataQueue {
    double **sample_queue;
    int head;
    int tail;
    unsigned char full;
};


// A struct to store delay values and some information about each delay
struct Delays {
    double **delay_values;

    // An array of strings about the delays. For instance, if the
    // delays[2] is for angle -90 degrees, then delay_info[2] will
    // probably be "-90 degrees".
    char **delay_info;
    long int max_delay;
};




#endif /* _MAIN_H */

