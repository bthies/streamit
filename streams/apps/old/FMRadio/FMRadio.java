/*
 * Early attempt at an FM Radio... probably junk
 */

import streamit.*;

public class FMRadio extends Pipeline
{
    static public void main(String[] t)
    {
        FMRadio test = new FMRadio();
        test.run();
    }
    
    public void init()
    {
	//this is junk data... for example only
	float samplingRate = 200; //no clue as to real sampling rate. Just for an example.
	float cutoffFrequency = 200; //ditto
	int numberOfTaps = 50;
	float maxAmplitude = 200;
	float bandwidth = 1000;
        add(new LowPassFilter(samplingRate, cutoffFrequency, numberOfTaps));
	add(new FMDemodulator(samplingRate, maxAmplitude, bandwidth));
	//need somewhere for the last stuff to go...
    }
}

        















