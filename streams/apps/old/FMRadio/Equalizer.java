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
	 input.popFloat();
	 input.popFloat();
    }
}

class EqualizerSplitJoin extends SplitJoin {

    public EqualizerSplitJoin(float rate)
    {
        super(rate);
    }

    public void init(final float rate)
    {
	final float mGain1 = 1;
	final float mGain2 = 1;
	final float mGain3 = 1;
	final float mGain4 = 1;
	
	setSplitter(DUPLICATE());
	add(new BandPassFilter(rate, 1250, 2500, 50, mGain1));
	add(new BandPassFilter(rate, 2500, 5000, 50, mGain2));
	add(new BandPassFilter(rate, 5000, 10000, 50, mGain3));
	add(new BandPassFilter(rate, 10000, 20000, 50, mGain4));
	setJoiner(ROUND_ROBIN());
    }
}

/**
 * Class Equalizer
 *
 * Implements an Equalizer for an FM Radio
 */

public class Equalizer extends Pipeline {

    public Equalizer(float rate)
    {
        super(rate);
    }

    public void init(float rate)
    {
	add(new EqualizerSplitJoin(rate));
	add(new FloatAdder());
    }
}
    











