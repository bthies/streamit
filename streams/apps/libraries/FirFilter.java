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
 * Class FirFilter
 *
 * Implements an FIR Filter 
 */

class FirFilter extends Filter {

    int numberOfTaps;
    float COEFF[]; 

    public FirFilter (int numTaps)
    {
	super ();
	numberOfTaps = numTaps;
	COEFF = new float[numTaps];
    }

    Channel input = new Channel (Float.TYPE, 1);
    Channel output = new Channel (Float.TYPE, 1);

    public void initIO ()
    {
	streamInput = input;
	streamOutput = output;
    }

    public void init() {
	//Build the FIR Coefficients
	//NEED TO DO THIS
	//XXXMJB
    }

    public void init(float taps[])
    //in case someone wants to specify the taps explicitly
    //anyone who uses this function has to be very careful.
    {
	COEFF = taps;
	numberOfTaps = taps.length;
    }

    public void work() {
	float sum = 0;
	for (int i=0; i<numberOfTaps; i++) {
	    sum += input.peekFloat(i)*COEFF[i];
	}

	input.popFloat();
	output.pushFloat(sum);
    }
}












