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

class BeamFormer extends Filter
{
  int numberOfBeams;
  int numberOfChannels;
  int numberOfSamples;
  float beamFormingWeights[];
  float inputData[];

  public BeamFormer (int nBeams, int nChannels, int nSamples)
  {
    super (nBeams, nChannels, nSamples);
  }

  public void init(int nBeams, int nChannels, int nSamples)
  {
    numberOfBeams      = nBeams;
    numberOfChannels   = nChannels;
    numberOfSamples    = nSamples;
    beamFormingWeights = new float [numberOfBeams*numberOfChannels];
    inputData          = new float [numberOfChannels*numberOfSamples];

    input = new Channel (Float.TYPE, nChannels*nSamples);
    output = new Channel (Float.TYPE, nBeams*nSamples);

    // NEED TO GENERATE BF WEIGHTS HERE
  }

  public void work()
  {
    System.out.println("Running the Beamformer...");

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
      for (j = 0; j < numberOfSamples; j++)
      {
        float out = 0;
        for (k = 0; k < numberOfChannels; k++)
        {
          out += (beamFormingWeights[i*numberOfChannels+k]) *
	    (inputData[k*numberOfSamples+j]);
        }
        output.pushFloat(out);
      }
    }
  }
}
