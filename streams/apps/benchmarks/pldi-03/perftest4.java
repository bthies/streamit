import streamit.*;
import java.lang.Math;

/**
 * This program is the amalgamation of perftest4 originally written by Mike Gordon.
 * Modified only to compile (eg removed import statements and public classes).
 * AAL 11/5/2002
 **/


class TestSource extends Filter{
    public void init() {
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
        output.pushFloat(0);
    }
}
	    
class NullSink extends Filter {
    public void init() {
	input = new Channel(Short.TYPE, 1);
    }
    public void work() {
	//System.out.println(input.popShort());
	input.popShort();
    }
}

class PerftestPipeline extends Pipeline {
    public PerftestPipeline(final float center_freq) {
	super(center_freq);
    }
    
    public void init(final float center_freq) {
	add(new ComplexFIRFilter(33000000, 825, 400, center_freq, 2));
	add(new QuadratureDemod(5, 0));
	add(new RealFIRFilter(8000, 5, 4000, 20, 1));
    }
}


class PerftestSplitJoin extends SplitJoin {
    public PerftestSplitJoin()
    {
        super();
    }

    public void init() {
	setSplitter(WEIGHTED_ROUND_ROBIN(825, 825, 825, 825));
	add(new PerftestPipeline(10370400));
	add(new PerftestPipeline(10355400));
	add(new PerftestPipeline(10340400));
	add(new PerftestPipeline(10960500));
	setJoiner(ROUND_ROBIN());
    }
}

public class perftest4 extends StreamIt
{
    
    static public void main(String[] t)
    {
        new perftest4().run(t);
    }
    
    public void init() {
	
	add(new TestSource());
	add(new PerftestSplitJoin());
	//add(new FileWriter("perftest.out", Short.TYPE));
	add(new NullSink());
    }
}	



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


/**
 * Class Filter
 *
 * Implements an FIR Filter
 */

class QuadratureDemod extends Filter {
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

/**
 * Class RealFIRFilter
 *
 * Implements an FIR Filter
 */

class RealFIRFilter extends Filter {
    int inSampFreq;
    int numTaps;
    float taps[];
    float cutoff, gain;
    int decimation;
    
    public RealFIRFilter (int sampleFreq, int dec, float c, int t, float g)
    {
        super (sampleFreq, dec, c, t, g);
    }

    public void init(int sampleFreq, final int dec, float c, final int t, float g) {
        input = new Channel (Float.TYPE, dec, t);
        output = new Channel (Short.TYPE, 1);
	
	inSampFreq = sampleFreq;
	numTaps = t;
	cutoff = c;
	gain = g;
	decimation = dec;
	taps = new float[numTaps];
	//buildFilter_real();
	//}

	//public void buildFilter_real() {
	float arg;
	float N = (float)numTaps;
	float M = N-1;
	float C_PI = (float)java.lang.Math.PI;
	int index;
	
	if (cutoff == 0.0) {
	    for (index=0;index < numTaps; index++) {
		taps[index] =  ((float)(gain*(0.54-0.46*((float)java.lang.Math.cos
						(2*C_PI*((float)index)/(M))))));
	    }
	}
	else {
	    arg = 2*C_PI*cutoff/((float)inSampFreq);
	    for (index=0;index < numTaps;index++) {
		if (((float)index)-(M/2.0) != 0){     
		    taps[index] =  ((float)
			(gain*(((float)java.lang.Math.sin(arg*(index-(M/2.0))))/
			      C_PI/(((float)index)-(M/2.0))*
			      (0.54-0.46*((float)java.lang.Math.cos(2.0*C_PI*
								    ((float)index)/M))))));
		}
	    }
	    if ( (((int)M)/2)*2 == (int)M ){ 
		taps[(int)M/2] =  gain*arg/C_PI;
	    }
	}
    }
        
    public void work() {
	int i;
  
	float sum = 0;
        for (i=0; i < decimation; i++) {
            sum += taps[i] * input.popFloat();
        }
	
	for (i = 0; i < numTaps - decimation; i++)
	    sum += taps[i + decimation] + input.peekFloat(i);

        output.pushShort((short)sum);
    }
}

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

/**
 * Class Filter
 *
 * Implements an FIR Filter
 */

class ComplexFIRFilter extends Filter {
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
