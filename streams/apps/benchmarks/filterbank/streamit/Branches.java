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


// This is the complete Filter Bank Split Join Structure

import streamit.library.*;
import streamit.library.io.*;

/**
 * Class Branches
 *
 * Implements Branches  Structure
 */

public class Branches extends SplitJoin {


 public Branches (int N_samp,int N_rows,int N_col,float[][] H,float[][] F)

    {
        super (N_samp,N_rows,N_col,H,F);
    }

    public void init( int N_samp,int N_ch,int N_col,float[][] H,float[][] F ) {
	setSplitter(DUPLICATE());
	for (int i=0; i<N_ch ; i++)
	    {
	    float[] H_ch=new float[N_col];
	    float[] F_ch=new float[N_col];
	    
	    	for (int j=0; j<N_col;j++)
		{
		    H_ch[j]=H[i][j];
		    F_ch[j]=F[i][j];
		}

		add (new Bank(N_samp,N_col,H_ch,F_ch));
	    }
			
	setJoiner(ROUND_ROBIN());
		    
    }
    

}























































