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

  public DataSource (int nBeams, int nChannels, int nSamples,
		     int tarBeam, int tarSample, int predecPulseSize)
  {
    super (nBeams, nChannels, nSamples, tarBeam, tarSample, predecPulseSize);
  }

  public void initIO()
  {
    streamOutput = output;
  }

  public void init(int nBeams, int nChannels, int nSamples,
		   int tarBeam, int tarSample, int predecPulseSize)
  {
    numberOfBeams      = nBeams;
    numberOfChannels   = nChannels;
    numberOfSamples    = nSamples;
    targetBeam         = tarBeam;
    targetSample       = tarSample;
    predecPulseSize    = predecPulseSize;
    steeringVectors    = new float(numberOfBeams*numberOfChannels);
    predecPulseShape   = new float(predecPulseSize);

    output = new Channel (Float.TYPE, numberOfChannels*numberOfSamples);

    // NEED TO GENERATE STEERING VECTORS AND PREDEC PULSE SHAPE HERE
  }

  public void work()
  {
    // Outer product of target beam and predec pulse shape
    // into sub matrix of output.
  }

}
