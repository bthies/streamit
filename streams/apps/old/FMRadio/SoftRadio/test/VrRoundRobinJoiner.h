#ifndef _VRROUNDROBINJOINER_H_
#define _VRROUNDROBINJOINER_H_

#include <VrSigProc.h>

template<class itype, class otype>
class VrRoundRobinJoiner : public VrSigProc<itype, itype> {
protected:
  int numJoiners;
  VrSigProc<itype, otype>* joiner;
  VrSigProc<itype, itype>** streams;
  int sf, bps;

public:
  virtual void work(int i) {
    for (int n = 0; n < i; n++) {
      outputWrite(inputRead(0));
      incInput(1);
    }
  }
  virtual void initialize() {
   
  }
  
  VrRoundRobinJoiner(int num, VrSigProc<itype, otype>* join, VrSigProc<itype, itype>** connectFrom, 
		     int _sf, int _bps):
	numJoiners(num), joiner(joiner), streams(connectFrom), sf(_sf), bps(_bps) 
    {
      for (int i = 0; i < numJoiners; i++) 
	CONNECT(this, streams[i], sf, bps);
      CONNECT(joiner, this, sf, bps);
    }
  
};
#endif
