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
 * Class AmDemodulator
 *
 * Implements an AM Demodulator 
 *
 */

class AMDemodulator extends Filter {
    
    float sampleRate;
    float Average;
    float gain;

    public AMDemodulator (float sampRate, float Gain)
    {
	super ();
	sampleRate = sampRate;
	Average = 0;
	gain = Gain;
	//gain = 1;
    }

    Channel input = new Channel (Float.TYPE, 1);
    Channel output = new Channel (Float.TYPE, 1);

    public void initIO ()
    {
	streamInput = input;
	streamOutput = output;
    }

    public void init()
    {
    }

    public void work() {
	float buffer = Math.abs(input.popFloat());
	Average = (float)((0.999)*Average) + (float)((0.001)*buffer);
	//for speed, you might later remove the gain and assume gain = 1
	output.pushFloat(gain *(buffer-Average)); 
    }
}
