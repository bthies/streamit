/*
 * Early attempt at an FM Radio... probably junk
 */

import streamit.library.*;

public class FMRadio extends Pipeline
{
    public FMRadio()
    {
	super();
    }

    public void init()
    {
	// final float samplingRate = 200000; //200khz sampling rate according to jeff at vanu
        final float samplingRate = 250000000; //250 MHz sampling rate much more sensible, though
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

        















