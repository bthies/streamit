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
public class FilterBank extends Pipeline {
    
    
    public FilterBank (int N_samp,int N_ch,int N_col ,float[][] H,float[][] F)
    {
        super (N_samp,N_ch,N_col,H,F);
    }
    
    public void init( int N_samp,int N_ch,int N_col,float[][] H,float[][] F ) {
	
	add (new Branches(N_samp,N_ch,N_col,H,F));
	add (new Combine(N_samp));
	
    }
    
    
}
