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
 * Class DataSource
 *
 * Implements the data source for the PCA demo app
 */

class DataSource extends Filter
{
  int numberOfSamples;
  int numberOfBeams;

  int targetBeam;
  int targetSample;

  float threshold;

  int currentSample;
  int currentBeam;

  public DataSource (int nBeams, int nSamples,
		     int tarBeam, int tarSample, float thresh)
  {
    super (nBeams, nSamples, tarBeam, tarSample, thresh);
  }

  public void init(int nBeams, int nSamples,
		   int tarBeam, int tarSample, thresh)
  {
    numberOfBeams      = nBeams;
    numberOfSamples    = nSamples;
    targetBeam         = tarBeam;
    targetSample       = tarSample;
    threshold          = thresh;

    currentBeam = 0;
    currentSample = 0;

    input = new Channel (Float.TYPE, 1);
  }

  public void work()
  {
    // No noise in input data -> no need for cfar filter -> detection is elementwise
    float inputData = input.popFloat();
    if(input >= threshold)
    {
      if(currentSample != targetSample && currentBeam != targetBeam)
      {
	// Bad things
      }
    }
    if(input < threshold)
    {
      if(currentSample == targetSample && currentBeam == targetBeam))
      {
	// More Bad things
      }
    }
    currentSample++;
    if (currentSample > numSamples)
    {
      currentSample = 0;
      currentBeam++;
    }
    if(currentBeam > numBeams)
    {
      currentBeam = 0;
    }
  }

}
