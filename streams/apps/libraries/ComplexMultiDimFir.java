/*
 *  Copyright 2001 Massachusetts Institute of Technology
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
 */

import streamit.library.*;

/**
 * Class FirFilter
 *
 * Implements an FIR Filter
 */

public class ComplexMultiDimFir extends Filter
{
  int numberOfTaps;
  int vectorLength;
  int currentScratchIndex;
  int currentVectorIndex;
  int decimationRatio;

  float scratchSpace[];
  float COEFF[];

  public ComplexMultiDimFir(int numTaps, int vectorLength, int decimationRatio)
  {
    super (numTaps, vectorLength, decimationRatio);
  }

  public void init(int numTaps, int vectorLen, int decRatio)
  {
    int i;

    currentScratchIndex = 0;
    currentVectorIndex = 0;
    vectorLength = vectorLen;
    numberOfTaps = numTaps;
    decimationRatio = decRatio;

    scratchSpace = new float[numberOfTaps*2];
    COEFF = new float[numberOfTaps*2];

    for (i=0; i<numberOfTaps*2; i++)
    {
      COEFF[i] = 0;
    }
    COEFF[0] = 1;

    input = new Channel (Float.TYPE, 2*decRatio);
    output = new Channel (Float.TYPE, 2);
  }

  public void work()
  {
    int i, j;
    float sumReal = 0;
    float sumImag = 0;

    scratchSpace[currentScratchIndex] = input.popFloat();
    scratchSpace[currentScratchIndex+1] = input.popFloat();

    for (i = 0; i < decimationRatio - 1; i++)
    {
      input.popFloat();
      input.popFloat();
    }

    for (i=0; i<numberOfTaps*2; i += 2)
    {
      sumReal += COEFF[i]*scratchSpace[i] - COEFF[i+1]*scratchSpace[i+1];
      sumImag += COEFF[i+1]*scratchSpace[i] + COEFF[i]*scratchSpace[i+1];
    }
    output.pushFloat(sumReal);
    output.pushFloat(sumImag);

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
