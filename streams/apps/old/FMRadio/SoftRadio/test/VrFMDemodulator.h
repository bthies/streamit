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


#ifndef _VRFMDEMODULATOR_H_
#define _VRFMDEMODULATOR_H_
#include <cmath>
#include <VrDecimatingSigProc.h>

class VrFMDemodulator : public VrDecimatingSigProc<float, float> {
protected:
  float mGain;
  float sampleRate;
  float maxAmplitude;
  float modulationBandwidth;

public :
  virtual void work(int n)
    {
      for (int j = 0; j < n; j++) { 
	float temp = 0;
        //may have to switch to complex?
        temp = (float)((inputRead(0) * (inputRead(-1))));
        //if using complex, use atan2
        temp = (float)(mGain * atan(temp));

        incInput(1);
        outputWrite(temp);
	//printf("%d %f\n", n, temp);
      }
      return;
    }

  VrFMDemodulator(float sampRate, float max, float bandwidth):
    VrDecimatingSigProc<float, float>(1, 1)
    {
      sampleRate = sampRate;
      maxAmplitude = max;
      modulationBandwidth = bandwidth;
      
      mGain = maxAmplitude*(sampleRate
                            /(modulationBandwidth*(float)M_PI));
    }
  
  virtual void initialize() {
    setHistory(2);
  }
};
#endif
