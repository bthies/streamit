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

import streamit.*;

/**
 * Class BeamFormer
 *
 * Implements a conventional beamformer.
 */

class BeamFormer extends Filter
{
  int numberOfBeams;
  int numberOfChannels;
  int numberOfSamples;
  float BeamFormingWeights[];
  float inputData[];

  public BeamFormer (int nBeams, int nChannels, int nSamples)
  {
    super ();
    numberOfBeams      = nBeams;
    numberOfChannels   = nChannels;
    numberOfSamples    = nSamples;
    BeamFormingWeights = new float(numberOfBeams*numberOfChannels);
    inputData          = new float(numberOfChannels*numberOfSamples);
  }

  Channel input = new Channel (Float.TYPE, numberOfChannels*numberOfSamples);
  Channel output = new Channel (Float.TYPE, numberOfBeams*numberOfSamples);

  public void initIO()
  {
    streamInput  = input;
    streamOutput = output;
  }

  public void init()
  {
    // NEED TO GENERATE BF WEIGHTS HERE
  }

  public void init(float weights[])
  {
    BeamFormingWeights = weights;
  }

  public void work()
  {
    int i, j, k;
    int v = 0;
    for (i = 0; i < numberOfChannels; i++)
    {
      for (j = 0; j < numberOfSamples; j++)
      {
	inputData[v++] = input.popFloat();
      }
    }

    for (i = 0;  i < numberOfBeams; i++)
    {
      for (j = 0; i < numberOfSamples; i++)
      {
	float out = 0;
	for (k = 0; k < numChannels; i++)
	{
	  out += inputData[i*numberOfChannels+k]*matrixB[k*numberOfSamples+j];
	}
	output.pushFloat(out);
      }
    }
  }
}
