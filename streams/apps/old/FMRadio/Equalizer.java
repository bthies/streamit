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
 * A local adder class.  It adds four floats.  It has input = 4 and
 * output = 1.
 */
class FloatAdder extends Filter
{
    public void init ()
    {
        input = new Channel (Float.TYPE, 4, 4);
        output = new Channel (Float.TYPE, 1);
    }

    public void work() {
        //subtract one from the other, round robin.
        output.pushFloat((float)(input.peekFloat(0)+input.peekFloat(1)+input.peekFloat(2)+input.peekFloat(3)));
        input.popFloat();
        input.popFloat();
    }
}

/**
 * Class Equalizer
 *
 * Implements an Equalizer for an FM Radio
 */

public class Equalizer extends Pipeline {

    float samplingRate;
    float mGain1;
    float mGain2;
    float mGain3;
    float mGain4;

    public Equalizer(float rate)
    {
        super(rate);
    }

    public void init(float rate)
    {
        input = new Channel (Float.TYPE, 1);
        output = new Channel (Float.TYPE, 1);

	samplingRate = rate;
	mGain1 = 1;
	mGain2 = 1;
	mGain3 = 1;
	mGain4 = 1;
	
	add(new SplitJoin(){
		public void init(){
		    setSplitter(DUPLICATE());
		    add(new BandPassFilter(samplingRate, 1250, 2500, 50, mGain1));
		    add(new BandPassFilter(samplingRate, 2500, 5000, 50, mGain2));
		    add(new BandPassFilter(samplingRate, 5000, 10000, 50, mGain3));
		    add(new BandPassFilter(samplingRate, 10000, 20000, 50, mGain4));
		    setJoiner(ROUND_ROBIN());
		}
	    });
	add(new FloatAdder());
    }
}
    











