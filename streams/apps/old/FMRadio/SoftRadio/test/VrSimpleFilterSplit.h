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


#ifndef _VRSIMPLEFILTERSPLIT_H_
#define _VRSIMPLEFILTERSPLIT_H_

#include <VrSigProc.h>

template<class iType,class oType> 
class VrSimpleFilterSplit : public VrSigProc<iType,oType> {
protected:
  int id, outputs;
public: 
  virtual void work(int n);
  VrSimpleFilterSplit(int o, int i);
};

template<class iType,class oType> void
VrSimpleFilterSplit<iType,oType>::work(int n)
{
  float c;
  
  for (int i=0; i < n; i++) {
    c = inputRead(0);
    printf("%d %f\n", id, c);
    for (int o = 0; o < outputs; o++)
      outputWriteN(o, c);
    incInput(1);
  }
  return;
}

template<class iType,class oType> 
VrSimpleFilterSplit<iType,oType>::VrSimpleFilterSplit(int o, int i):
    VrSigProc<iType, oType>(o), id(i), outputs(o)
{
}

#endif



