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
        add(new FloatFileReader("floats5000"));
        add(new LowPassFilter(samplingRate, cutoffFrequency, numberOfTaps,0));
        add(new FloatFileWriter("filteroutput"));
    }
}

















