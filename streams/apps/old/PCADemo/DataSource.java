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
  int numberOfChannels;
  int numberOfSamples;
  int numberOfBeams;
  int predecPulseSize;

  int targetBeam;
  int targetSample;

  float steeringVectors[];
  float predecPulseShape[];

  public DataSource (int nChannels, int nSamples, int nBeams,
		     int tarBeam, int tarSample, int predecPulseSize)
  {
    super (nBeams, nChannels, nSamples, tarBeam, tarSample, predecPulseSize);
  }

  public void init(int nChannels, int nSamples, int nBeams,
		   int tarBeam, int tarSample, int predecPulseSize)
  {
    numberOfBeams      = nBeams;
    numberOfChannels   = nChannels;
    numberOfSamples    = nSamples;
    targetBeam         = tarBeam;
    targetSample       = tarSample;
    predecPulseSize    = predecPulseSize;
    steeringVectors    = new float [numberOfBeams*numberOfChannels];
    predecPulseShape   = new float [predecPulseSize];

    output = new Channel (Float.TYPE, nChannels*nSamples);

    // NEED TO GENERATE STEERING VECTORS AND PREDEC PULSE SHAPE HERE
  }

  public void work()
  {
    System.out.println("Running the DataSource Filter...");
    // Outer product of target beam and predec pulse shape
    // into sub matrix of output.
    int i, j;
    for(i = 0; i < numberOfChannels; i++)
    {
      for(j = 0; j < numberOfSamples; j++ )
      {
// 	if( j < targetSample || j > targetSample+predecPulseSize )
// 	{
	  output.pushFloat(0.0f);
// 	}
// 	else // outer prod of target beam and predec pulse shape
// 	{
// 	  output.pushFloat(predecPulseShape[j-targetSample]*steeringVectors[targetBeam+i*numberOfBeams]);
// 	}
      }
    }
  }
}
