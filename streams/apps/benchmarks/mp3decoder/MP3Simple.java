/*
 * Test for decoding mp3 files
 */

import streamit.StreamIt;
import streamit.io.FileReader;
import streamit.io.FileWriter;
import streamit.Filter;
import streamit.Channel;

public class MP3Simple extends StreamIt
{
    public static void main(String[] args)
    {
        new MP3Simple ().run(args);
    }

    public void init()
    {
        // add .raw to the name to run on RAW
        add (new FileReader ("/u/karczma/traces-streamit/Blur.float.raw", Float.TYPE));
        add (new MultiChannelPCMSynthesis (2));
        add (new Filter ()
        {
            public void init () { this.input = new Channel (Short.TYPE, 1); }
            public void work () { System.out.println (this.input.popShort ()); };
        });
        //add (new FileWriter ("Blur.short.out", Short.TYPE));
		//add(new SoundOutput(44100, 2));
    }
}

