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
 * Class FMDemodulator
 *
 * Implements an FM Demodulator
 *
 */

class FMDemodulator extends Filter {

  float mGain;
  float sampleRate;
  float maxAmplitude;
  float modulationBandwidth;

    public FMDemodulator (float sampRate, float max, float bandwidth)
    {
        super (sampRate, max, bandwidth);
    }

    public void init(float sampRate, float max, float bandwidth)
    {
        input = new Channel (Float.TYPE, 1, 2);
        output = new Channel (Float.TYPE, 1);

        sampleRate = sampRate;
        maxAmplitude = max;
        modulationBandwidth = bandwidth;

        mGain = maxAmplitude*(sampleRate
                            /(modulationBandwidth*(float)Math.PI));
    }

    public void work() {
        float temp = 0;
        //may have to switch to complex?
        temp = (float)((input.peekFloat(0)) * (input.peekFloat(1)));
        //if using complex, use atan2
        temp = (float)(mGain * Math.atan(temp));

        input.popFloat();
        output.pushFloat(temp);
    }
}
