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

  float threshold

  public DataSource (int nBeams, int nSamples,
		     int tarBeam, int tarSample, float thresh)
  {
    super (nBeams, nSamples, tarBeam, tarSample, thresh);
  }

  public void initIO()
  {
    streamInput = input;
  }

  public void init(int nBeams, int nSamples,
		   int tarBeam, int tarSample, thresh)
  {
    numberOfBeams      = nBeams;
    numberOfSamples    = nSamples;
    targetBeam         = tarBeam;
    targetSample       = tarSample;
    threshold          = thresh;

    input = new Channel (Float.TYPE, numberOfBeams*numberOfSamples);
  }

  public void work()
  {
    // Need to associate each input value with a beam and sample
    //   check if input is above detection threshold
    //   if input is above detection threshhold and not in right place
    //     error
    //   if input is in right place but not above detection threshold
    //     error
  }

}
