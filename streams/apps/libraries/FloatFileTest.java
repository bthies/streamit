/*
 * Test for testing LowPassFilter with float files
 */

import streamit.*;

public class FloatFileTest extends Stream
{
    static public void main(String[] t)
    {
        FloatFileTest test = new FloatFileTest();
        test.run();
    }
    
    public void init()
    {
	add(new FloatFileReader("floats5000"));
	//add(new LowPassFilter(samplingRate, cutoffFrequency, numberOfTaps));
	add(new FloatFileWriter("filteroutput"));
    }
}

        















