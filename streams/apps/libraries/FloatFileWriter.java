/*
 *  Copyright 2001 Massachusetts Institute of Technology
 *
 *  Permission to use, copy, modify, distribute, and sell this software and its
 *  documentation for any purpose is hereby granted without fee, provided that
 *  the above copyright notice appear in all copies and that both that
 *  copyright notice and this permission notice appear in supporting
 *  documentation, and that the name of M.I.T. not be used in advertising or
 *  publicity pertaining to distribution of the software without specific,
 *  written prior permission.  M.I.T. makes no representations about the
 *  suitability of this software for any purpose.  It is provided "as is"
 *  without express or implied warranty.
 */

import streamit.library.*;
import java.io.*;

/**
 * Class FloatFileWriter
 *
 * Outputs floats to a file
 */

class FloatFileWriter extends Filter {

    File outputFile;
    FileWriter out;
    float c;

    public FloatFileWriter (String output)
    {
        super ();
    }

    public void init(String output)
    {
        input = new Channel (Float.TYPE, 1);
        try{
            outputFile = new File(output);
            out = new FileWriter(outputFile);
        }
        catch(FileNotFoundException e)
            {
                System.err.println("File not found: " + input + " exception: " + e);
            }
        catch(IOException e)
            {
                System.err.println("IO Exception: " + e);
            }
    }

    public void work() {
        try{
            //crude, but it'll do.
            c = input.popFloat();
            int d = Float.floatToIntBits(c);
            int out1 = (d&0xff000000)>>24;
            int out2 = (d&0x00ff0000)>>16;
            int out3 = (d&0x0000ff00)>>8;
            int out4 = (d&0x000000ff);
            out.write(out1);
            out.write(out2);
            out.write(out3);
            out.write(out4);
        }
        catch(IOException e)
            {
                System.err.println("IO Exception: " + e);
            }
    }
}












