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

import streamit.*;
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

    Channel input = new Channel (Float.TYPE, 1);

    public void initIO ()
    {
	streamInput = input;
    }

    public void init() {
    }

    public void work() {
	try{
	    c = input.popFloat();
	    //this is WAY wrong... fix it later (the castinG)
	    out.write((int)c);
	}
	catch(IOException e)
	    {
		System.err.println("IO Exception: " + e);
	    }
    }
}












