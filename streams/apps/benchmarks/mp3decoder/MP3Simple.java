/*
 * Test for decoding mp3 files
 */

import streamit.StreamIt;
import streamit.io.FileReader;
import streamit.io.FileWriter;

public class MP3Simple extends StreamIt
{
    public static void main(String[] args)
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

