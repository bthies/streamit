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
    super (nChannels, nSamples, nBeams, tarBeam, tarSample, predecPulseSize);
  }

  public void init(int nChannels, int nSamples, int nBeams,
		   int tarBeam, int tarSample, int predecPulseSize)
  {
    int i;
    numberOfBeams      = nBeams;
    numberOfChannels   = nChannels;
    numberOfSamples    = nSamples;
    targetBeam         = tarBeam;
    targetSample       = tarSample;
    this.predecPulseSize = predecPulseSize;
    steeringVectors    = new float [nBeams*nChannels*2];
    predecPulseShape   = new float [predecPulseSize*2];

    for(i = 0; i < nBeams*nChannels*2; i++)
    {
      steeringVectors[i] = ((float) i)/2.718281828f;
    }
    for(i = 0; i < predecPulseSize; i++)
    {
      predecPulseShape[i] = ((float) i)/1.414213562f;
    }


    output = new Channel (Float.TYPE, nChannels*nSamples*2);

    // NEED TO GENERATE STEERING VECTORS AND PREDEC PULSE SHAPE HERE
  }

  public void work()
  {
    //System.out.println("Running the DataSource Filter...");
    // Outer product of target beam and predec pulse shape
    // into sub matrix of output.
    int i, j;
    System.out.println(numberOfChannels);
    System.out.println(numberOfSamples);
    for(i = 0; i < numberOfChannels; i += 1)
    {
      for(j = 0; j < numberOfSamples*2; j += 2 )
      {
	float outReal;
	float outImag;
	//System.out.println(j);
 	if( j < targetSample*2 || j >= (targetSample*2+predecPulseSize*2) )
 	{
	  outReal = 17.0f;
	  outImag = 3.0f;
 	}
 	else // outer prod of target beam and predec pulse shape
 	{
	  outReal = predecPulseShape[j-targetSample*2]*steeringVectors[targetBeam*2+i*numberOfBeams]
	    - predecPulseShape[j-targetSample*2+1]*steeringVectors[targetBeam*2+i*numberOfBeams+1];
	  outImag = predecPulseShape[j-targetSample*2]*steeringVectors[targetBeam*2+i*numberOfBeams+1]
	    + predecPulseShape[j-targetSample*2+1]*steeringVectors[targetBeam*2+i*numberOfBeams];
 	}
	if( outReal != 17.0f )
	  System.out.println(outReal);
	output.pushFloat(outReal);
	output.pushFloat(outImag);
      }
    }
  }
}
