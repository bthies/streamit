
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
