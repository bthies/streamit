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
 * Class vRamp
 *
 * Implements a Ramp Function given, start, stride and num-elems
 */

public class vRamp extends Filter {

    int numberOfElems;
    int start;
    int stride;

    public vRamp (int numStart, int numStride, int numElems)
    {
        super ();
    }

    public void init (int numStart, int numStride, int numElems)
    {
        output = new Channel (Float.TYPE, numElems);

        numberOfElems = numElems;
        start = numStart;
        stride = numStride;
    }


    public void work() {
        int currVal;
        currVal = start;

        for (int i=0; i<numberOfElems; i++) {
            output.pushFloat(currVal);
            currVal += stride;
        }

    }
}












