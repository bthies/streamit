#include "Globals.h"
#include "LPF.h"

void LPF_create(LPF_Data* this,
		vsip_cmview_f* inputMat,
		vsip_cmview_f* outputMat)
{
  vsip_cvview_f* fineFilterCoeffs = vsip_cvcreate_f(FINE_FILTER_SIZE, VSIP_MEM_NONE);
  vsip_cvview_f* coarseFilterCoeffs = vsip_cvcreate_f(COARSE_FILTER_SIZE, VSIP_MEM_NONE);

  this->time = 0;
  this->coarseTempRow = vsip_cvcreate_f(NUM_RANGES/COARSE_DECIMATION_RATIO, VSIP_MEM_NONE);
  this->inputRowView  = vsip_cmrowview_f(inputMat, 0);
  this->outputRowView = vsip_cmrowview_f(outputMat, 0);

  createLpf1(coarseFilterCoeffs);
  createLpf2(fineFilterCoeffs);

  this->coarseFir = vsip_cfir_create_f(coarseFilterCoeffs,
				       VSIP_NONSYM,
				       NUM_RANGES,
				       COARSE_DECIMATION_RATIO,
				       VSIP_STATE_NO_SAVE,
				       0,
				       0);

  this->fineFir = vsip_cfir_create_f(fineFilterCoeffs,
				     VSIP_NONSYM,
				     NUM_RANGES/COARSE_DECIMATION_RATIO,
				     FINE_DECIMATION_RATIO,
				     VSIP_STATE_NO_SAVE,
				     0,
				     0);

  vsip_cvalldestroy_f(fineFilterCoeffs);
  vsip_cvalldestroy_f(coarseFilterCoeffs);
}

void LPF_processPulse(LPF_Data* this,
		      vsip_cmview_f* inputMat,
		      vsip_cmview_f* outputMat)
{
  int i,j;

  clock_t start, stop;
  int localInputDataStride  = vsip_cmgetcolstride_f(inputMat);   /* Distance between input rows  */
  int localInputDataOffset  = vsip_cmgetoffset_f(inputMat);      /* Offset of first input row    */
  int localOutputDataStride = vsip_cmgetcolstride_f(outputMat);  /* Distance between output rows */
  int localOutputDataOffset = vsip_cmgetoffset_f(outputMat);     /* Offset of first output row   */
  int numLocalInputRows     = vsip_cmgetcollength_f(inputMat);   /* num rows in input matrix     */

  start = clock();
  for ( i = 0; i < numLocalInputRows; i++ )
  {
    /* move input and output row views into the proper position */
    vsip_cvputoffset_f(this->inputRowView, localInputDataOffset+i*localInputDataStride);
    vsip_cvputoffset_f(this->outputRowView, localOutputDataOffset+i*localOutputDataStride);

    /* Perform low pass filtering */
    vsip_cfirflt_f(this->coarseFir, this->inputRowView, this->coarseTempRow);
    vsip_cfirflt_f(this->fineFir, this->coarseTempRow, this->outputRowView);
  }

  stop = clock();
  this->time += ((double)(stop-start))/CLOCKS_PER_SEC;
}

void LPF_destroy(LPF_Data* this)
{
  vsip_cvalldestroy_f(this->coarseTempRow);
  vsip_cvdestroy_f(this->inputRowView);
  vsip_cvdestroy_f(this->outputRowView);
  vsip_cfir_destroy_f(this->fineFir);
  vsip_cfir_destroy_f(this->coarseFir);
}
