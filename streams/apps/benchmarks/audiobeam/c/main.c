#include "main.h"

#define NUM_MIC 15
//#define NUM_MIC 14
#define ANGLE_ENERGY_WINDOW_SIZE 400
#define NUM_ANGLES 180
#define GRID_STEP_SIZE 0.003  // .3cm
#define GRID_ENERGY_WINDOW_SIZE 3200 // (samples) = 200ms
#define NUM_DIRS 7


// 15-microphone array for data set from Kevin

double mic_locations[NUM_MIC][3] = {
    {1.5,2.79,0},
    {1.5,2.82,0},
    {1.5,2.85,0},
    {1.5,2.88,0},
    {1.5,2.91,0},
    {1.5,2.94,0},
    {1.5,2.97,0},
    {1.5,3,0},
    {1.5,3.03,0},
    {1.5,3.06,0},
    {1.5,3.09,0},
    {1.5,3.12,0},
    {1.5,3.15,0},
    {1.5,3.18,0},
    {1.5,3.21,0}
};

double source_location[] = {1.1677,2.1677,1};

double origin_location[] = {1.5,3,0};  // center of the array


/******************************  HELPER FUNCTIONS *************************/



// Parse a line of input samples. Samples are just doubles separated by spaces, as follows:
//  -1.8569790e-004 -9.0919049e-004  3.6711283e-004 -1.0073081e-005 ...
// One line per sample, one double per microphone
double* parse_line(char* line, double* double_arr, int num_mic) {
    char *str;
    int i;
    unsigned char done = 0;
    double f;

    str = line;
    
    // Assuming we only have doubles and spaces on line
    while (!done && (str[0] != '\0')) {
        done = 0;
        for (i=0; i<num_mic; i++) {
            while (str[0] == 32) {
                str++;
            }
            
            sscanf(str, "%lf",&f);
//            printf("here4 -- %d\n",i);
            double_arr[i] = f;

            str = (char*) strchr(str,' ');
            if (str == NULL) {
                done = 1;
            }
        }
    }
    
    return double_arr;
}
    

// Convert a double array to a string
void double_arr_to_str(double *arr, int size, char* str) {
    int i;
    char str_tmp[MAX_LINE];

    sprintf (str,"Double array: ");
    
    for (i=0; i<size; i++) {
        sprintf (str_tmp," Element [%d]: %f",i,arr[i]);
        strcat(str,str_tmp);
    }
    strcat(str,"\n");
}

// Find the maximum element in an array of doubles. The return type is
// a long int... this is because we usually use this to calculate the
// maximum delay for a particular array, and we want that number to be
// the ceiling of the fractional maximum delay.
long int find_max_in_arr(double *arr, int size) {
    int i;
    double max = 0;

    for (i=0; i<size; i++) {
        if (arr[i] > max) {
            max = arr[i];
        }
    }

    return ceil(max);
}

// Find the minimum element in an array of doubles. The return type is
// a long int... this is because we usually use this to calculate the
// minimum delay for a particular array, and we want that number to be
// the floor of the fractional minimum delay.
long int find_min_in_arr(double *arr, int size) {
    int i;
    double min = 999e999;

    for (i=0; i<size; i++) {
        if (arr[i] < min ) {
            min = arr[i];
        }
    }

    return floor(min);
}


// Increment with wrap, i.e. if after adding offset to i, i exceeds
// max_i, wrap it around. For example, if i is 8, offset is 12, and
// max_i is 10, the result will come out to be (8+12-10-1) = 9. The
// -1 is because we have to include 0.
int wrapped_inc_offset(int i, int offset, int max_i) {
    if (i+offset > max_i) {
        return (i+offset-max_i-1);
    } else {
        return (i+offset);
    }
}

// Decrement with wrap, i.e. if after subtracting offset from i, i is
// less than 0, wrap it around from the other side. For example, if i
// is 8, offset is 12, and max_i is 10, the result will come out to be
// (10-(12-8)+1) = 7. The +1 is because we have to include 0.
int wrapped_dec_offset(int i, int offset, int max_i) {
    if (i-offset < 0) {
        return (max_i-(offset-i)+1);
    } else {
        return (i-offset);
    }
}


// Increment with wrap, i.e. if index exceeds max index, wrap back to
// 0
int wrapped_inc(int i, int max_i) {
    return wrapped_inc_offset(i,1,max_i);
}


// Decrement with wrap, i.e. if index is below 0, wrap back to max
int wrapped_dec(int i, int max_i) {
    return wrapped_dec_offset(i,1,max_i);
}

// Interpolate between two sample values to avoid quantization
// error. The idea is that we have a fractional delay, but obviously
// since the signal is sampled, we can only get real values for the
// signal at whole delay values. So, we can use linear approximation
// to estimate what the value of the signal would have been at the
// fractional delay.
double interpolate(double low_value, double high_value, double offset) {
    double val = ((high_value-low_value)*(offset)) + low_value;

//    printf("interpolation-- low_value: %lf;high_value: %lf;offset: %lf; interpolated: %lf\n", 
//           low_value, high_value, offset, val);
    
    return val;
}

/******************************  DATA STRUCTURE INITIALIZATION *************************/

// Initialize a DataQueue struct
struct DataQueue* init_data_queue(int max_delay, int num_mic) {
    int i, j;
    
    struct DataQueue *queue;
    // Initialize an array big enough to store max_delay+1 samples We
    // want it to be max_delay+1, so that we can keep max_delay
    // previous samples and the current sample

    
    queue = (struct DataQueue *) malloc(sizeof(struct DataQueue));
    queue->sample_queue = (double**) malloc((max_delay+1)*sizeof(double*));

    for (i=0; i<(max_delay+1);i++) {
        (queue->sample_queue)[i] = (double*) malloc(num_mic*sizeof(double));
        for (j=0; j<num_mic; j++) {
            (queue->sample_queue)[i][j] = 0.0; // Initialize values to 0
        }
    }

    queue->head = 0;
    queue->tail = 0;
    queue->full = 0;

    return queue;

}



// Intitialize the delay struct
struct Delays* init_delays (int num_angles, int num_mic) {
    struct Delays *delays;
    int i;
    
    delays = (struct Delays *) malloc(sizeof(struct Delays));

    // Initialize the delays array
    delays->delay_values = (double**) malloc(num_angles*sizeof(double*));
    delays->delay_info = (char**) malloc(num_angles*sizeof(char*));

    for (i=0; i<(NUM_ANGLES); i++) {
        delays->delay_values[i] = (double*) malloc(num_mic*sizeof(double));
        delays->delay_info[i] = (char*) malloc(MAX_LINE*sizeof(char));
    }
    
    return delays;
}

/******************************  DELAY/WEIGHTS/ENERGY MATH *************************/



// Calculate the cartesian distances from the source to each of the
// microphones
calc_distances(double *source_location, double mic_locations[NUM_MIC][3],
               double *distances,
               int num_mic) {
    int i;

    double source_x = source_location[0];
    double source_y = source_location[1];

    for (i=0; i<num_mic; i++) {
        distances[i]=CARTESIAN_DISTANCE(mic_locations[i][0],mic_locations[i][1],mic_locations[i][2],
                                        source_location[0],source_location[1],source_location[2]);
    }
}


// Calculate the delay (in # of samples) for each one of the microphones
calc_delays(double *distances, double *delays, int sound_speed, int sampling_rate, int num_mic) {
    int i;
    
    
    for (i=0; i<num_mic; i++) {
        // delay(in samples) = (distance to travel / speed of sound) * (# samples / second)
        delays[i] = (distances[i] / sound_speed) * sampling_rate;
    }
}


// Calculate the delay (in # of samples) for each one of the
// microphones using the far-field assumption (i.e. use only an
// azimuthal angle, and ignore the distance from the array to the
// source -- that means assuming the sound wavefront is planar and not
// spherical)
calc_far_field_delays(double mic_locations[NUM_MIC][3], double origin_location[3], double angle,
                      double *delays, 
                      int sound_speed, int sampling_rate, int num_mic) {
    int i;
    
    double cosine = cos(-angle);
    double sine = sin(-angle);
//    double cosine = cos(angle);
//    double sine = sin(angle);

    
    for (i=0; i<num_mic; i++) {
        // delay(in samples) = (- [(xm-x0)*(cos(theta))+(ym-y0)*(sin(theta))] / speed of sound) * (# samples / second)

        delays[i] = - (((mic_locations[i][0]-origin_location[0])*cosine) + 
                       ((mic_locations[i][1]-origin_location[1])*sine)) * sampling_rate / sound_speed;

/*        // alternative formula for array oriented along the z-axis
        // delay(in samples) = (- [(ym-y0)*(cos(theta))+(zm-z0)*(sin(theta))] / speed of sound) * (# samples / second)

        delays[i] = - (((mic_locations[i][1]-origin_location[1])*cosine) + 
                       ((mic_locations[i][2]-origin_location[2])*sine)) * sampling_rate / sound_speed;
*/

    }
}


// Make all the delays positive by adding the maximum delay to all of
// the delays
void positivize_delays(double* delays, int num_mic) {
    int i;
    long int max_delay = find_max_in_arr (delays, num_mic);

    for (i=0; i<num_mic; i++) {
        delays[i] += max_delay;
    }
}


// Adjust the delays by subtracting the smallest delay from each
// delay. This is done in order to minimize the size of our queue.
void adjust_delays(double* delays, int num_mic) {
    int i;
    long int min_delay = find_min_in_arr (delays, num_mic) - 1;

    for (i=0; i<num_mic; i++) {
        delays[i] -= min_delay;
    }
}


// Create a "cube" of delays around a given point, meaning extend by
// GRID_STEP_SIZE in each direction along the x, y, and z axes
double calc_target_cube_delays(double initial_location[3], double **target_locations, struct Delays *delays) {
    double current_location[3];
    double mic_distances[NUM_MIC];

    int i;
    char str[MAX_LINE];


    // Initialize a "cube" of delays
    
    // It's easier to just to it this way rather than try to roll it
    // into a loop.... errr I mean, I'm doing fancy loop unrolling for
    // effieciency :D
    target_locations[0][0] = initial_location[0]-GRID_STEP_SIZE; // x
    target_locations[0][1] = initial_location[1]; // y
    target_locations[0][2] = initial_location[2]; // z
    
    target_locations[1][0] = initial_location[0]+GRID_STEP_SIZE; // x
    target_locations[1][1] = initial_location[1]; // y
    target_locations[1][2] = initial_location[2]; // z

    target_locations[2][0] = initial_location[0]; // x
    target_locations[2][1] = initial_location[1]-GRID_STEP_SIZE; // y
    target_locations[2][2] = initial_location[2]; // z

    target_locations[3][0] = initial_location[0]; // x
    target_locations[3][1] = initial_location[1]+GRID_STEP_SIZE; // y
    target_locations[3][2] = initial_location[2]; // z

    target_locations[4][0] = initial_location[0]; // x
    target_locations[4][1] = initial_location[1]; // y
    target_locations[4][2] = initial_location[2]-GRID_STEP_SIZE; // z

    target_locations[5][0] = initial_location[0]; // x
    target_locations[5][1] = initial_location[1]; // y
    target_locations[5][2] = initial_location[2]+GRID_STEP_SIZE; // z

    target_locations[6][0] = initial_location[0]; // x
    target_locations[6][1] = initial_location[1]; // y
    target_locations[6][2] = initial_location[2]; // z


    for (i = 0; i < NUM_DIRS; i++) {
        calc_distances(target_locations[i], mic_locations, mic_distances, NUM_MIC);
        calc_delays(mic_distances, delays->delay_values[i], SOUND_SPEED, SAMPLING_RATE, NUM_MIC);

//        printf("Index %d, position:(%f,%f,%f)\n",i,target_locations[i][0],target_locations[i][1],target_locations[i][2]);
        sprintf(delays->delay_info[i],"Index %d, position:(%f,%f,%f)",i,target_locations[i][0],target_locations[i][1],target_locations[i][2]);
/*        printf ("Mic distances:\n");
        double_arr_to_str(mic_distances,NUM_MIC,str);
        printf ("%s",str);
        
        printf ("Mic delays(# samples):\n");
        double_arr_to_str(delays->delay_values[i],NUM_MIC,str);
        printf ("%s",str);
*/
    }        


}






// Calculate weights according to the Hamming window:
// an = 0.54+0.46cos(pi*n/N)
// where there are 2N+1 microphones
double *calc_weights_lr (int num_mic) {
    double* weights = (double*) malloc(num_mic*sizeof(double));
    int index = 0;
    int y, z;

    int half = num_mic/4;
    
    for (z = 1; z >= -1; z -= 2) {
        for (y = -half; y <= half; y++) {
            weights[index] = 0.54+0.46*cos(M_PI*y/half);
            index++;
        }
    }

    return weights;
}



// Calculate weights according to the Hamming window:
// an = 0.54+0.46cos(pi*n/N)
// where there are 2N+1 microphones
double *calc_weights_left_only (int num_mic) {
    double* weights = (double*) malloc(num_mic*sizeof(double));
    int index = 0;
    int y;

    int half = num_mic/2;
    
    for (y = -half; y <= half; y++) {
        weights[index] = 0.54+0.46*cos(M_PI*y/half);
        index++;
    }

    return weights;
}



// Calculate the energy in a signal by summing the squares of the values
double calculate_energy(double* samples, int num_samples) {
    int i;
    double sum = 0.0;
    char str[MAX_LINE*100];

//    double_arr_to_str(samples,num_samples,str);
//    printf ("%s",str);


    for (i=0; i<num_samples; i++) {
        sum += (samples[i]*samples[i]);
//        printf ("sum is %e\n",sum);
        
//        printf("%lf ^2 = %lf, energy is %lf\n", samples[i], samples[i]*samples[i], sum);


    }

    return sum;
}

/******************************  BEAMFORMING LOOKUP *************************/

// Take in a sample and a queue of previous samples, and output a
// beamformed one-channel sample, incorporating timing information
// from max_delay previous samples
double do_beamforming(double *delays, double **sample_queue, 
                      int queue_head, long int max_delay, int num_mic, double* weights) {
    int i;
    double sum=0;
    int delay_floor;
    int delay_ceil;
    int low_index;
    int high_index;
    int desired_index;
    double interpolated_value;
    double offset;
    
    // add up all the num_mic delayed samples
    for (i=0; i<num_mic; i++) {
        // low_index gets the quantized index right before the desired time index,
        // and high_index -- right after. See comments for wrapped_inc_offset.

        delay_floor = (int) floor(delays[i]);
        delay_ceil = (int) ceil(delays[i]);
        
        low_index = wrapped_inc_offset(queue_head,
                                       delay_floor,
                                       max_delay);


        high_index = wrapped_inc_offset(queue_head,
                                        delay_ceil,
                                        max_delay);
        
//        printf("for microphone: %d, queue_head: %d, low_index: %d, high_index: %d, desired_delay: %lf, desired_index: %lf\n",
//               i, queue_head, low_index, high_index, delays[i],
//               (low_index + (delays[i] - delay_floor))
//                         );

        
        // See comments for interpolate function
        interpolated_value = interpolate(sample_queue[low_index][i], 
                                         sample_queue[high_index][i], 
                                         delays[i]-delay_floor);
        

//        printf("value at low index: %e, value at high index: %e, interpolated value: %e\n",
//               sample_queue[low_index][i], sample_queue[high_index][i], interpolated_value);


        // If we have microphone weights, multiply the value by the weight
        if (weights != NULL) {
            sum+=(interpolated_value*weights[i]);
        } else {
            sum+=interpolated_value;
        }
    }

    return sum;
    
}


/******************************  RUNTIME LOOP *************************/


// Read in lines of the signal and run the beamforming algorithms on
// it.  The main idea is that we have a circular queue of samples. It
// is implemented via an array of max_delay+1 samples, so that we can
// keep max_delay previous samples and the current sample. Once the
// queue is "full", that is, we have max_delay+1 samples in it, we can
// start outputting beamformed results (delay each microphone by the
// appropriate amount by reading that amount into the future).
// Return 1 if end of data, 0 if still more to come!
int process_signal(FILE *fp, FILE *output_fp, struct Delays *delays, int num_mic, 
                   double sampling_rate, double** beamform_results, char* info_str, 
                   struct DataQueue *queue,
                   int num_beams, int window, double* weights) {
    char line[MAX_LINE];
    long int max_delay = 0;
    int i,j;
    int queue_head = 0;
//    int queue_tail = 0;
    unsigned char queue_full = 0;
    double time_index = 0;
    double time_index_inc = (1.0/sampling_rate);
    double storage_index;  // Index into the storage array beamform_result

    char str[MAX_LINE];

    double value;

//  queue = (struct DataQueue *) malloc(sizeof(struct DataQueue));


    
//    double **sample_queue = malloc((max_delay+1)*sizeof(double*));






    if (output_fp != NULL) {
        //        fprintf(output_fp,"; Sample Rate %d\t%s\n",SAMPLING_RATE,info_str);
    }

//    printf ("Max delay: %li\n",max_delay);
    
    i = 0;
//    while (fgets(line,MAX_LINE,fp)!=NULL) {
    
    // RMR { prime the sample queue (fill in max_delay-1 entries; last entry filled in main loop)
    // so that the samples from low index and high index in beamforming()
    // point to appropriate data
    for (i = 0; i < delays->max_delay-1; i++) {
        if (fgets(line,MAX_LINE,fp)!=NULL) {
            parse_line(line, (queue->sample_queue)[queue->head], NUM_MIC);
        } else { 
            return(-1);
        }
        queue->head = wrapped_inc(queue->head, delays->max_delay);
    }
    // } RMR

    // If window is <0, keep going until EOF
    for (i=0; (i<window) || (window<0) ; i++) {
//    printf("blah2\n");
        if (fgets(line,MAX_LINE,fp)!=NULL) {
            parse_line(line, (queue->sample_queue)[queue->head], NUM_MIC);
        } else { 
            return(1);
        }

//        printf ("time_index %lf\n",time_index);

//        if (queue->full == 1) {
            // output beamformed signal for max_delay samples back (in
            // other words, the sample that's located at
            // (queue->head+1) -- the next sample to get overwritten )

            // Note: we are using wrapped_inc(queue->head,max_delay)
            // rather than queue->head+1 since we don't want to overrun
            // the end of the array, i.e. if queue->head is max_delay,
            // wrapped_inc(queue->head,max_delay) will be one
        for (j=0; j<num_beams; j++) {
            value = do_beamforming(delays->delay_values[j], (queue->sample_queue), 
                                   wrapped_inc(queue->head,delays->max_delay),
                                   delays->max_delay, num_mic, weights);


            // normalize value by number of microphones
            value = value/num_mic;
            // It's the jth beam at the ith sample

            if (beamform_results != NULL) {
                beamform_results[j][i] = value;
            } else if (output_fp != NULL) {
                fprintf(output_fp,"%lf %lf\n",time_index, value);
            }
                
        }

            
        // Set the tail to the current head and increment the head by
        // one

        queue->tail = queue->head;
        queue->head = wrapped_inc(queue->head, delays->max_delay);


        time_index += time_index_inc;
    }


// Figure out what to do with this later
//    fprintf(output_fp,"%lf %lf\n",time_index, value);

    return (0);
}    






/******************************  EVALUATION LOGIC *************************/


// Calculate the beamforming result for a single point or
// direction. Returns if we are done or not
int calc_beamforming_result(FILE *fp, FILE *output_fp, struct Delays *delays, 
                            double** beamform_results, double* energies, 
                            char* info_str, 
                            struct DataQueue *queue,
                            int num_beams, int window,
                            int hamming) {
    
//    beamform_result = (double*) malloc(ENERGY_WINDOW_SIZE*sizeof(double));
//    energies = (double*) malloc(ENERGY_WINDOW_SIZE*sizeof(double));
    int i;
    int done; 
    double* weights=NULL;
    char str[MAX_LINE];
    

    if (hamming) {
        if ( (NUM_MIC%2) == 1 ) {
            // Assume Odd numbers are only the left microphones
            weights = calc_weights_left_only(NUM_MIC);
        } else {
            weights = calc_weights_lr(NUM_MIC);
        }

        printf ("Mic Weights:\n");
        double_arr_to_str(weights,NUM_MIC,str);
        printf ("%s",str);
    }


    done = process_signal(fp,output_fp,delays,NUM_MIC,SAMPLING_RATE, 
                          beamform_results, info_str, queue, num_beams, window, weights);

    if (beamform_results != NULL) {
        for (i = 0; i<num_beams; i++) {
            energies[i] = calculate_energy(beamform_results[i], window);
        }
    }
    
    return done;
}





// Seach the far_field_angles. Return the maximum energy
double search_far_field_angles(double* max_result, char* data_file, char *output_file, int hamming) {
    FILE *fp, *output_fp;
    
    double *energies = (double*) malloc(NUM_ANGLES*sizeof(double));
    double max_angle;
    double angle;

    char str[MAX_LINE];
    char info_str[MAX_LINE];
    double **beamform_results;
    double max_energy = 0;
    int i;
    long int tmp;
    int done = 0;
    double time_index = 0;
    double time_index_inc = ((double)ANGLE_ENERGY_WINDOW_SIZE)/((double)SAMPLING_RATE);

    // Intialize the delays struct
    struct Delays *delays = init_delays(NUM_ANGLES, NUM_MIC);

    struct DataQueue *queue;// = (struct DataQueue *) malloc(sizeof(struct DataQueue));

    // Open the output file

    // Initialize the results array
    beamform_results = (double**) malloc(NUM_ANGLES*sizeof(double*));

    for (i=0; i<(NUM_ANGLES); i++) {
        beamform_results[i] = (double*) malloc(ANGLE_ENERGY_WINDOW_SIZE*sizeof(double));
    }


    i = 0;
    for (angle = (-M_PI/2); angle <= (M_PI/2); angle+= (M_PI/NUM_ANGLES)) {
        calc_far_field_delays(mic_locations, origin_location, angle, 
                              delays->delay_values[i], SOUND_SPEED, SAMPLING_RATE, NUM_MIC);
        positivize_delays(delays->delay_values[i], NUM_MIC);
        sprintf(delays->delay_info[i], "angle is %lf\n", angle);

        i++;
    }

    delays->max_delay = 0;

    // Determine the maximum of all delays
    for (i = 0; i<NUM_ANGLES; i++) {
        tmp = find_max_in_arr (delays->delay_values[i], NUM_MIC);
        if (tmp > delays->max_delay) {
            delays->max_delay = tmp;
        }
    }        


    queue = init_data_queue(delays->max_delay, NUM_MIC);




    // Open the data file and process it
    if ((fp = fopen(data_file,"r")) == NULL) {
        printf("can't open %s\n",data_file);
        exit(1);
    } 
  

//        printf("1angle is %lf\n", angle);
        
//f    sprintf(info_str, "angle is %lf\n", angle);
//        printf("2angle is %lf\n", angle);
    
    

    while (done == 0) {
        max_energy = 0;
        max_angle = 0;


        // Get beamforming results for all NUM_ANGLES directions
        done = calc_beamforming_result(fp, NULL /*output_fp*/, delays, beamform_results, 
                                       energies, info_str, queue, NUM_ANGLES, ANGLE_ENERGY_WINDOW_SIZE, hamming);


        // Now, figure out which angle has the maximum energy
        for (i = 0; i<NUM_ANGLES; i++) {
//            printf("%lf %lf %lf\n",time_index,(i-NUM_ANGLES/2)*M_PI/NUM_ANGLES,energies[i]);

            if (energies[i]>max_energy) {
                max_energy = energies[i];
                max_angle = (i-NUM_ANGLES/2)*M_PI/NUM_ANGLES;
                memcpy(max_result, beamform_results[i], ANGLE_ENERGY_WINDOW_SIZE);
            }
        }

        
        // At this point, just print out the result. Later, figure out
        // how to track it or whatever.
        printf("at time index %lf, maximum energy was %lf, occuring at angle %lf (radians)\n",
               time_index, max_energy, max_angle);

        // It always does an extra one -- why? Need to figure this out
        
        time_index += time_index_inc;
    }
    
    fclose(fp);

//    fclose(output_fp);

    return(max_energy);
}




void calc_single_pos(double source_location[3], double mic_locations[NUM_MIC][3], int hamming, char* data_file, char* output_file) {
    double mic_distances[NUM_MIC];
//    double delays[NUM_MIC];
    char str[MAX_LINE];
    char info_str[MAX_LINE];
    double energy;
    FILE *fp;
    FILE *output_fp;
    double *beamform_result;
    // Intialize the delays struct for only one set of delays
    struct Delays *delays = init_delays(1, NUM_MIC);
    struct DataQueue *queue;// = (struct DataQueue *) malloc(sizeof(struct DataQueue));
    

    double **beamform_results;
    double *weights = NULL;


    // Initialize the results array
//    beamform_results = (double**) malloc(sizeof(double*));
//
//    for (i=0; i<(NUM_ANGLES); i++) {
//        beamform_results[i] = (double*) malloc(*sizeof(double));
//    }


    // Calculate distances from source to each of mics
    calc_distances(source_location, mic_locations, mic_distances, NUM_MIC);
    calc_delays(mic_distances, delays->delay_values[0], SOUND_SPEED, SAMPLING_RATE, NUM_MIC);
    
    
    printf ("Mic distances:\n");
    double_arr_to_str(mic_distances,NUM_MIC,str);
    printf ("%s",str);
    
    
    printf ("Mic delays(# samples):\n");
    double_arr_to_str(delays->delay_values[0],NUM_MIC,str);
    printf ("%s",str);
    
    
    adjust_delays(delays->delay_values[0], NUM_MIC);
    printf ("Adjusted Mic delays(# samples):\n");
    double_arr_to_str(delays->delay_values[0],NUM_MIC,str);
    printf ("%s",str);


/*    if (hamming) {
        if ( (NUM_MIC%2) == 1 ) {
            // Assume Odd numbers are only the left microphones
            weights = calc_weights_left_only(NUM_MIC);
        } else {
            weights = calc_weights_lr(NUM_MIC);
        }

        printf ("Mic Weights:\n");
        double_arr_to_str(weights,NUM_MIC,str);
        printf ("%s",str);
    }
*/
    delays->max_delay = find_max_in_arr (delays->delay_values[0], NUM_MIC);

    queue = init_data_queue(delays->max_delay, NUM_MIC);

    sprintf (info_str, "source position is (%lf,%lf,%lf)\n", source_location[0], source_location[1], source_location[2]);

    // Open the output file
    if ((output_fp = fopen(output_file,"w")) == NULL) {
        printf("can't open %s\n",output_file);
        exit(1);
    } 

    // Open the data file and process it
    if ((fp = fopen(data_file,"r")) == NULL) {
        printf("can't open %s\n",data_file);
        exit(1);
    } 


    // number of beams = 1, energy window size = -1 results are NULL
    // because we don't necessarily know how many results there will
    // be in advance... since we are going to EOF
    calc_beamforming_result(fp, output_fp, delays, NULL, 
                            NULL, info_str, queue, 1, -1, hamming);

    fclose(fp);
    fclose(output_fp);

//    return energy;
}


// Search along a grid of points, each time perturbing each of the x,
// y, and z coordinates, and focusing on the point with the maximum
// energy.
double search_grid(double initial_location[3], char* data_file, char* output_file, int hamming) {
    FILE *fp, *output_fp;
    
//    double energy;
    double *energies = (double*) malloc(NUM_ANGLES*sizeof(double));
    double max_angle;
    double angle;
    char str[MAX_LINE];
    char info_str[MAX_LINE];
    double **beamform_results;
    double max_energy = 0;
    int i;
    long int tmp;
    int done = 0;
    double time_index = 0;
    double time_index_inc = ((double)GRID_ENERGY_WINDOW_SIZE)/((double)SAMPLING_RATE);
    double mic_distances[NUM_MIC];
    double **target_locations;
    double *max_result = (double*) malloc(ANGLE_ENERGY_WINDOW_SIZE*sizeof(double));
    double current_location[3];
    double *max_location;

    double x,y,z;

    // Intialize the delays struct
    struct Delays *delays = init_delays(NUM_ANGLES, NUM_MIC);

    struct DataQueue *queue;

    // Open the data file and process it
    if ((fp = fopen(data_file,"r")) == NULL) {
        printf("can't open %s\n",data_file);
        exit(1);
    } 
    

    // Initialize the results array
    beamform_results = (double**) malloc(NUM_DIRS*sizeof(double*));

    for (i=0; i<(NUM_ANGLES); i++) {
        beamform_results[i] = (double*) malloc(GRID_ENERGY_WINDOW_SIZE*sizeof(double));
    }

    // Initialize the locations array
    target_locations = (double**) malloc(NUM_DIRS*sizeof(double*));

    for (i=0; i<(NUM_DIRS); i++) {
        target_locations[i] = (double*) malloc(3*sizeof(double));
    }

//    printf("Starting reference position is :(%f,%f,%f)\n",initial_location[0],initial_location[1],initial_location[2]);

    memcpy(current_location, initial_location, 3*sizeof(double));
    printf("Starting reference position is :(%f,%f,%f)\n",current_location[0],current_location[1],current_location[2]);

    while (done == 0) {
        calc_target_cube_delays(current_location, target_locations, delays);
        
        delays->max_delay = 0;
        
        // Determine the maximum of all delays
        for (i = 0; i<NUM_DIRS; i++) {
            tmp = find_max_in_arr (delays->delay_values[i], NUM_MIC);
            if (tmp > delays->max_delay) {
                delays->max_delay = tmp;
            }
        }        
        
        queue = init_data_queue(delays->max_delay, NUM_MIC);
        


        // Get the beamforming result for all of the NUM_DIRS surrounding points
        done = calc_beamforming_result(fp, NULL, delays, beamform_results, 
                                       energies, info_str, queue, NUM_DIRS, GRID_ENERGY_WINDOW_SIZE, hamming);
        

        // Figure out which position has the max energy
        for (i = 0; i<NUM_DIRS; i++) {
//            printf("%lf %lf %lf\n",time_index,i);
            printf ("%s, Energy: %lf\n", delays->delay_info[i],energies[i]);
            
            if (energies[i]>max_energy) {
                max_energy = energies[i];
//                max_angle = (i-NUM_ANGLES/2)*M_PI/NUM_ANGLES;
                memcpy(max_result, beamform_results[i], ANGLE_ENERGY_WINDOW_SIZE);
                max_location = target_locations[i];
            }
        }


        // Adjust the current location to the location with the maximum energy

        printf("New reference position is :(%f,%f,%f)\n",max_location[0],max_location[1],max_location[2]);

        memcpy(current_location, max_location, 3*sizeof(double));
        

//        printf("at time index %lf, maximum energy was %lf, occuring at angle %lf\n",
//               time_index, max_energy, max_angle);

        // It always does an extra one -- why?
        
        time_index += time_index_inc;
    }
    

    fclose(fp);


}




print_usage () {
    fprintf (stderr, "Usage: delay_and_sum [options]\n\n");
    fprintf (stderr,"General options\n");
    fprintf (stderr,"  -data_file <filename>       Data file to get input from\n");
    fprintf (stderr,"  -output_file <filename>     File to output result to\n");
    fprintf (stderr,"  -search_far_field           Search far field angles from -90 to +90 degrees\n");
    fprintf (stderr,"  -hill_climb                 Search for maximum energy via hill-climbing\n");
    fprintf (stderr,"  -hamming                    Apply hamming window weighing to microphones\n");
}



main (int argc, char* argv[]) {
    char *data_file=NULL;
    char *output_file=NULL;

    FILE *fp;
    FILE *output_fp;
    int i;
    char* key;

    double *max_result;
    double max_energy;
    char search_far_field = 0;
    char hill_climb = 0;
    char hamming = 0;
    

    if (argc>1) {
        if (strstr(argv[1], "-h")) {
            print_usage();
            exit(1);
        } else {
            for (i=1;i<argc;i++) { 
                key = argv[i];
                if (strcmp(key, "-data_file") == 0) {
                    data_file = (char *) strdup(argv[++i]);
                } else if (strcmp(key, "-output_file") == 0) {
                    output_file = (char *) strdup(argv[++i]);
                } else if (strcmp(key, "-search_far_field") == 0) {
                    search_far_field = 1;
                } else if (strcmp(key, "-hill_climb") == 0) {
                    hill_climb = 1;
                } else if (strcmp(key, "-hamming") == 0) {
                    hamming = 1;
                } else {
                    fprintf (stderr, "Error: unknown option: %s\n",key);
                    print_usage();
                    exit(1);
                }
            }
        }
    }

    if (!data_file) {
        fprintf (stderr, "You must specify a data file\n");
        print_usage();
        exit(1);
    }

    for(i = 0; i<NUM_MIC; i++) {
        printf("Position for microphone %d:(%f,%f,%f)\n",i,mic_locations[i][0],mic_locations[i][1],mic_locations[i][2]);
    }

    printf("Position for source: (%f,%f,%f)\n",source_location[0],source_location[1],source_location[2]);



    if (search_far_field == 1) {
        max_result = (double*) malloc(ANGLE_ENERGY_WINDOW_SIZE*sizeof(double));


        // Seach the far_field_angles. Return the maximum result, angle, and energy
        max_energy = search_far_field_angles(max_result, data_file, output_file, hamming);
    } else if (hill_climb == 1) {
        search_grid(source_location, data_file, output_file, hamming);
    } else {
        calc_single_pos(source_location, mic_locations, hamming, data_file, output_file);
    }




//    process_signal(fp,output_fp,delays,NUM_MIC,SAMPLING_RATE);
//    process_signal(fp,output_fp,delays,NUM_MIC,SAMPLING_RATE, beamform_result, info_str);



//    fclose(fp);

//    printf ("blah\n");

    exit(0);
}


