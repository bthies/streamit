/*
 *  Copyright 2002 Massachusetts Institute of Technology
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


// This is the complete FIR pipeline

import streamit.library.*;
import streamit.library.io.*;

/**
 * Class FirFilter
 *
 * Implements an FIR Filter
 */

public class Bank extends Pipeline {
 

    public Bank (int N,int L,float[] H,float[] F)
    {
        super (N,L,H,F);
	}

    public void init(  int N,int L,float[] H,float[] F ) {
	
  
	///add (new source(r)); They are here for debugging purposes
	//add (new FIR(H));
        // Bill says to inline:
	add (new Delay_N(L-1));
	add (new FirFilter(L,H));
        //
	add (new DownSamp(N));
	add (new UpSamp(N));
	//add (new FIR(F));
        // inlining again:
	add (new Delay_N(L-1));
	add (new FirFilter(L,F));
	///add (new sink(r.length));
    }
    

}























































