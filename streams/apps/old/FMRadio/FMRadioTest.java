/*
 * Test for testing LowPassFilter with float files
 */

import streamit.library.*;

public class FMRadioTest extends StreamIt
{
    static public void main(String[] t)
    {
        FMRadioTest test = new FMRadioTest();
        test.run(t);
    }
    
    public void init()
    {
	add(new FloatFileReader("FmRadioIn200Float"));
	add(new FMRadio());
	add(new FloatFileWriter("radioOutput"));
    }
}

        















