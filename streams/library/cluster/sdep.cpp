/*
 * Copyright 2006 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

#include "sdep.h"

#include <stdlib.h>
#include <stdio.h>

  sdep::sdep(int initSrc, int initDst, int steadySrc, int steadyDst) {
    numInitSrcExec = initSrc;
    numInitDstExec = initDst;
    numSteadySrcExec = steadySrc;
    numSteadyDstExec = steadyDst;
    dst2srcDependency = (int*)malloc(sizeof(int)*(initDst+steadyDst+1));
  }

  void sdep::setDst2SrcDependency(int dst, int src) {
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

    if (nSrcPhase <= 0) {
      printf("WARNING: negative downstream message sent too early!\n");
      return 1;
    }

    //fprintf(stderr,"nSrcPhase=%d\n", nSrcPhase);
    // first have to figure out if I need to "wrap around"
    int addDstPhase = 0;
    if (nSrcPhase >= numInitSrcExec + numSteadySrcExec + 1)
      {
	int fullExecs =
	  (nSrcPhase - numInitSrcExec - 1) / numSteadySrcExec;
	addDstPhase = fullExecs * numSteadyDstExec;
	nSrcPhase = nSrcPhase - fullExecs * numSteadySrcExec;
      }
    //fprintf(stderr,"addDstPhase=%d nSrcPhase=%d\n", addDstPhase, nSrcPhase);
    
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
    //fprintf(stderr,"dstPhaseLow=%d dst2src[]=%d\n", dstPhaseLow, dst2srcDependency[dstPhaseLow]);
    //fprintf(stderr,"dstPhaseHigh=%d dst2src[]=%d\n", dstPhaseHigh, dst2srcDependency[dstPhaseHigh]);
    int dstPhase;
    if (dst2srcDependency[dstPhaseLow] == nSrcPhase)
      {
	dstPhase = dstPhaseLow;
      }
    else
      {
	dstPhase = dstPhaseHigh;
      }
    //fprintf(stderr,"dstPhase=%d\n", dstPhase);
    
    return dstPhase + addDstPhase;
  }

 
