#include <raw.h>
#include <stdio.h>

#include <vsip.h>
#include "Globals.h"
#include "DIT.h"
#include "LPF.h"
#include "BF.h"
#include "PC.h"
#include "Utils.h"


#define NUM_PULSES 10000

#define DEBUG_0

int main()
{
  int i;
  vsip_cmview_f* ditLpfData;
  vsip_cmview_f* lpfBfData;
  vsip_cmview_f* bfPcData;
  DIT_Data       dit;
  LPF_Data	 lpf;
  BF_Data	 bf;
  PC_Data        pc;

  vsip_init(NULL);

  ditLpfData = vsip_cmcreate_f(NUM_CHANNELS,
			       NUM_RANGES,
			       VSIP_ROW,
			       VSIP_MEM_NONE);
  lpfBfData = vsip_cmcreate_f(NUM_CHANNELS,
			      NUM_POSTDEC2,
			      VSIP_ROW,
			      VSIP_MEM_NONE);
  bfPcData = vsip_cmcreate_f(NUM_BEAMS,
			     NUM_POSTDEC2,
			     VSIP_ROW,
			     VSIP_MEM_NONE);

#ifdef DEBUG_0
  printf("VSIP is intialized... \n");
#endif

  DIT_create(&dit);

#ifdef DEBUG_0
  printf("  DIT is intialized... \n");
#endif

  LPF_create(&lpf, ditLpfData, lpfBfData);

#ifdef DEBUG_0
  printf("  LPF is intialized... \n");
#endif

  BF_create(&bf);

#ifdef DEBUG_0
  printf("  BF is intialized... \n");
#endif

  PC_create(&pc);

#ifdef DEBUG_0
  printf("All Stages Intialized...\n");
#endif



  for( i = 0; i<NUM_PULSES; i++ )
  {
    /* DIT_processPulse(&dit, ditLpfData); */
    LPF_processPulse(&lpf, ditLpfData, lpfBfData);
    BF_processPulse(&bf, lpfBfData, bfPcData);
    PC_processPulse(&pc, bfPcData);
  }

#ifdef DEBUG_0
  printf("Finished Processing...\n");
#endif

  vsip_cmalldestroy_f(ditLpfData);
  vsip_cmalldestroy_f(lpfBfData);
  vsip_cmalldestroy_f(bfPcData);

  DIT_destroy(&dit);
  LPF_destroy(&lpf);
  BF_destroy(&bf);
  PC_destroy(&pc);

  vsip_finalize(NULL);

#ifdef DEBUG_0
  printf("Destroyed Everything, exiting...\n");
#endif

  return 0;
}
