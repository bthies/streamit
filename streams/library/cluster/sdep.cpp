
#include "sdep.h"

#include <stdlib.h>

  sdep::sdep(int initSrc, int initDst, int steadySrc, int steadyDst) {
    numInitSrcExec = initSrc;
    numInitDstExec = initDst;
    numSteadySrcExec = steadySrc;
    numSteadyDstExec = steadyDst;
    dst2srcDependency = (int*)malloc(sizeof(int)*(initDst+steadyDst+1));
  }

  void sdep::setDst2SrcDependecny(int dst, int src) {
    dst2srcDependency[dst] = src;
  }


  int sdep::getSrcPhase4DstPhase(int nDstPhase) {
    if (nDstPhase < numInitDstExec + 1)
      {
	return dst2srcDependency[nDstPhase];
      }
    else
      {
	int nSteadyStates =
	  (nDstPhase - (numInitDstExec + 1)) / numSteadyDstExec;
	int nSmallerDstPhase =
	  ((nDstPhase - (numInitDstExec + 1)) % numSteadyDstExec)
	  + numInitDstExec
	  + 1;
	return dst2srcDependency[nSmallerDstPhase]
	  + nSteadyStates * numSteadySrcExec;
      }
  }

  int sdep::getDstPhase4SrcPhase(int nSrcPhase) {
    // first have to figure out if I need to "wrap around"
    int addDstPhase = 0;
    if (nSrcPhase >= numInitSrcExec + numSteadySrcExec + 1)
      {
	int fullExecs =
	  (nSrcPhase - numInitSrcExec - 1) / numSteadySrcExec;
	addDstPhase = fullExecs * numSteadyDstExec;
	nSrcPhase = nSrcPhase - fullExecs * numSteadySrcExec;
      }
    
    int dstPhaseLow = 0,
      dstPhaseHigh = numInitDstExec + numSteadyDstExec;
    
    // make the binary search biased towards finding the low answer
    while (dstPhaseHigh - dstPhaseLow > 1)
      {
	int dstPhaseMid = (dstPhaseLow + dstPhaseHigh) / 2;
	if (dst2srcDependency[dstPhaseMid] >= nSrcPhase)
	  {
	    dstPhaseHigh = dstPhaseMid;
	  }
	else
	  {
	    dstPhaseLow = dstPhaseMid;
	  }
      }
    
    // now comes the slight contradiction:
    // If there is an entry of nSrcPhase, I want to get the lowest index
    // that contains it. But if such an entry doesn't exist, I want the
    // high-end entry! 
    int dstPhase;
    if (dst2srcDependency[dstPhaseLow] == nSrcPhase)
      {
	dstPhase = dstPhaseLow;
      }
    else
      {
	dstPhase = dstPhaseHigh;
      }
    
    return dstPhase + addDstPhase;
  }

 
