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


#ifndef _VRSIMPLEFILTER_H_
#define _VRSIMPLEFILTER_H_

#include <VrSigProc.h>

template<class iType,class oType> 
class VrSimpleFilter : public VrSigProc<iType,oType> {
public: 
  virtual void work(int n);
  VrSimpleFilter();
};

template<class iType,class oType> void
VrSimpleFilter<iType,oType>::work(int n)
{
  float c;
  
  for (int i=0; i < n; i++) {
    c = inputRead(i);
    printf("%f\n", c);
    outputWrite(c);
    incInput(1);
  }
  return;
}

template<class iType,class oType> 
VrSimpleFilter<iType,oType>::VrSimpleFilter()
{
}

#endif







