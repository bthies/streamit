#include "Globals.h"
#include "DIT.h"

void DIT_create(DIT_Data* this)
{
  float count;
  int i, j;
  vsip_cmview_f* bfWeightsTrans  = vsip_cmcreate_f(NUM_CHANNELS, NUM_BEAMS, VSIP_ROW, VSIP_MEM_NONE);
  vsip_cvview_f* pulseShape = vsip_cvcreate_f(PULSE_SIZE, VSIP_MEM_NONE);
  vsip_cvview_f* matchedFilter = vsip_cvcreate_f(MF_SIZE, VSIP_MEM_NONE);

  this->time = 0;
  this->steeringVectors = vsip_cmcreate_f(NUM_CHANNELS,
					  NUM_BEAMS,
					  VSIP_ROW,
					  VSIP_MEM_NONE);

  this->predecPulseShape = vsip_cvcreate_f(PREDEC_PULSE_SIZE, VSIP_MEM_NONE);

  count = 0.0;
  for(i = 0; i < vsip_cmgetcollength_f(this->steeringVectors); i++)
  {
    for(j = 0; j < vsip_cmgetrowlength_f(this->steeringVectors); j++)
    {
      vsip_cscalar_f z;
      z.r = ((float) count)/2.718281828;
      count++;
      z.i = ((float) count)/2.718281828;
      count++;
      vsip_cmput_f(this->steeringVectors,i,j,z);
//      printf("(%f, %f)\n", z.r, z.i);
    }
  }
  count = 0;
  for(i = 0; i < vsip_cvgetlength_f(this->predecPulseShape); i++)
  {
    vsip_cscalar_f z;
    z.r = ((float) count)/1.414213562;
    count++;
    z.i = ((float) count)/1.414213562;
    count++;
    vsip_cvput_f(this->predecPulseShape, i, z);
    //    printf("(%f, %f)\n", z.r, z.i);
  }

  createBf(this->steeringVectors, bfWeightsTrans);
  createMf(pulseShape, this->predecPulseShape, matchedFilter);

  vsip_cmalldestroy_f(bfWeightsTrans);
  vsip_cvalldestroy_f(pulseShape);
  vsip_cvalldestroy_f(matchedFilter);
}

void DIT_processPulse(DIT_Data* this, vsip_cmview_f* outputMat)
{
  int i, j;
  clock_t start, stop;
  start = clock();
  createRawData(this->steeringVectors, this->predecPulseShape, outputMat);
  stop = clock();
  this->time += ((double)(stop-start))/CLOCKS_PER_SEC;
}

void DIT_destroy(DIT_Data* this)
{
  vsip_cvalldestroy_f(this->predecPulseShape);
  vsip_cmalldestroy_f(this->steeringVectors);
}
