#ifndef _DIT_H_
#define _DIT_H_

#include <vsip.h>

typedef struct _DIT_Data DIT_Data;

struct _DIT_Data
{
  vsip_cmview_f*	steeringVectors;
  vsip_cvview_f*        predecPulseShape;
  double            time;
};

void DIT_create(DIT_Data* this);
void DIT_processPulse(DIT_Data* this, vsip_cmview_f* outputMat);
void DIT_destroy(DIT_Data* this);





#endif
