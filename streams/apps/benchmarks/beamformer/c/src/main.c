#include <vsip.h>
#include "Globals.h"
#include "DIT.h"
#include "LPF.h"
#include "BF.h"
#include "PC.h"
#include "Utils.h"
#include "time.h"

#define NUM_PULSES 10000

static double howManyMflopsDIT()
{
  double outerProdOps =
    NUM_CHANNELS * 8 * PREDEC_PULSE_SIZE;

  return outerProdOps/1e6;
}

static double howManyMflopsLPF()
{
  double coarseFilterOps =
    NUM_CHANNELS * 8 * COARSE_FILTER_SIZE * NUM_POSTDEC1;

  double fineFilterOps  =
    NUM_CHANNELS * 8 * FINE_FILTER_SIZE * NUM_POSTDEC2;

  return (coarseFilterOps + fineFilterOps)/1e6;
}

static double howManyMflopsBF()
{
  double beamformOps =
    NUM_CHANNELS * 8 * NUM_BEAMS * NUM_POSTDEC2;

  return (beamformOps)/1e6;
}

static double howManyMflopsPC()
{
  double filterOps =
    NUM_BEAMS * 8 * MF_SIZE * MF_SIZE;

  double magOps =
    NUM_BEAMS * 20 * MF_SIZE;

  double detOps =
    2.0*(NUM_BEAMS * MF_SIZE);

  return (filterOps + magOps + detOps)/1e6;
}


int main( int argc, char* argv[] )
{
  int i;
  clock_t        start;
  clock_t        stop;
  double         totalTime;
  double         timePerPulse;
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

  start = clock();
  for( i = 0; i<NUM_PULSES; i++ )
  {
    // DIT_processPulse(&dit, ditLpfData);
    LPF_processPulse(&lpf, ditLpfData, lpfBfData);
    BF_processPulse(&bf, lpfBfData, bfPcData);
    PC_processPulse(&pc, bfPcData);
  }
  stop = clock();

  totalTime = ((double)(stop - start))/CLOCKS_PER_SEC;
  timePerPulse = totalTime/NUM_PULSES;
  printf("The application took %f seconds to process %d pulses\n", totalTime, NUM_PULSES);
  printf("    %f pulses/sec for the whole app\n", NUM_PULSES/totalTime);
  printf("    %f Mflops/sec for the whole app\n", (/*howManyMflopsDIT()+*/
	 howManyMflopsLPF() + howManyMflopsBF() + howManyMflopsPC())/timePerPulse);
  printf("    DIT: %f secs/pulse %f pulses/sec, for a flop rate of %f Mflops/s\n",
	 dit.time/NUM_PULSES, NUM_PULSES/dit.time, howManyMflopsDIT()/(dit.time/NUM_PULSES));
  printf("    LPF: %f secs/pulse, %f pulses/sec, for a flop rate of %f Mflops/s\n",
	 lpf.time/NUM_PULSES, NUM_PULSES/lpf.time, howManyMflopsLPF()/(lpf.time/NUM_PULSES));
  printf("    BF:  %f secs/pulse, %f pulses/sec, for a flop rate of %f Mflops/s\n",
	 bf.time/NUM_PULSES, NUM_PULSES/bf.time, howManyMflopsBF()/(bf.time/NUM_PULSES));
  printf("    PC:  %f secs/pulse, %f pulses/sec, for a flop rate of %f Mflops/s\n",
	 pc.time/NUM_PULSES, NUM_PULSES/pc.time, howManyMflopsPC()/(pc.time/NUM_PULSES));

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
