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


#ifndef _VRFLOATSUBTRACT_H_
#define _VRFLOATSUBTRACT_H_

#include <VrSigProc.h>

class VrFloatSubtract : public VrSigProc<float,float> {
protected:
  int inputs;
  int id;
public: 
  virtual void work(int n);
  VrFloatSubtract();
  virtual void initialize();
};

void
VrFloatSubtract::work(int n)
{
  for (int i=0; i < n; i++) {
    outputWrite(inputReadN(0, 0)-inputReadN(1, 0));
    incInputN(0, 1);
    incInputN(1, 1);
  }
  return;
}

void
VrFloatSubtract::initialize() {
}

VrFloatSubtract::VrFloatSubtract()
{
}


#endif
