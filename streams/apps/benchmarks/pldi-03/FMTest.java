import streamit.*; import streamit.io.*; import java.lang.Math;
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




/*
 * Software equalizer.  This version uses n+1 low-pass filters directly,
 * as opposed to n band-pass filters, each with two low-pass filters.
 * The important observation is that we have bands 1-2, 2-4, 4-8, ...
 * This means that we should run an LPF for each intermediate frequency,
 * rather than two per band.  Calculating this in StreamIt isn't that bad.
 * For a four-band equalizer:
 *
 *              |
 *             DUP
 *    +---------+---------+
 *    |         |         |
 *    |        DUP        |
 *    |    +----+----+    |
 *    |    |    |    |    |
 *   16    8    4    2    1
 *    |    |    |    |    |
 *    |  (dup)(dup)(dup)  |
 *    |    |    |    |    |
 *    |    +----+----+    |
 *    |       RR(2)       |
 *    |         |         |
 *    +---------+---------+
 *       WRR(1,2(n-1),1)
 *              |
 *            (a-b)
 *              |
 *            SUM(n)
 *              |
 *
 * It's straightforward to change the values of 1, 16, and n.  Coming out
 * of the EqualizerSplitJoin is 16 8 8 4 4 2 2 1; we can subtract and scale
 * these as appropriate to equalize.
 */

class FloatNAdder extends Filter
{
    int N;

    public FloatNAdder(int count)
    {
        super(count);
    }
    public void init (final int count)
    {
        N = count;
        input = new Channel (Float.TYPE, count, count);
        output = new Channel (Float.TYPE, 1);
    }

    public void work() {
        float sum = 0.0f;
        int i;
        for (i = 0; i < N; i++)
            sum += input.popFloat();
        output.pushFloat(sum);
    }
}

class FloatDiff extends Filter
{
    public void init()
    {
        input = new Channel(Float.TYPE, 2, 2);
        output = new Channel(Float.TYPE, 1);
    }
    public void work() 
    {
        output.pushFloat(input.peekFloat(0) - input.peekFloat(1));
	input.popFloat();
	input.popFloat();
    }
}

class FloatDup extends Filter
{
    public void init()
    {
        input = new Channel(Float.TYPE, 1, 1);
        output = new Channel(Float.TYPE, 2);
    }
    public void work()
    {
        float val = input.popFloat();
        output.pushFloat(val);
        output.pushFloat(val);
    }
}

class EqualizerInnerPipeline extends Pipeline 
{
    public EqualizerInnerPipeline(float rate, float freq)
    {
        super(rate, freq);
    }
    public void init(final float rate, final float freq)
    {
        add(new LowPassFilter(rate, freq, 64, 0));
        add(new FloatDup());
    }
}

class EqualizerInnerSplitJoin extends SplitJoin
{
    public EqualizerInnerSplitJoin(float rate, float low, float high, int bands)
    {
        super(rate, low, high, bands);
    }
    public void init(final float rate, final float low, final float high,
                     final int bands)
    {
        int i;
        setSplitter(DUPLICATE());
        for (i = 0; i < bands - 1; i++)
            add(new EqualizerInnerPipeline
                (rate,
                 (float)java.lang.Math.exp
                 ((i+1) *
                  (java.lang.Math.log(high) - java.lang.Math.log(low)) /
                  bands + java.lang.Math.log(low))));
        setJoiner(ROUND_ROBIN(2));
    }
}

class EqualizerSplitJoin extends SplitJoin {

    public EqualizerSplitJoin(float rate, float low, float high, int bands)
    {
        super(rate, low, high, bands);
    }

    public void init(final float rate, final float low, final float high,
                     final int bands)
    {
        // To think about: gains.
	
	setSplitter(DUPLICATE());
        add(new LowPassFilter(rate, high, 64, 0));
        add(new EqualizerInnerSplitJoin(rate, low, high, bands));
        add(new LowPassFilter(rate, low, 64, 0));
	setJoiner(WEIGHTED_ROUND_ROBIN(1, (bands-1)*2, 1));
    }
}

/**
 * Class Equalizer
 *
 * Implements an Equalizer for an FM Radio
 */

class Equalizer extends Pipeline {

    public Equalizer(float rate)
    {
        super(rate);
    }

    public void init(final float rate)
    {
        final int bands = 10;
        final float low = 55;
        final float high = 1760;
	add(new EqualizerSplitJoin(rate, low, high, bands));
        add(new FloatDiff());
	add(new FloatNAdder(bands));
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



class FloatOneSource extends Filter
{
    int x;
    public void init ()
    {
        output = new Channel(Float.TYPE, 1);
	x = 0;
    }
    public void work()
    {
        output.pushFloat(x++);
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



class FloatPrinter extends Filter
{
    public void init ()
    {
        input = new Channel(Float.TYPE, 1);
    }
    public void work ()
    {
        System.out.println(input.popFloat ());
    }
}





/*
 * Early attempt at an FM Radio... probably junk
 */



class FMRadio extends Pipeline
{
    public FMRadio()
    {
	super();
    }

    public void init()
    {
	final float samplingRate = 200000; //200khz sampling rate according to jeff at vanu
	final float cutoffFrequency = 108000000; //guess... doesn't FM freq max at 108 Mhz? 
	final int numberOfTaps = 64;
	final float maxAmplitude = 27000;
	final float bandwidth = 10000;
	//decimate 4 samples after outputting 1
        add(new LowPassFilter(samplingRate, cutoffFrequency, numberOfTaps, 4));
	add(new FMDemodulator(samplingRate, maxAmplitude, bandwidth));
	add(new Equalizer(samplingRate));
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
 * Class LowPassFilter
 *
 * Implements a Low Pass FIR Filter
 */

class LowPassFilter extends Filter {

    int numberOfTaps;
    float COEFF[];
    float cutoffFreq, samplingRate, tapTotal;
    int mDecimation;

    public LowPassFilter(float sampleRate, float cutFreq, int numTaps, int decimation)
    {
        super(sampleRate, cutFreq, numTaps, decimation);
    }

    public void init(final float sampleRate, final float cutFreq,
                     final int numTaps, final int decimation)
    {
	float pi, m, w;
	//float temptaps[];
	int i;
        samplingRate = sampleRate;
        cutoffFreq = cutFreq;
        numberOfTaps = numTaps;

        pi = (float)java.lang.Math.PI;
        //build the taps, and call super.init(taps[])
        //temptaps = new float[numberOfTaps];

        m = numberOfTaps -1;
        //from Oppenheim and Schafer, m is the order of filter

	mDecimation = decimation;
        input = new Channel (Float.TYPE, 1+decimation, numTaps);
        output = new Channel (Float.TYPE, 1);

        //all frequencies are in hz
        COEFF = new float[numTaps];

        if(cutoffFreq == 0.0)
            {
                //Using a Hamming window for filter taps:
                tapTotal = 0;

                for(i=0;i<numberOfTaps;i++)
                    {
                        COEFF[i] = (float)(0.54 - 0.46*java.lang.Math.cos((2*pi)*(i/m)));
                        tapTotal = tapTotal + COEFF[i];
                    }

                //normalize all the taps to a sum of 1
                for(i=0;i<numberOfTaps;i++)
                    {
                        COEFF[i] = COEFF[i]/tapTotal;
                    }
            }
        else{
            //ideal lowpass filter ==> Hamming window
            //has IR h[n] = sin(omega*n)/(n*pi)
            //reference: Oppenheim and Schafer

            w = (2*pi) * cutoffFreq/samplingRate;

            for(i=0;i<numberOfTaps;i++)
                {
                    //check for div by zero
                    if(i-m/2 == 0)
                        COEFF[i] = w/pi;
                    else
                        COEFF[i] = (float)(java.lang.Math.sin(w*(i-m/2)) / pi
                                       / (i-m/2) * (0.54 - 0.46
                                                    * java.lang.Math.cos((2*pi)*(i/m))));
                }
        }
        //COEFF = temptaps;
        // Is this actually useful?  StreamIt doesn't like .length,
        // and at any rate, COEFF.length will always be numTaps, which
        // will always have the same value as numberOfTaps.  --dzm
        // numberOfTaps = COEFF.length;
    }

    public void work() {
        float sum = 0;
        int i;
        for (i=0; i<numberOfTaps; i++) {
            sum += input.peekFloat(i)*COEFF[i];
        }

        input.popFloat();
        for(i=0;i<mDecimation;i++)
            input.popFloat();
        output.pushFloat(sum);
    }
}













/*
 * Test for testing LowPassFilter with float files
 */




public class FMTest extends StreamIt
{
    static public void main(String[] t)
    {
        new FMTest().run(t);
    }
    
    public void init()
    {
	add(new FloatOneSource());
	add(new FMRadio());
	add(new FloatPrinter());
    }
}

        















