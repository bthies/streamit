/*
 * Early attempt at an AM Radio... probably junk
 */

import streamit.library.*;

public class AMRadio extends Stream
{
    static public void main(String[] t)
    {
        AMRadio test = new AMRadio();
        test.run(t);
    }
    
    public void init()
    {
	//this is junk data... for example only
	float samplingRate = 200; //no clue as to real sampling rate. Just for an example.
	float cutoffFrequency = 200; //ditto
	int numberOfTaps = 50;
	float gain = 1;
        add(new LowPassFilter(samplingRate, cutoffFrequency, numberOfTaps));
	add(new AMDemodulator(samplingRate, gain));
	//need somewhere for the last stuff to go...
    }
}

        















