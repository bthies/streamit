/*
 * Test for decoding mp3 files
 */

import streamit.library.*;
import streamit.library.io.*;

import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;

import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

public class HeaderHuffmanDecoder extends Filter
{
    Bitstream bitstream;
    Decoder decoder;
    
    final int OUT_SIZE = 4 * (32 * (18 + 1) + 1);

    public void init()
    {

        try
        {
            String fName = new String("c:\\mp3\\Blur - Song 2.mp3");
            FileInputStream fin = new FileInputStream(fName);
            BufferedInputStream bin = new BufferedInputStream(fin);
            bitstream = new Bitstream(bin);

            output = new Channel (Float.TYPE, OUT_SIZE);
            
            decoder = new Decoder(output);
            
        } catch (Throwable t)
        {
            ASSERT(t);
        }

    }
    
    public void work ()
    {
        try
        {
            Header h = bitstream.readFrame();
            //if (h == null) ERROR ("done");
            
            SampleBuffer outputBuffer = (SampleBuffer) decoder.decodeFrame (h, bitstream);
            
        	bitstream.closeFrame();
        } catch (Throwable t)
        {
	    //ERROR (t);
        }
    }
}
