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
 * Class LowPassFilter
 *
 * Implements a Low Pass FIR Filter
 */

public class LowPassFilter extends Filter {

    int numberOfTaps;
    float COEFF[];
    float cutoffFreq, samplingRate;
    int mDecimation;

    public LowPassFilter(float sampleRate, float cutFreq, int numTaps, int decimation)
    {
        super(sampleRate, cutFreq, numTaps, decimation);
    }

    public void init(float sampleRate, float cutFreq, int numTaps, int decimation)
    {
	mDecimation = decimation;
        input = new Channel (Float.TYPE, 1+decimation, numTaps);
        output = new Channel (Float.TYPE, 1);

        //all frequencies are in hz
        samplingRate = sampleRate;
        cutoffFreq = cutFreq;
        numberOfTaps = numTaps;
        COEFF = new float[numTaps];

        float pi = (float)java.lang.Math.PI;
        //build the taps, and call super.init(taps[])
        float temptaps[] = new float[numberOfTaps];

        float m = numberOfTaps -1;
        //from Oppenheim and Schafer, m is the order of filter

        if(cutoffFreq == 0.0)
            {
                //Using a Hamming window for filter taps:
                float tapTotal = 0;

                for(int i=0;i<numberOfTaps;i++)
                    {
                        temptaps[i] = (float)(0.54 - 0.46*java.lang.Math.cos((2*pi)*(i/m)));
                        tapTotal += temptaps[i];
                    }

                //normalize all the taps to a sum of 1
                for(int i=0;i<numberOfTaps;i++)
                    {
                        temptaps[i] = temptaps[i]/tapTotal;
                    }
            }
        else{
            //ideal lowpass filter ==> Hamming window
            //has IR h[n] = sin(omega*n)/(n*pi)
            //reference: Oppenheim and Schafer

            float w = (2*pi) * cutoffFreq/samplingRate;

            for(int i=0;i<numberOfTaps;i++)
                {
                    //check for div by zero
                    if(i-m/2 == 0)
                        temptaps[i] = w/pi;
                    else
                        temptaps[i] = (float)(java.lang.Math.sin(w*(i-m/2)) / pi
                                       / (i-m/2) * (0.54 - 0.46
                                                    * java.lang.Math.cos((2*pi)*(i/m))));
                }
        }
        COEFF = temptaps;
        numberOfTaps = COEFF.length;
    }

    public void work() {
        float sum = 0;
        for (int i=0; i<numberOfTaps; i++) {
            sum += input.peekFloat(i)*COEFF[i];
        }

        input.popFloat();
        for(int i=0;i<mDecimation;i++)
            input.popFloat();
        output.pushFloat(sum);
    }
}













