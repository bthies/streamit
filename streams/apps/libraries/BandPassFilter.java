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
import streamit.library.*;

/**
 * A local subtractor class.  It subtracts two floats.  It has input = 2 and
 * output = 1.
 */
class FloatSubtract extends Filter
{
    public void init ()
    {
        input = new Channel (Float.TYPE, 2, 2);
        output = new Channel (Float.TYPE, 1);
    }

    public void work() {
        //subtract one from the other, round robin.
        output.pushFloat((float)(input.peekFloat(0)-input.peekFloat(1)));
	input.popFloat();
	input.popFloat();
    }
}


/**
 * Need to have a separate class for now (instead of inlining this
 * splitjoin) so that constant prop. will work.
 */
class BandPassSplitJoin extends SplitJoin {

    public BandPassSplitJoin(float sampleRate, 
			     float lowFreq, 
			     float highFreq,
			     int numTaps) {
	super(sampleRate, lowFreq, highFreq, numTaps);
    }

    public void init (float sampleRate, 
		      float lowFreq, 
		      float highFreq, 
		      int numTaps) {
	setSplitter(DUPLICATE());
	this.add(new LowPassFilter(sampleRate, highFreq, numTaps, 0));
	this.add(new LowPassFilter(sampleRate, lowFreq, numTaps, 0));
	setJoiner(ROUND_ROBIN());
    }
}

/**
 * Class BandPassFilter
 *
 * Implements a Low Pass FIR Filter
 */

public class BandPassFilter extends Pipeline {

    public BandPassFilter(float sampleRate, float lowFreq, float highFreq, int numTaps, float gain)
    {
        super(sampleRate, lowFreq, highFreq, numTaps, gain);
    }


    public void init(float sampleRate, float lowFreq, float highFreq, int numTaps, float gain)
    {
        add(new BandPassSplitJoin(sampleRate, lowFreq, highFreq, numTaps));
        add (new FloatSubtract ());
    }

}













