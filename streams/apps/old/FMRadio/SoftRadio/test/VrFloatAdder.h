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


#ifndef _VRFLOATADDER_H_
#define _VRFLOATADDER_H_

#include <VrSigProc.h>

class VrFloatAdder : public VrSigProc<float,float> {
protected:
  int id;
public: 
  virtual void work(int n);
  VrFloatAdder();
  virtual void initialize();
};

void
VrFloatAdder::work(int n)
{
  for (int i=0; i < n; i++) {
    outputWrite(inputReadN(0, 0)+inputReadN(1, 0) +
		inputReadN(2, 0) + inputReadN(3, 0));
    incInputN(0, 1);
    incInputN(1, 1);
    incInputN(2, 1);
    incInputN(3, 1);
  }
  return;
}

void
VrFloatAdder::initialize() {
}

VrFloatAdder::VrFloatAdder()
{
}


#endif
