#ifndef _LPF_H_
#define _LPF_H_

#include <vsip.h>

typedef struct _LPF_Data LPF_Data;
struct _LPF_Data
{
  vsip_cvview_f*    coarseTempRow;
  vsip_cvview_f*    inputRowView;
  vsip_cvview_f*    outputRowView;
  vsip_cfir_f*      coarseFir;
  vsip_cfir_f*      fineFir;
  double            time;
};

void LPF_create(LPF_Data* this,
		vsip_cmview_f* inputMat,
		vsip_cmview_f* outputMat);

void LPF_processPulse(LPF_Data* this,
		      vsip_cmview_f* inputMat,
		      vsip_cmview_f* outputMat);

void LPF_destroy(LPF_Data* this);

#endif
