/*
 * Test for decoding mp3 files
 */

import streamit.*;
import streamit.io.*;

public class MP3Simple extends StreamIt
{
    static public void main(String[] args)
    {
        new MP3Simple ().run(args);
    }

    public void init()
    {
        add (new FileReader ("Blur.float.raw", Float.TYPE));
        add (new MultiChannelPCMSynthesis (2));
        add (new FileWriter ("Blur.short.out", Short.TYPE));
		//add(new SoundOutput(44100, 2));
    }
}

