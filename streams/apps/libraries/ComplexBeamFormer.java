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
 * Class BeamFormer
 *
 * Implements a conventional beamformer.
 */

class ComplexBeamFormer extends Filter
{
  int numberOfBeams;
  int numberOfChannels;
  int numberOfSamples;
  float beamFormingWeights[];
  float inputData[];

  public ComplexBeamFormer (int nBeams, int nChannels, int nSamples)
  {
    super (nBeams, nChannels, nSamples);
  }

  public void init(int nBeams, int nChannels, int nSamples)
  {
    int i;

    numberOfBeams      = nBeams;
    numberOfChannels   = nChannels;
    numberOfSamples    = nSamples;
    beamFormingWeights = new float [numberOfBeams*numberOfChannels*2];
    inputData          = new float [numberOfChannels*numberOfSamples*2];

    input = new Channel (Float.TYPE, nChannels*nSamples*2);
    output = new Channel (Float.TYPE, nBeams*nSamples*2);

    for (i = 0; i < numberOfBeams*numberOfChannels*2; i++)
    {
      beamFormingWeights[i] = ((float) i)/ 2.449489743f;
      //System.out.println(beamFormingWeights[i]);
    }
  }

  public void work()
  {
    //System.out.println("Running the Beamformer...");

    int i, j, k;
    int v = 0;
    for (i = 0; i < numberOfChannels; i++)
    {
      for (j = 0; j < numberOfSamples*2; j++)
      {
	float x;
        x = inputData[v++] = input.popFloat();
//	System.out.println(x);
      }
    }

    for (i = 0;  i < numberOfBeams; i++)
    {
      for (j = 0; j < numberOfSamples*2; j += 2)
      {
        float outReal = 0;
	float outImag = 0;
        for (k = 0; k < numberOfChannels*2; k += 2)
        {
          outReal += (beamFormingWeights[i*numberOfChannels*2+k] *
		      inputData[k*numberOfSamples+j] -
		      beamFormingWeights[i*numberOfChannels*2+k+1] *
		      inputData[k*numberOfSamples+j+1]);
	  outImag += (beamFormingWeights[i*numberOfChannels*2+k] *
		      inputData[k*numberOfSamples+j+1] +
		      beamFormingWeights[i*numberOfChannels*2+k+1] *
		      inputData[k*numberOfSamples+j]);
        }
        output.pushFloat(outReal);
	output.pushFloat(outImag);
      }
    }
  }
}
