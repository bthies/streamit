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


#ifndef _VRSUBTRACTOR_H_
#define _VRSUBTRACTOR_H_

#include <VrSigProc.h>
 
class VrSubtractor : public VrSigProc<float,float> {
protected:
  int rate;
public: 
  virtual void work(int n);
};

void
VrSubtractor::work(int n)
{
  outputWrite(inputRead(1)-inputRead(1));
  return;
}


#endif







