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
 * Class TestDetector
 *
 * Implements a (VERY) simple detection algorithm
 */

class TestDetector extends Filter
{
  int numberOfSamples;
  int numberOfBeams;

  int targetBeam;
  int targetSample;

  float threshold;

  int currentSample;
  int currentBeam;

  public TestDetector (int nBeams, int nSamples,
		     int tarBeam, int tarSample, float thresh)
  {
    super (nBeams, nSamples, tarBeam, tarSample, thresh);
  }

  public void init(int nBeams, int nSamples,
		   int tarBeam, int tarSample, float thresh)
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
    //System.out.println("Detecting...");
    // No noise in input data -> no need for cfar filter -> detection is elementwise
    float inputData = input.popFloat();
    if (inputData == 0)
      System.out.println("Got zero - good stuff.");
    if (inputData != 0)
      System.err.println("Did not get zero - very bad stuff.");

//     if(inputData >= threshold)
//     {
//       if(currentSample != targetSample && currentBeam != targetBeam)
//       {
// 	// Bad things
//       }
//     }
//     if(inputData < threshold)
//     {
//       if(currentSample == targetSample && currentBeam == targetBeam)
//       {
// 	// More Bad things
//       }
//       else if (inputData != 1)
//       {
// 	System.err.println("Getting screwy data out of the pipe.");
//       }
//     }
    currentSample++;
    if (currentSample > numberOfSamples)
    {
      currentSample = 0;
      currentBeam++;
    }
    if(currentBeam > numberOfBeams)
    {
      currentBeam = 0;
    }
  }

}
