#ifndef _BF_H_
#define _BF_H_

#include <vsip.h>

typedef struct _BF_Data BF_Data;

struct _BF_Data
{
  /* The beamforming weights are stored in a transposed manner for efficiency. */
  vsip_cmview_f* bfWeights;
};

void BF_create(BF_Data* this);
void BF_processPulse(BF_Data* this, vsip_cmview_f* inputMat, vsip_cmview_f* outputMat);
void BF_destroy(BF_Data* this);

#endif
