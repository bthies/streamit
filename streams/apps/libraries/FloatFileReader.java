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
 * Class FloatFileReader
 *
 * Inputs floats from a file and outputs them to it's output port.
 */

class FloatFileReader extends Filter {

    File inputFile;
    FileReader in;
    int c,d;


    public FloatFileReader (String input)
    {
        super ();
    }

    public void init(String input)
    {
        output = new Channel (Float.TYPE, 1);
        try{
            inputFile = new File(input);
            in = new FileReader(inputFile);
        }
        catch(FileNotFoundException e)
            {
                System.err.println("File not found: " + input + " exception: " + e);
            }
    }

    public void work() {
	try{
	    //each read only does 1 byte.... take in four, and meld em
	    c = 0; //clear c
	    for(int i = 0; i<4; i++)
		if((d = in.read()) != -1)
		    {
			c += (d << ((3-i)<<3));
		    }
		else{
		    //System.err.println("End of file reached.");
		    System.exit(0);
		    //return;
		}
	    output.pushFloat(Float.intBitsToFloat(c));
	}
	catch(IOException e)
	    {
		System.err.println("IO Exception: " + e);
	    }
    }
}












