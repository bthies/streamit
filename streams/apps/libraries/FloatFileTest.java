/*
 * Test for testing LowPassFilter with float files
 */

import streamit.*;

public class FloatFileTest extends StreamIt
{
    static public void main(String[] t)
    {
        FloatFileTest test = new FloatFileTest();
        test.run();
    }

    public void init()
    {
	float samplingRate = 10;
	float cutoffFrequency = 200;
	int numberOfTaps = 30;
        add(new FloatFileReader("floats5000"));
        add(new LowPassFilter(samplingRate, cutoffFrequency, numberOfTaps, 0));
        add(new FloatFileWriter("filteroutput"));
    }
}

















