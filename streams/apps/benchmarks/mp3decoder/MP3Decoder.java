/*
 * Test for decoding mp3 files
 */

import streamit.library.*;
import streamit.library.io.*;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

public class MP3Decoder extends StreamIt
{
    static public void main(String[] args)
    {
        new MP3Decoder().run(args);
    }

    Bitstream bitstream;

    public void init()
    {
        try
        {
            // open the file and figure out the rate & # of channels
            String fName = new String("c:\\mp3\\Blur - Song 2.mp3");
            FileInputStream fin = new FileInputStream(fName);
            BufferedInputStream bin = new BufferedInputStream(fin);
            bitstream = new Bitstream(bin);

            Header h = bitstream.readFrame();
            float sampling_rate = h.frequency();
            int nChannels = (h.mode() == h.SINGLE_CHANNEL ? 1 : 2);

            add(new HeaderHuffmanDecoder());
            add (new SplitJoin ()
            {
                public void init ()
                {
                    setSplitter (DUPLICATE ());
                    add (new Filter ()
                    {
                        public void init ()
                        {
                            input = new Channel (Float.TYPE, 1);
                            output = new Channel (Float.TYPE, 1);
                        }
                        
                        public void work ()
                        {
                            output.pushFloat (input.popFloat ());
                        }
                    });
                    add (new FileWriter ("Blur.float.raw", Float.TYPE));
                    setJoiner (WEIGHTED_ROUND_ROBIN (1,0));
                }
            });
            add(new MultiChannelPCMSynthesis (nChannels));
            add(new SoundOutput(sampling_rate, nChannels));
        } catch (Throwable t)
        {
            //ERROR(t);
        }
    }
}
