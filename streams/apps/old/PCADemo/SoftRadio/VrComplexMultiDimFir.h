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


#ifndef _VRCOMPLEXMULTIDIMFIR_H_
#define _VRCOMPLEXMULTIDIMFIR_H_
#include <cmath>
#include <VrDecimatingSigProc.h>

class VrComplexMultiDimFir : public VrDecimatingSigProc<float, float> 
{
protected:
  
  int numberOfTaps;
  int vectorLength;
  int currentScratchIndex;
  int currentVectorIndex;
  int decimationRatio;

  float* scratchSpace;
  float* COEFF;

public:
  
  VrComplexMultiDimFir(int numTaps, int vectorLen, int decRatio):
    VrDecimatingSigProc<float, float>(1, 2*decRatio),
    numberOfTaps(numTaps), vectorLength(vectorLen),
    currentScratchIndex(0), currentVectorIndex(0), 
    decimationRatio (decRatio) 
    {
      int i;
      
      scratchSpace = new float[numberOfTaps*2];
      COEFF = new float[numberOfTaps*2];
      
      for (i=0; i<numberOfTaps*2; i++)
      {
	COEFF[i] = 0;
      }
      COEFF[0] = 1;
    }
  
  virtual void work(int n) 
    {
      for (int j = 0; j < n; j++) { 
	int i, j;
	float sumReal = 0;
	float sumImag = 0;

	scratchSpace[currentScratchIndex] = inputRead();
	scratchSpace[currentScratchIndex+1] = inputRead(-1);
	incInput(2*decimationRatio);

	for (i=0; i<numberOfTaps*2; i += 2)
	{
	  sumReal += COEFF[i]*scratchSpace[i] - COEFF[i+1]*scratchSpace[i+1];
	  sumImag += COEFF[i+1]*scratchSpace[i] + COEFF[i]*scratchSpace[i+1];
	}
	outputWrite(sumReal);
	outputWrite(sumImag);
	
	currentScratchIndex += 2;
	if( currentScratchIndex >= numberOfTaps*2)
	{
	  currentScratchIndex = 0;
	}
	
	currentVectorIndex++;
	if( currentVectorIndex >= vectorLength )
	{
	  currentVectorIndex = currentScratchIndex = 0;
	  for(j = 0; j < numberOfTaps*2; j++)
	    scratchSpace[j] = 0;
	}
      }
    }
  
  virtual void initialize() 
    {
      setHistory(2);
    }
};


#endif
