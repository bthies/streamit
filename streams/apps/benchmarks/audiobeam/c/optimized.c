#include "optimized.h"
#include <sys/time.h>

#define NUM_MIC 15
//#define NUM_MIC 1024
//#define NUM_MIC 14
#define ANGLE_ENERGY_WINDOW_SIZE 400
#define NUM_ANGLES 180
#define GRID_STEP_SIZE 0.003  // .3cm
#define GRID_ENERGY_WINDOW_SIZE 3200 // (samples) = 200ms
#define NUM_DIRS 7
#define NUM_TILES 16

#define MIC_HORIZ_SPACE 0.038257
#define MIC_VERT_SPACE 0.015001
#define TWO23        8388608.0    // 2^23
//#define BUFFER_DATA
#define BUFFER_SIZE 20000 //1600
#define NUM_MIC_IN_CHAIN 32
#define NUM_BOARDS_IN_CHAIN 16


#define INTERPOLATE(low_value, high_value, offset) (((high_value-low_value)*(offset)) + low_value)

// 15-microphone array for data set from Kevin

float mic_locations[NUM_MIC][3] = {
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

// For Kevin's data
float source_location[] = {1.1677,2.1677,1};

// In front of RAW - Meters from center of array
//float source_location[] = {0.79692, -1.118859, 0.058023};
void preprocess_delays(struct PreprocessedDelays prep_delays[], float* delays)
{
    int i;

    for (i=0; i < NUM_MIC; i++) {
        prep_delays[i].delay = delays[i];
        prep_delays[i].high = (int) ceil(delays[i]);
        prep_delays[i].low = (int) floor(delays[i]);
        prep_delays[i].offset = delays[i] - prep_delays[i].low;
    }
}



float origin_location[] = {1.5,3,0};  // center of the array


/******************************  HELPER FUNCTIONS *************************/
// Parse a line of input samples. Samples are just floats separated by spaces, as follows:
//  -1.8569790e-004 -9.0919049e-004  3.6711283e-004 -1.0073081e-005 ...
// One line per sample, one float per microphone
float* parse_line(char* line, float* float_arr, int num_mic) {
    char *str;
    int i;
    unsigned char done = 0;
    float f;

    str = line;
    
    // Assuming we only have floats and spaces on line
    while (!done && (str[0] != '\0')) {
        done = 0;
        for (i=0; i<num_mic; i++) {
            while (str[0] == 32) {
                str++;
            }
            
            sscanf(str, "%f",&f);
            //            printf("here4 -- %d\n",i);
            float_arr[i] = f;

            str = (char*) strchr(str,' ');
            if (str == NULL) {
                done = 1;
            }
        }
    }
    
    return float_arr;
}

void float_arr_to_str(float *arr, int size, char* str) {
    int i;
    char str_tmp[MAX_LINE];
    int multiplier = 32768;
    
    sprintf (str,"Float array: ");
    
    for (i=0; i<size; i++) {
        sprintf (str_tmp," Element [%d]: %f %d",i,arr[i],(int)floor(arr[i]*multiplier));
        strcat(str,str_tmp);
    }
    strcat(str,"\n");
}

// Find the maximum element in an array of floats. The return type is
// a long int... this is because we usually use this to calculate the
// maximum delay for a particular array, and we want that number to be
// the ceiling of the fractional maximum delay.
long int find_max_in_arr(float *arr, int size) {
    int i;
    float max = 0;

    for (i=0; i<size; i++) {
        if (arr[i] > max) {
            max = arr[i];
        }
    }

    return ceil(max);
}

// Find the minimum element in an array of floats. The return type is
// a long int... this is because we usually use this to calculate the
// minimum delay for a particular array, and we want that number to be
// the floor of the fractional minimum delay.
long int find_min_in_arr(float *arr, int size) {
    int i;
    float min = 999e999;

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
float interpolate(float low_value, float high_value, float offset) {
    float val = ((high_value-low_value)*(offset)) + low_value;
    int multiplier = 32768;

    //    printf("interpolation-- low_value: %f;high_value: %f;offset: %f; interpolated: %f\n", 
    //           low_value, high_value, offset, val);
    //    printf("interpolation-- low_value: %d;high_value: %d;offset: %f; interpolated: %f\n", 
    //           (int)floor(low_value*multiplier), (int)floor(high_value*multiplier), offset, val);
    
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
    queue->sample_queue = (float**) malloc((max_delay+1)*sizeof(float*));

    for (i=0; i<(max_delay+1);i++) {
        (queue->sample_queue)[i] = (float*) malloc(num_mic*sizeof(float));
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
    delays->delay_values = (float**) malloc(num_angles*sizeof(float*));
    delays->delay_info = (char**) malloc(num_angles*sizeof(char*));

    for (i=0; i<(NUM_ANGLES); i++) {
        delays->delay_values[i] = (float*) malloc(num_mic*sizeof(float));
        delays->delay_info[i] = (char*) malloc(MAX_LINE*sizeof(char));
    }
    
    return delays;
}

/******************************  DELAY/WEIGHTS/ENERGY MATH *************************/



// Calculate the cartesian distances from the source to each of the
// microphones
calc_distances(float *source_location, float mic_locations[NUM_MIC][3],
               float *distances,
               int num_mic) {
    int i;

    float source_x = source_location[0];
    float source_y = source_location[1];

    for (i=0; i<num_mic; i++) {
        distances[i]=CARTESIAN_DISTANCE(mic_locations[i][0],mic_locations[i][1],mic_locations[i][2],
                                        source_location[0],source_location[1],source_location[2]);
    }
}


// Calculate the delay (in # of samples) for each one of the microphones
calc_delays(float *distances, float *delays, int sound_speed, int sampling_rate, int num_mic) {
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
calc_far_field_delays(float mic_locations[NUM_MIC][3], float origin_location[3], float angle,
                      float *delays, 
                      int sound_speed, int sampling_rate, int num_mic) {
    int i;
    
    float cosine = cos(-angle);
    float sine = sin(-angle);
    //    float cosine = cos(angle);
    //    float sine = sin(angle);

    
    for (i=0; i<num_mic; i++) {
        // delay(in samples) = (- [(xm-x0)*(cos(theta))+(ym-y0)*(sin(theta))] / speed of sound) * (# samples / second)

        delays[i] = - (((mic_locations[i][0]-origin_location[0])*cosine) + 
                       ((mic_locations[i][1]-origin_location[1])*sine)) * sampling_rate / sound_speed;

        /*
        // alternative formula for array oriented along the z-axis
        // delay(in samples) = (- [(ym-y0)*(cos(theta))+(zm-z0)*(sin(theta))] / speed of sound) * (# samples / second)

        delays[i] = - (((mic_locations[i][1]-origin_location[1])*cosine) + 
        ((mic_locations[i][2]-origin_location[2])*sine)) * sampling_rate / sound_speed;
        */

    }
}


// Make all the delays positive by adding the maximum delay to all of
// the delays
void positivize_delays(float* delays, int num_mic) {
    int i;
    long int max_delay = find_max_in_arr (delays, num_mic);

    for (i=0; i<num_mic; i++) {
        delays[i] += max_delay;
    }
}


// Adjust the delays by subtracting the smallest delay from each
// delay. This is done in order to minimize the size of our queue.
void adjust_delays(float* delays, int num_mic) {
    int i;
    long int min_delay = find_min_in_arr (delays, num_mic) - 1;

    for (i=0; i<num_mic; i++) {
        delays[i] -= min_delay;
    }
}


// Create a "cube" of delays around a given point, meaning extend by
// GRID_STEP_SIZE in each direction along the x, y, and z axes
float calc_target_cube_delays(float initial_location[3], float **target_locations, struct Delays *delays) {
    float current_location[3];
    float mic_distances[NUM_MIC];

    int i;
    char str[MAX_LINE];


    // Initialize a "cube" of delays
    
    // It's easier to just do it this way rather than try to roll it
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
        //        sprintf(delays->delay_info[i],"Index %d, position:(%f,%f,%f)",i,target_locations[i][0],target_locations[i][1],target_locations[i][2]);
        /*        printf ("Mic distances:\n");
                  float_arr_to_str(mic_distances,NUM_MIC,str);
                  printf ("%s",str);
        
                  printf ("Mic delays(# samples):\n");
                  float_arr_to_str(delays->delay_values[i],NUM_MIC,str);
                  printf ("%s",str);
        */
    }        


}





// Calculate weights according to the Hamming window:
// an = 0.54+0.46cos(pi*n/N)
// where there are 2N+1 microphones
float *calc_weights_lr (int num_mic) {
    float* weights = (float*) malloc(num_mic*sizeof(float));
    int index = 0;
    int y, z;

    int half = num_mic/4;
    
    for (z = 1; z >= -1; z -= 2) {
        for (y = 0; y < half; y++) {
            weights[index] = 0.54+0.46*cos(M_PI*y/half);
            index++;
        }
        for (y = 0; y < half; y++) {
            weights[index] = 0.54+0.46*cos(M_PI*(-y)/half);
            index++;
        }


    }

    /*
      for (z = 1; z >= -1; z -= 2) {
      for (y = -half; y <= half; y++) {
      weights[index] = 0.54+0.46*cos(M_PI*y/half);
      index++;
      }
      }

    */    return weights;
}


// Calculate weights according to the Hamming window:
// an = 0.54+0.46cos(pi*n/N)
// where there are 2N+1 microphones
float *calc_weights_left_only (int num_mic) {
    float* weights = (float*) malloc(num_mic*sizeof(float));
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
float calculate_energy(float* samples, int num_samples) {
    int i;
    float sum = 0.0;
    char str[MAX_LINE*100];

    //    float_arr_to_str(samples,num_samples,str);
    //    printf ("%s",str);


    for (i=0; i<num_samples; i++) {
        sum += (samples[i]*samples[i]);
        //        printf ("sum is %e\n",sum);
        
        //        printf("%f ^2 = %f, energy is %f\n", samples[i], samples[i]*samples[i], sum);


    }

    return sum;
}

/******************************  BEAMFORMING LOOKUP *************************/


// Figure out the first and last microphone index for this tile to process
void figure_which_mics(int tile_num, int* first_mic, int* last_mic) {
    // Get the "slice" of microphones for this tile to work on. We
    // want to get the ceil of our share of microphones, and the last
    // tile may get less than the others in case the number of
    // microphones does not divide evenly by the number of tiles on
    // the chip
    
    int slice_size = ceil(((float)NUM_MIC)/((float)NUM_TILES));
    //    int i;
    //    raw_test_pass_reg(259);
    //    raw_test_pass_reg(slice_size);

    *first_mic = tile_num*slice_size;
    *last_mic = (tile_num+1)*slice_size-1;

    if (*last_mic >= NUM_MIC) {
        *last_mic = NUM_MIC-1;
    }
}

// Take in a sample and a queue of previous samples, and output a
// beamformed one-channel sample, incorporating timing information
// from max_delay previous samples
float do_beamforming(struct PreprocessedDelays preprocessed_delays[],
                     float *delays, float **sample_queue, 
                     int queue_head, long int max_delay, int num_mic, float* weights) {
    int i;
    float sum=0;
    int delay_floor;
    int delay_ceil;
    int low_index;
    int high_index;
    int desired_index;
    float interpolated_value;
    float offset;
    
    // add up all the num_mic delayed samples
    for (i=0; i<num_mic; i++) {
        delay_floor = preprocessed_delays[i].low;
        delay_ceil  = preprocessed_delays[i].high;

        // Inline wrap around here
        // Low index gets index of sample right before desired sample
        low_index = queue_head + delay_floor;
        if (low_index > max_delay) {
            low_index -= (max_delay+1);
        }

        // High index gets index of sample right after desired sample
        high_index = queue_head + delay_ceil;
        if (high_index > max_delay) {
            high_index -= (max_delay+1);
        }

        // i gives the value of the microphone we want. However, since
        // the array only has microphones first_mic to last_mic, we
        // need to offset our index by first_mic

        /*
          interpolated_value = interpolate(sample_queue[low_index][i], 
          sample_queue[high_index][i], 
          preprocessed_delays[i].offset);
        */


        interpolated_value = INTERPOLATE(sample_queue[low_index][i], 
                                         sample_queue[high_index][i], 
                                         preprocessed_delays[i].offset);

        /*
          printf("interpolation for mic # %d -- low_value at index %d: %d;high_value at index %d: %d;offset: %f; interpolated: %f\n", 
          i,
          low_index, 
          (int)(sample_queue[low_index][i]*TWO23), 
          high_index,
          (int)(sample_queue[high_index][i]*TWO23), 
          preprocessed_delays[i].offset, interpolated_value);
        */

        // If we have microphone weights, multiply the value by the weight
        if (weights != NULL) {
            sum+=(interpolated_value*weights[i]);
        } else {
            sum+=interpolated_value;
        }
    }

    return sum;
    
}

void initialize_microphone_locations_lr(float locations[NUM_MIC][3])
{
    int x, y, z;
    int half = NUM_MIC/4;
    int index = 0;
    float offset = .5*MIC_HORIZ_SPACE;

    x = 0;
    for (z = 1; z >= -1; z -= 2) {
        for (y = 0; y < half; y++) {
            locations[index][0] = x*0.0;
            locations[index][1] = (-offset) - y*MIC_HORIZ_SPACE;
            locations[index][2] = z*MIC_VERT_SPACE;
            index++;
        }
        for (y = 0; y < half; y++) {
            locations[index][0] = x*0.0;
            locations[index][1] = offset + y*MIC_HORIZ_SPACE;
            locations[index][2] = z*MIC_VERT_SPACE;
            index++;
        }
    }
}

void initialize_microphone_locations_left_only(float locations[NUM_MIC][3])
{
    int x, y, z;
    int half = NUM_MIC/2;
    int index = 0;
    x = 0;
    z = -1;
    for (y = -half; y <= half; y++) {
        locations[index][0] = x*0.0;
        locations[index][1] = y*MIC_HORIZ_SPACE;
        locations[index][2] = z*MIC_VERT_SPACE;
        index++;

    }
}

/*
  void initialize_microphone_locations(float locations[NUM_MIC][3])
  {
  if ( (NUM_MIC%2) == 1 ) {
  // Assume Odd numbers are only the left microphones
  initialize_microphone_locations_left_only(locations);
  } else {
  initialize_microphone_locations_lr(locations);
  }
  }
*/
void set_mic_location(float locations[NUM_MIC][3],int index,float x, float y, float z)
{
    if (index >=0 && index < NUM_MIC) {
        locations[index][0] = x;
        locations[index][1] = y;
        locations[index][2] = z;
    } else {
        exit(1);
    }
}


void initialize_microphone_locations(float locations[NUM_MIC][3])
{
    float x, y, z;
    int index = 0;
    float center = 0.0;
    int first_right_index = NUM_MIC/2;
    int chain;
    int num_chains = ceil(((float)NUM_MIC)/((float)NUM_MIC_IN_CHAIN));

    x = 0;
    z = 0;

    //
    // vertical microphones
    // 
    for (chain=0; chain < num_chains; chain++) {

        /*
          if (chain == 0) y = -1.5;
          if (chain == 1) y = -3.5;
          if (chain == 2) y = 2.5;
          if (chain == 3) y = 0.5;
        */

        // Generalizing this

        if (chain<(num_chains/2)) {
            y = 0-1.5-(2*chain);
        } else {
            y = 0.5+(2*(num_chains-chain-1));
        }            


        index = chain*NUM_BOARDS_IN_CHAIN;
        for (z = 1; (z <= NUM_BOARDS_IN_CHAIN) && (index < NUM_MIC) ; z++) {
            // Left
            set_mic_location(locations,index,0.0,y*MIC_HORIZ_SPACE,z*MIC_VERT_SPACE);
            // Right
            // only set the right one if it's within NUM_MIC
            if (index+first_right_index < NUM_MIC) {
                set_mic_location(locations,index+first_right_index,0.0,(y+1)*MIC_HORIZ_SPACE,z*MIC_VERT_SPACE);
            }
            index++;
        }
    }
}


__inline__ unsigned long long int rdtsc()
{
    unsigned long long int x;
    __asm__ volatile (".byte 0x0f, 0x31" : "=A" (x));
    return x;
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
                   float sampling_rate, float** beamform_results, char* info_str, 
                   struct DataQueue *queue,
                   int num_beams, int window, float* weights) {
    char line[MAX_LINE];
    long int max_delay = 0;
    int i,j, k;
    int queue_head = 0;
    //    int queue_tail = 0;
    unsigned char queue_full = 0;
    float time_index = 0;
    float time_index_inc = (1.0/sampling_rate);
    float storage_index;  // Index into the storage array beamform_result

    char str[MAX_LINE];

    float value;

    int buffer = 0;

    struct timeval start_time, end_time;
    int total_usecs;

    unsigned long long int start_cycle_count;
    unsigned long long int end_cycle_count;
    unsigned long long int total_cycle_count;
    unsigned long long int cycle_count;

    int done =0;
    double total_time = 0;

#ifdef BUFFER_DATA
    int **tmp_int_buffer;
    float **data_buffer;

    // Initialize the buffer array
    data_buffer = (float**) malloc(BUFFER_SIZE*sizeof(float*));
    tmp_int_buffer = (int**) malloc(BUFFER_SIZE*sizeof(int*));

    for (i=0; i<(BUFFER_SIZE); i++) {
        data_buffer[i] = (float*) malloc(NUM_MIC*sizeof(float));
        tmp_int_buffer[i] = (int*) malloc(NUM_MIC*sizeof(int));
    }
#endif

    // Pre-calulated ceiling, floor and offset values for each delay
    struct PreprocessedDelays preprocessed_delays[NUM_MIC];
    
    // Pre-calculate delay values
    preprocess_delays(preprocessed_delays, delays->delay_values[0]);

    //  queue = (struct DataQueue *) malloc(sizeof(struct DataQueue));
    //    float **sample_queue = malloc((max_delay+1)*sizeof(float*));

    //    if (output_fp != NULL) {
    //        fprintf(output_fp,"; Sample Rate %d\t%s\n",SAMPLING_RATE,info_str);
    //    }

    //    printf ("Max delay: %li\n",max_delay);
    
    i = 0;
    //    while (fgets(line,MAX_LINE,fp)!=NULL) {

#ifdef BUFFER_DATA
    buffer = 1;

    i = 0;
    if (buffer  == 1) {
        while (fgets(line,MAX_LINE,fp)!=NULL) {
            parse_line(line, data_buffer[i], NUM_MIC);

            //            printf("parsing current line");
            //            float_arr_to_str(data_buffer[i], NUM_MIC, str);
            //            printf("%s", str);
            //            printf("i: %d\n",i);

            // Copy into the int array so that we can simulate the division later
            for (j=0; j< NUM_MIC; j++) {
                tmp_int_buffer[i][j] = (int)((data_buffer[i][j]*TWO23));
            }

            i++;
        }
    }

    //    printf("read %d lines\n",i);
#endif
   

    // RMR { prime the sample queue (fill in max_delay-1 entries, the last entry is filled in the mail loop)
    for (i = 0; i < delays->max_delay - 1; i++) {
        if (fgets(line,MAX_LINE,fp)!=NULL) {
            parse_line(line, (queue->sample_queue)[queue->head], NUM_MIC);
        } else { 
            return(-1);
        }
        queue->head = wrapped_inc(queue->head, delays->max_delay);
    }
    // } RMR

    /* First, call gettimeofday() to get start time */
    //    printf("blah\n");

    //    for (k=0; k<=100; k++) 
    {
        //        printf("k:%d\n",k);
        
        gettimeofday(&start_time, NULL);
        start_cycle_count = rdtsc();
   
        // If window is <0, keep going until EOF
        for (i=0; (i<window) || (window<0) ; i++) {
#ifdef BUFFER_DATA
            if (buffer == 1) {
                if (i>=BUFFER_SIZE) {
                    done = 1;
                    //                    printf("buffer size exceeded\n");
                    //                    printf("processed %d lines\n",i);
                    break;

                    //                    exit (0);
                
                }
                

                //                memcpy((queue->sample_queue)[queue->head], data_buffer[i], NUM_MIC*sizeof(float));

                // conver the tmp_int_array back to the buffer
                for (j=0;j<NUM_MIC;j++) {
                    (queue->sample_queue)[queue->head][j] = ((float)tmp_int_buffer[i][j])/TWO23;
                }


            } else 
#endif
            if (fgets(line,MAX_LINE,fp)!=NULL) {
                parse_line(line, (queue->sample_queue)[queue->head], NUM_MIC);
            } else { 
                done = 1;
                //                printf("window exceeded\n");
                break;
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
            //        printf("num_beams: %d\n",num_beams);
            for (j=0; j<num_beams; j++) {
                value = do_beamforming(preprocessed_delays,
                                       delays->delay_values[j], (queue->sample_queue), 
                                       wrapped_inc(queue->head,delays->max_delay),
                                       delays->max_delay, num_mic, weights);


                // normalize value by number of microphones
                value = value/num_mic;
                // It's the jth beam at the ith sample

                if (beamform_results != NULL) {
                    beamform_results[j][i] = value;
                } else if (output_fp != NULL) {
                    // put this back later
                    fprintf(output_fp,"%lf %lf\n",time_index, value);//*TWO23);
                }

                //                fprintf(output_fp,"%f %d\n",time_index, (int)(value*TWO23));
            }

            
            // Set the tail to the current head and increment the head by
            // one

            queue->tail = queue->head;
            queue->head = wrapped_inc(queue->head, delays->max_delay);


            time_index += time_index_inc;
        }


        end_cycle_count = rdtsc();

        /* Now call gettimeofday() to get end time */
        gettimeofday(&end_time, NULL);  /* after time */
    
        /* Print the execution time */
        total_usecs = (end_time.tv_sec-start_time.tv_sec) * 1000000 +
            (end_time.tv_usec-start_time.tv_usec);
        
        cycle_count = end_cycle_count-start_cycle_count;
        printf("Total time was %f Sec.\n", ((float)total_usecs)/1000000);
        printf("Total cycle count was %ld.\n", cycle_count);
    

        total_time += ((float)total_usecs/1000000);

        total_cycle_count += cycle_count;

    }

    //    printf("Average cycle count was %ld.\n", (total_cycle_count/k));
    //    printf("Average time was %f Sec.\n", (total_time/k));


    // Figure out what to do with this later
    //    fprintf(output_fp,"%lf %lf\n",time_index, value);
    return (done);
}    



/******************************  EVALUATION LOGIC *************************/


// Calculate the beamforming result for a single point or
// direction. Returns if we are done or not
int calc_beamforming_result(FILE *fp, FILE *output_fp, struct Delays *delays, 
                            float** beamform_results, float* energies, 
                            char* info_str, 
                            struct DataQueue *queue,
                            int num_beams, int window,
                            int hamming) {
    
    //    beamform_result = (float*) malloc(ENERGY_WINDOW_SIZE*sizeof(float));
    //    energies = (float*) malloc(ENERGY_WINDOW_SIZE*sizeof(float));
    int i;
    int done; 
    float* weights=NULL;
    char str[MAX_LINE];

    struct timeval start_time, end_time;
    long int total_usecs;

    if (hamming) {
        if ( (NUM_MIC%2) == 1 ) {
            // Assume Odd numbers are only the left microphones
            weights = calc_weights_left_only(NUM_MIC);
        } else {
            weights = calc_weights_lr(NUM_MIC);
        }

        //        printf ("Mic Weights:\n");
        //        float_arr_to_str(weights,NUM_MIC,str);
        //        printf ("%s",str);
    }



    /* First, call gettimeofday() to get start time */

    gettimeofday(&start_time, NULL);
    
    done = process_signal(fp,output_fp,delays,NUM_MIC,SAMPLING_RATE, 
                          beamform_results, info_str, 
                          queue, num_beams, window, weights);
    
    //    end_cycle_count = rdtsc();

    /* Now call gettimeofday() to get end time */
    gettimeofday(&end_time, NULL);  /* after time */
    
    /* Print the execution time */
    total_usecs = (end_time.tv_sec-start_time.tv_sec) * 1000000 +
        (end_time.tv_usec-start_time.tv_usec);
    
    printf("Total execution time was %f Sec.\n", ((float)total_usecs)/1000000);


    if (beamform_results != NULL) {
        for (i = 0; i<num_beams; i++) {
            energies[i] = calculate_energy(beamform_results[i], window);
        }
    }
    return done;
}





// Seach the far_field_angles. Return the maximum energy
float search_far_field_angles(float* max_result, char* data_file, char *output_file, int hamming) {
    FILE *fp, *output_fp;
    
    float *energies = (float*) malloc(NUM_ANGLES*sizeof(float));
    float max_angle;
    float angle;

    char str[MAX_LINE];
    char info_str[MAX_LINE];
    float **beamform_results;
    float max_energy = 0;
    int i;
    long int tmp;
    int done = 0;
    float time_index = 0;
    float time_index_inc = ((float)ANGLE_ENERGY_WINDOW_SIZE)/((float)SAMPLING_RATE);

    // Intialize the delays struct
    struct Delays *delays = init_delays(NUM_ANGLES, NUM_MIC);

    struct DataQueue *queue;// = (struct DataQueue *) malloc(sizeof(struct DataQueue));

    // Open the output file

    // Initialize the results array
    beamform_results = (float**) malloc(NUM_ANGLES*sizeof(float*));

    for (i=0; i<(NUM_ANGLES); i++) {
        beamform_results[i] = (float*) malloc(ANGLE_ENERGY_WINDOW_SIZE*sizeof(float));
    }


    i = 0;
    for (angle = (-M_PI/2); angle <= (M_PI/2); angle+= (M_PI/NUM_ANGLES)) {
        calc_far_field_delays(mic_locations, origin_location, angle, 
                              delays->delay_values[i], SOUND_SPEED, SAMPLING_RATE, NUM_MIC);
        positivize_delays(delays->delay_values[i], NUM_MIC);
        //        sprintf(delays->delay_info[i], "angle is %f\n", angle);

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
  
    //        printf("1angle is %f\n", angle);
    
    //        sprintf(info_str, "angle is %f\n", angle);
    //        printf("2angle is %f\n", angle);
    
    while (done == 0) {
        max_energy = 0;
        max_angle = 0;

        // Get beamforming results for all NUM_ANGLES directions
        done = calc_beamforming_result(fp, NULL /*output_fp*/, delays, beamform_results, 
                                       energies, info_str, queue, NUM_ANGLES, ANGLE_ENERGY_WINDOW_SIZE, hamming);

        // Now, figure out which angle has the maximum energy
        for (i = 0; i<NUM_ANGLES; i++) {
            //            printf("%f %f %f\n",time_index,(i-NUM_ANGLES/2)*M_PI/NUM_ANGLES,energies[i]);

            if (energies[i]>max_energy) {
                max_energy = energies[i];
                max_angle = (i-NUM_ANGLES/2)*M_PI/NUM_ANGLES;
                memcpy(max_result, beamform_results[i], ANGLE_ENERGY_WINDOW_SIZE);
            }
        }

        // At this point, just print out the result. Later, figure out
        // how to track it or whatever.
        printf("at time index %f, maximum energy was %f, occuring at angle %f (radians)\n",
               time_index, max_energy, max_angle);

        // It always does an extra one -- why? Need to figure this out
        
        time_index += time_index_inc;
    }
    
    fclose(fp);

    //    fclose(output_fp);

    return(max_energy);
}




void calc_single_pos(float source_location[3], float mic_locations[NUM_MIC][3], int hamming, char* data_file, char* output_file) {
    float mic_distances[NUM_MIC];
    //    float delays[NUM_MIC];
    char str[MAX_LINE];
    char info_str[MAX_LINE];
    float energy;
    FILE *fp;
    FILE *output_fp;
    float *beamform_result;
    // Intialize the delays struct for only one set of delays
    struct Delays *delays = init_delays(1, NUM_MIC);
    struct DataQueue *queue;// = (struct DataQueue *) malloc(sizeof(struct DataQueue));
    

    float **beamform_results;
    float *weights = NULL;
    int i;


    // Initialize the results array
    //    beamform_results = (float**) malloc(sizeof(float*));
    //
    //    for (i=0; i<(NUM_ANGLES); i++) {
    //        beamform_results[i] = (float*) malloc(*sizeof(float));
    //    }


    // Calculate distances from source to each of mics
    calc_distances(source_location, mic_locations, mic_distances, NUM_MIC);
    calc_delays(mic_distances, delays->delay_values[0], SOUND_SPEED, SAMPLING_RATE, NUM_MIC);
    

    
    //    printf ("Mic distances:\n");
    //    float_arr_to_str(mic_distances,NUM_MIC,str);
    //    printf ("%s",str);
    
    
    //    printf ("Mic delays(# samples):\n");
    //    float_arr_to_str(delays->delay_values[0],NUM_MIC,str);
    //    printf ("%s",str);
    
    
    adjust_delays(delays->delay_values[0], NUM_MIC);
    //    printf ("Adjusted Mic delays(# samples):\n");
    //    float_arr_to_str(delays->delay_values[0],NUM_MIC,str);
    //    printf ("%s",str);


    /*
    if (hamming) {
        if ( (NUM_MIC%2) == 1 ) {
            // Assume Odd numbers are only the left microphones
            weights = calc_weights_left_only(NUM_MIC);
        } else {
            weights = calc_weights_lr(NUM_MIC);
        }
        
        printf ("Mic Weights:\n");
        float_arr_to_str(weights,NUM_MIC,str);
        printf ("%s",str);
    }
    */
    delays->max_delay = find_max_in_arr (delays->delay_values[0], NUM_MIC);

    queue = init_data_queue(delays->max_delay, NUM_MIC);

    //    sprintf (info_str, "source position is (%f,%f,%f)\n", source_location[0], source_location[1], source_location[2]);
    
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
float search_grid(float initial_location[3], char* data_file, char* output_file, int hamming) {
    FILE *fp, *output_fp;
    
    //    float energy;
    float *energies = (float*) malloc(NUM_ANGLES*sizeof(float));
    float max_angle;
    float angle;
    char str[MAX_LINE];
    char info_str[MAX_LINE];
    float **beamform_results;
    float max_energy = 0;
    int i;
    long int tmp;
    int done = 0;
    float time_index = 0;
    float time_index_inc = ((float)GRID_ENERGY_WINDOW_SIZE)/((float)SAMPLING_RATE);
    float mic_distances[NUM_MIC];
    float **target_locations;
    float *max_result = (float*) malloc(ANGLE_ENERGY_WINDOW_SIZE*sizeof(float));
    float current_location[3];
    float *max_location;

    float x,y,z;

    // Intialize the delays struct
    struct Delays *delays = init_delays(NUM_ANGLES, NUM_MIC);

    struct DataQueue *queue;

    // Open the data file and process it
    if ((fp = fopen(data_file,"r")) == NULL) {
        printf("can't open %s\n",data_file);
        exit(1);
    } 
    

    // Initialize the results array
    beamform_results = (float**) malloc(NUM_DIRS*sizeof(float*));

    for (i=0; i<(NUM_ANGLES); i++) {
        beamform_results[i] = (float*) malloc(GRID_ENERGY_WINDOW_SIZE*sizeof(float));
    }

    // Initialize the locations array
    target_locations = (float**) malloc(NUM_DIRS*sizeof(float*));

    for (i=0; i<(NUM_DIRS); i++) {
        target_locations[i] = (float*) malloc(3*sizeof(float));
    }

    //    printf("Starting reference position is :(%f,%f,%f)\n",initial_location[0],initial_location[1],initial_location[2]);

    memcpy(current_location, initial_location, 3*sizeof(float));
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
            //            printf("%f %f %f\n",time_index,i);
            //            printf ("%s, Energy: %f\n", delays->delay_info[i],energies[i]);
            
            if (energies[i]>max_energy) {
                max_energy = energies[i];
                //                max_angle = (i-NUM_ANGLES/2)*M_PI/NUM_ANGLES;
                memcpy(max_result, beamform_results[i], ANGLE_ENERGY_WINDOW_SIZE);
                max_location = target_locations[i];
            }
        }


        // Adjust the current location to the location with the maximum energy

        //        printf("New reference position is :(%f,%f,%f)\n",max_location[0],max_location[1],max_location[2]);

        memcpy(current_location, max_location, 3*sizeof(float));
        

        //        printf("at time index %f, maximum energy was %f, occuring at angle %f\n",
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

    float *max_result;
    float max_energy;
    char search_far_field = 0;
    char hill_climb = 0;
    char hamming = 0;


    // Old location
    //    float source_location[] = {0.812800, 0.760362-(4*MIC_HORIZ_SPACE), 0.054849};
    //    float source_location[] = {0.79692, -1.118859, 0.058023};
           

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
                    //                } else if (strcmp(key, "-num_mic") == 0) {
                    //                    num_mic = (int) strtol(argv[++i],&pEnd,0);
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


    // take this out later
    //    initialize_microphone_locations(mic_locations);

    /*
      for(i = 0; i<NUM_MIC; i++) {
      printf("Position for microphone %d:(%f,%f,%f)\n",i,mic_locations[i][0],mic_locations[i][1],mic_locations[i][2]);
      }

      printf("Position for source: (%f,%f,%f)\n",source_location[0],source_location[1],source_location[2]);
    */


    if (search_far_field == 1) {
        max_result = (float*) malloc(ANGLE_ENERGY_WINDOW_SIZE*sizeof(float));


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


