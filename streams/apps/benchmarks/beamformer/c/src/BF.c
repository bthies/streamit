#include "Globals.h"
#include "BF.h"
#include <time.h>

void BF_create(BF_Data* this)
{

  float count;
  int i, j;
  vsip_cmview_f* steeringVectors = vsip_cmcreate_f(NUM_CHANNELS, NUM_BEAMS, VSIP_ROW, VSIP_MEM_NONE);

  /* This temporary holds the beam-forming weights in their "standard" config - that is, the
   * dimensions are the same as the steering vector matrix. However, for efficiency, we are going
   * to store the beamforming weights locally as a transpostition of the standard config.
   * This way, hot-loop processing just involves a matrix multiplication and not an additional transpose.
   */
  vsip_cmview_f* bfWeightsTrans  = vsip_cmcreate_f(NUM_CHANNELS, NUM_BEAMS, VSIP_ROW, VSIP_MEM_NONE);

  this->time = 0;
  createBf(steeringVectors, bfWeightsTrans);

  /* Store the transpose of the weights. */
  this->bfWeights = vsip_cmtransview_f(bfWeightsTrans);

  for(i = 0; i < vsip_cmgetcollength_f(this->bfWeights); i++)
  {
    for(j = 0; j < vsip_cmgetrowlength_f(this->bfWeights); j++)
    {
      vsip_cscalar_f z;
      z.r = ((float) count)/2.449489743;
      count++;
      z.i = ((float) count)/2.449489743;
      count++;
      vsip_cmput_f(this->bfWeights,i,j,z);
    }
  }
  vsip_cmalldestroy_f(steeringVectors);
  vsip_cmdestroy_f(bfWeightsTrans);
}

void BF_processPulse(BF_Data* this, vsip_cmview_f* inputMat, vsip_cmview_f* outputMat)
{
  int i, j;
  clock_t start, stop;
  start = clock();
  vsip_cmprod_f(this->bfWeights, inputMat, outputMat);
  stop = clock();
  this->time += ((double)(stop-start))/CLOCKS_PER_SEC;
}

void BF_destroy(BF_Data* this)
{
  vsip_cmalldestroy_f(this->bfWeights);
}
