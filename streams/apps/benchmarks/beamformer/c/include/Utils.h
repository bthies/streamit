#ifndef _UTILS_H_
#define _UTILS_H_

#include <vsip.h>

void checkIfAllZeroCf( vsip_cmview_f* a, char* file, char* line );

void createLpf1(vsip_cvview_f* coarseFilterWeights);

void createLpf2(vsip_cvview_f* fineFilterWeights);

void createBf(vsip_cmview_f* steeringVectors,
	      vsip_cmview_f* bfWeights);

void createMf(vsip_cvview_f* pulseShape,
	      vsip_cvview_f* predecPulseShape,
	      vsip_cvview_f* mfWeights);

void createCfar(vsip_cvview_f* cfarWeights);

void createRawData(const vsip_cmview_f* steeringVectors,
                   const vsip_cvview_f* predecPulseShape,
                   vsip_cmview_f*       raw_data);


#endif
