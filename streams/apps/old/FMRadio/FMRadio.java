/*
 * Early attempt at an FM Radio... probably junk
 */

import streamit.*;

public class FMRadio extends Pipeline
{
    public FMRadio()
    {
	super();
    }
    public void init()
    {
	//this is junk data... for example only
	float samplingRate = 200000; //200khz sampling rate according to jeff at vanu
	float cutoffFrequency = 108000000; //guess... doesn't FM freq max at 108 Mhz? 
	int numberOfTaps = 100;
	float maxAmplitude = 27000;
	float bandwidth = 10000;
	//decimate 4 samples after outputting 1
        add(new LowPassFilter(samplingRate, cutoffFrequency, numberOfTaps, 4));
	add(new FMDemodulator(samplingRate, maxAmplitude, bandwidth));
	add(new Equalizer(samplingRate));
    }
}

        















