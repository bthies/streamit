#ifndef _VRDUPLICATESPLITTER_H_
#define _VRDUPLICATESPLITTER_H_

#include <VrSigProc.h>

template<class itype, class otype>
class VrDuplicateSplitter : public VrSigProc<otype, otype> {
protected:
  int numSplitters;
  VrSigProc<itype, otype>* splitter;
  VrSigProc<otype, otype>** streams;
  int sf, bps;

public:
  virtual void work(int i) {
    for (int n = 0; n < i; n++) {
      otype in = inputRead(0);
      for (int s = 0; s < numSplitters; s++)
	outputWrite(in);
      incInput(1);
    }      
  }
  virtual void initialize() {

  }
  
  VrDuplicateSplitter(int num, VrSigProc<itype, otype>* split, VrSigProc<otype, otype>** connectTo, 
		      int _sf, int _bps):VrSigProc<otype, otype>(num), 
	numSplitters(num), splitter(split), streams(connectTo), sf(_sf), bps(_bps) 
    {
      CONNECT(this, splitter, sf, bps);
      for (int i = 0; i < numSplitters; i++) {
	CONNECTN(streams[i], this, i, sf, bps);
      }
    }
  
};
#endif
