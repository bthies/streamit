/* -*- Mode: c++ -*-
 *
 *  Copyright 1997 Massachusetts Institute of Technology
 * 
 *  Permission to use, copy, modify, distribute, and sell this software and its
 *  documentation for any purpose is hereby granted without fee, provided that
 *  the above copyright notice appear in all copies and that both that
 *  copyright notice and this permission notice appear in supporting
 *  documentation, and that the name of M.I.T. not be used in advertising or
 *  publicity pertaining to distribution of the software without specific,
 *  written prior permission.  M.I.T. makes no representations about the
 *  suitability of this software for any purpose.  It is provided "as is"
 *  without express or implied warranty.
 * 
 */

#ifndef _VRNULLSINK_H_
#define _VRNULLSINK_H_


#include <VrSink.h>


template<class iType> 
class VrNullSink : public VrSink<iType> {
private:
  int myhistory, it;
public:
  static int id;
  int myID;
  virtual void work(int n);
  virtual void initialize();
  VrNullSink(): myhistory(1) {}
  VrNullSink(int s): myhistory(s) {};
};

template<class iType> void 
VrNullSink<iType>::work(int n)
{
  //  long long init = CYCLE_COUNT();
  // printf("NullSink %d\n", n);

  while(n>0) {
    it ++;
    if (myID == 0 && it > 1000)
      exit(0);
    //printf("%d %d\n", myID, inputRead(0));  
    // printf("NullSink requesting 1\n");
    inputRead(0);

// need to read one sample to force the connector
                 // to request more data
    incInput(myhistory);
    n-=myhistory;
  }
  //  long long end = CYCLE_COUNT();
  //cout << "to nullsink = " << (end - init - 39)/(float)n << 
  //  " cycles per sample over " << n << "samples" << endl;

}

template<class itype> int VrNullSink<itype>::id;

template<class iType> void
VrNullSink<iType>::initialize() {
  setHistory(myhistory);  
  setOutputSize(myhistory);  
  it = 0;
  myID = id++;
}

#endif
