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


#ifndef _VRCLOATTOCHAR_H_
#define _VRFLOATTOCHAR_H_

#include <VrSigProc.h>
 
class VrFloatToChar : public VrSigProc<float, char> {
protected:
  int rate;
public: 
  virtual void work(int n);
  virtual void initialize();
};

/*
void
VrFloatToChar::initialize() {
  setHistory(1);
}
*/

void
VrFloatToChar::work(int n)
{
  float *c;
  *c = inputRead(1);
  int* d = reinterpret_cast<int*>(c);
  int out1 = (*d&0xff000000)>>24;
  int out2 = (*d&0x00ff0000)>>16;
  int out3 = (*d&0x0000ff00)>>8;
  int out4 = (*d&0x000000ff);
  outputWrite(out1);
  outputWrite(out2);
  outputWrite(out3);
  outputWrite(out4);

  return;
}


#endif







