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
import java.lang.Math;

/**
 * Class Filter
 *
 * Implements an FIR Filter
 */

public class QuadratureDemod extends Filter {
    int firing;
    float gain;

    public float multiplyReal(float a, float b, float c, float d) {
	return a*c - b*d;
    }

    public float multiplyImag(float a, float b, float c, float d) {
	return a*d - b*c;
    }

    public float argument(float a, float b) {
	return 
	    (float)(Math.asin(b/(Math.sqrt(Math.pow(a,2) + Math.pow(b,2)))));
    }

    public QuadratureDemod ()
    {
        super ();
    }

    public void init(int firingRate, float g) {

        input = new Channel (Float.TYPE, 2*firing);
        output = new Channel (Float.TYPE, firing);

	firing = firingRate;
	gain = g;
    }

    public void work() {
	float lastValReal, productReal, valReal;
	float lastValImag, productImag, valImag;

	lastValReal = input.peekFloat(1);
	lastValImag = input.peekFloat(0);

	for (int i = 0; i < firing; i++) {
	    valImag = input.popFloat();
	    valReal = input.popFloat();
	    
	    //product = val * conj(lastVal);
	    productReal = multiplyReal(valReal, valImag, lastValReal, -lastValImag);
	    productImag = multiplyImag(valReal, valImag, lastValReal, -lastValImag);

	    lastValReal = valReal;
	    lastValImag = valImag;
	    
	    output.pushFloat(gain * argument(productReal, productImag));
	}
    }
}
