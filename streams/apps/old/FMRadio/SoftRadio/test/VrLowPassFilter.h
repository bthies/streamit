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


#ifndef _VRLOWPASSFILTER_H_
#define _VRLOWPASSFILTER_H_
#include <cmath>
#include <VrDecimatingSigProc.h>

class VrLowPassFilter : public VrDecimatingSigProc<float, float> {
protected:
  int numberOfTaps;
  float* COEFF;
  float cutoffFreq, samplingRate, tapTotal;
  int mDecimation;

public :
  virtual void work(int n)
    {
      for (int j = 0; j < n; j++) { 
	float sum = 0;
	int i;
	float* inputArray = inputReadPtr(-numberOfTaps + 1);

	for (i=0; i < numberOfTaps; i++) {	  
	  sum += inputArray[i] * COEFF[i];
	}
	incInput(mDecimation+1);
	outputWrite(sum);
      }
      return;
    }

  VrLowPassFilter(float sampleRate, float cutFreq,
		  int numTaps, int decimation):
    VrDecimatingSigProc<float, float>(1, decimation)
    {
      float pi, m, w;
      float* temptaps;
      int i;
  
      samplingRate = sampleRate;
      cutoffFreq = cutFreq;
      numberOfTaps = numTaps;
  
      pi = M_PI;
      temptaps =  new float[numberOfTaps];
  
      m = numberOfTaps -1;
      mDecimation = decimation;

      COEFF = new float[numTaps];
  
      if(cutoffFreq == 0.0)
      {
//Using a Hamming window for Filter taps:
	tapTotal = 0.0;
    
	for(i=0;i<numberOfTaps;i++)
	{
	  temptaps[i] = (float)(0.54 - 0.46*cos((2.0*pi)*(((float)i)/((float) m))));
	  tapTotal += temptaps[i];
	}
	
	//normalize all the taps to a sum of 1
	for(i=0;i<numberOfTaps;i++)
	{
	  temptaps[i] = temptaps[i]/tapTotal;
	}
      }
      else{
	//ideal lowPass Filter ==> Hamming window
	//has IR h[n] = sin(omega*n)/(n*pi)
	//reference: Oppenheim and Schafer

	w = (2.0*pi) * cutoffFreq/samplingRate;
	for(i=0;i<numberOfTaps;i++)
	{
	  //check for div by zero
	  if(((float)i)-((float)m)/2.0 == 0) {
	    temptaps[i] = w/pi;
	  }
	  else {
	    temptaps[i] = (float)(sin(w*(i-m/2)) / pi
                                       / (i-m/2) * (0.54 - 0.46
                                                    * cos((2*pi)*(i/m))));
	  }


	}
      }
      COEFF = temptaps;
    }
  
  virtual void initialize() {
    setHistory(numberOfTaps);
  }
};
#endif
