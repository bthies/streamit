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


#ifndef _VRQUADRATUREDEMOD_H_
#define _VRQUADRATUREDEMOD_H_

#include <VrSigProc.h>
#include <math.h>

template<class oType> 
class VrQuadratureDemod : public VrSigProc<complex,oType> {
protected:
  float gain;
public: 
  void setGain(float g){ gain = g; return;}
  virtual void work(int n);
  virtual void initialize();
  VrQuadratureDemod(oType g);
  VrQuadratureDemod();
};

template<class oType> void
VrQuadratureDemod<oType>::work(int n)
{
  printf("QuadDemod %d\n", n);
  
  complex product,val;
  printf("QuadDemod requesting -1\n");
  complex lastVal = inputRead(-1);

  for (int i=0; i < n; i++,
	 printf("QuadDemod incrementing 1\n"),
	 incInput(1)) {
    printf("QuadDemod requesting 0\n");
    val = inputRead(0);
    product = val * conj(lastVal);
    lastVal = val;
    printf("QuadDemod writing 1\n");
    outputWrite((oType)(gain * arg(product)));
  }
  return;
}

template<class oType> void
VrQuadratureDemod<oType>::initialize()
{
  setHistory (2);
}

template<class oType> 
VrQuadratureDemod<oType>::VrQuadratureDemod(oType g)
  :gain(g)
{
}

template<class oType> 
VrQuadratureDemod<oType>::VrQuadratureDemod()
  :gain(1)
{
}

#endif









