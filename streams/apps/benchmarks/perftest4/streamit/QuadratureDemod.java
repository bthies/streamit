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
import java.lang.Math;

/**
 * Class Filter
 *
 * Implements an FIR Filter
 */

public class QuadratureDemod extends Filter {
    int firing;
    float gain;

  
    public QuadratureDemod (int firingRate, float g)
    {
        super (firingRate, g);
    }

    public void init(final int firingRate, float g) {
	final int peekAmount;
	if (2*firingRate > 4) peekAmount =2*firingRate;
	else peekAmount = 4;

        input = new Channel (Float.TYPE, 2*firingRate, 2 * firingRate/*peekAmount*/);
        output = new Channel (Float.TYPE, firingRate);

	firing = firingRate;
	gain = g;
    }

    public void work() {
	float lastValReal, productReal, valReal;
	float lastValImag, productImag, valImag;
	int i;

	lastValReal = input.peekFloat(3);
	lastValImag = input.peekFloat(2);

	for (i = firing; i > 0; i--) {
	    valImag = input.popFloat();
	    valReal = input.popFloat();
	    
	    productReal = (valReal *lastValReal) - (valImag * lastValImag);
	    productImag = (valReal * (-lastValImag)) - (valImag *lastValReal);

	    lastValReal = valReal;
	    lastValImag = valImag;
	    
	    output.pushFloat
		(gain * (float)(Math.asin(productImag/
					  (Math.sqrt(Math.pow(productReal,2) + 
						     Math.pow(productImag,2))))));
	}
    }
}

