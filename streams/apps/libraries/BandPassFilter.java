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

import java.lang.Math.*;
import streamit.*;

/**
 * Class BandPassFilter
 *
 * Implements a Low Pass FIR Filter
 */

class BandPassFilter extends Filter {

    int numberOfTaps;
    float samplingRate;
    int mDecimation;
    float mGain; 
    float mLowFreq;
    float mHighFreq;

    public BandPassFilter(float sampleRate, float lowFreq, float highFreq, int numTaps, float gain)
    {
	super(sampleRate, lowFreq, highFreq, numTaps, gain);
    }

    Channel input = new Channel (Float.TYPE, 2);
    Channel output = new Channel (Float.TYPE, 1);

    public void initIO ()
    {
	streamInput = input;
	streamOutput = output;
    }


    public void init(float sampleRate, float lowFreq, float highFreq, int numTaps, float gain)
    {
        //all frequencies are in hz
        samplingRate = sampleRate;
        mHighFreq = highFreq;
        mLowFreq = lowFreq;
        mGain = gain;
        numberOfTaps = numTaps;

	add(new SplitJoin() {
		public void init () {
		    setSplitter(DUPLICATE());
		    add(new LowPassFilter(samplingRate, mHighFreq, numberOfTaps));
		    add(new LowPassFilter(samplingRate, mLowFreq, numberOfTaps));
		    setJoiner(ROUND_ROBIN());
		}
	    });
    }

    public void work() {
	    //subtract one from the other, round robin.
	output.pushFloat(mGain*(float)(input.peekFloat(0)-input.peekFloat(1)));
	input.popFloat();
	input.popFloat();
    }
}













