/*
 * Test for testing LowPassFilter with float files
 */

import streamit.*;
import java.lang.Math;
import java.io.*;


public class FMTest extends StreamIt
{
    static public void main(String[] t)
    {
        new FMTest().run();
    }
    
    public void init()
    {
	add(new FloatFileReader("FmRadioIn200Float"));
	add(new FMRadio());
	add(new FloatFileWriter("radioOutput"));
    }
}

        















