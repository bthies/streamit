#ifndef _PC_H_
#define _PC_H_

#include <vsip.h>

typedef struct _PC_Data PC_Data;

struct _PC_Data
{
  unsigned long   numPulses;

  vsip_cvview_f*  matchedFilter;
  vsip_mview_f*   result;

  vsip_cmview_f*  concatData;
  vsip_cmview_f*  fstHalfData;
  vsip_cmview_f*  sndHalfData;

  vsip_cvview_f*  concatRow;
  vsip_vview_f*   rsltRow;
  vsip_cvview_f*  filteredRow;

#ifndef PC_IN_TIME
  vsip_fft_f*     fft;
  vsip_fft_f*     ifft;
#else
  vsip_cfir_f*    pcFir;
#endif
  double            time;
};

void PC_create(PC_Data* this);
void PC_processPulse(PC_Data* this, vsip_cmview_f* inputMat);
void PC_destroy(PC_Data* this);

#endif
