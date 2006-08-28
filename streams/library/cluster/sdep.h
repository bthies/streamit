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

#ifndef __SDEP_H
#define __SDEP_H

class sdep {
  
  int numInitSrcExec;
  int numInitDstExec;
  
  int numSteadySrcExec;
  int numSteadyDstExec;
  
  int* dst2srcDependency;
  
public:

  sdep(int initSrc, int initDst, int steadySrc, int steadyDst);

  void setDst2SrcDependency(int dst, int src);

  int getSrcPhase4DstPhase(int nDstPhase);
  int getDstPhase4SrcPhase(int nSrcPhase);  
};

#endif
