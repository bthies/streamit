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

#ifndef _VRFLOATSINK_H_
#define _VRFLOATSINK_H_

#include <VrSink.h>

class VrFloatSink : public VrSink<float> {
protected:
  int it;
  int id;
public :
  virtual void work(int i) {

    for (int n = 0; n < i; n++) {
      float t;
      t = inputRead(0);
      incInput(1);
      it++;
      if (it > 100000)
	exit(0);
      //printf("%d %f\n", id, t);
    }
  }
  VrFloatSink(int i):id(i) {
    it = 0;
  }
  VrFloatSink():id(0) {
    it = 0;
  }
};
#endif

