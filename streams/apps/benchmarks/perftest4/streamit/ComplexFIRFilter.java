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

public class ComplexFIRFilter extends Filter {
    int inSampFreq;
    int numtaps;
    int decimation;
    float phase_correctionReal, phase_correctionImag;
    float phase_corr_incrReal, phase_corr_incrImag;
    float center_freq, gain;
    float[] tapsReal;
    float[] tapsImag;
    float[] inputArray;


    public ComplexFIRFilter (int sampFreq, int dec, int t,
			     float freq, float g)
    {
        super (sampFreq, dec, t, freq, g);
	
    }

    public void init(int samplFreq, final int dec, int t,
		     float freq, float g) {	
        int index;
        float N;
        float M;
        float C_PI = (float)Math.PI;

        input = new Channel (Float.TYPE, dec, dec);
        output = new Channel (Float.TYPE,2);
	
	inSampFreq = samplFreq;
	numtaps = t;
	gain = g;
	center_freq = freq;
	decimation = dec;
	
	tapsReal = new float[numtaps];
	tapsImag = new float[numtaps];
	inputArray = new float[numtaps];

	phase_corr_incrReal = 1;
	phase_corr_incrImag = 0;
	
	phase_correctionReal = 1;
	phase_correctionImag = 0;

        N = (float)numtaps;
        M = N - 1;
	
        if (center_freq == 0.0) {
            for (index = 0; index < numtaps; index++) {
                tapsReal[index] = (float)
                    (gain * (0.54-0.46*Math.cos(2*C_PI*index/(M))));
                tapsImag[index] = 0;
            }
        }
        else {
            float arg = 2*C_PI*center_freq/(float)inSampFreq;
            for (index = 0; index < numtaps; index++) {
                tapsReal[index] = (float)
                    (gain*Math.cos(arg*((float)index)*
                                   (0.54-0.46*Math.cos(2*C_PI*index/(M)))));
                tapsImag[index] = (float)(gain*(-1)*Math.sin(arg*index)*
                                          (0.54-0.46*Math.cos(2*C_PI*index/(M))));
            }
            phase_corr_incrReal = (float)(Math.cos(arg*((float)decimation)));
            phase_corr_incrImag = (float)((-1)*Math.sin(arg*(float)decimation));
        }
    }

    public void work() {
	float resultReal = 0;
	float resultImag = 0;
	int i, t;

	for (t = 0; t < numtaps; t++) {
	    inputArray[t] = input.popFloat();
	}
	
	for (t = 0; t < (decimation - numtaps); t++) {
	  input.popFloat();
	}
	
	for(t = 0; t < numtaps; t++) {
		resultReal += tapsReal[t] * inputArray[t];
		resultImag += tapsImag[t];
	}
	
	if (center_freq != 0.0){
	   phase_correctionReal = (phase_correctionReal *  phase_corr_incrReal) -
		(phase_correctionImag * phase_corr_incrImag);
	    phase_correctionImag = (phase_correctionReal * phase_corr_incrImag) -
		(phase_correctionImag * phase_corr_incrReal);
	    
	    resultReal =  (resultReal *phase_correctionReal) -
		(resultImag *				       
		phase_correctionImag);
	    
	    resultImag =  (resultReal * phase_correctionImag) -
		(resultImag * 
		 phase_correctionReal);
	}
	
	output.pushFloat(resultReal);
	output.pushFloat(resultImag);
    }

}
