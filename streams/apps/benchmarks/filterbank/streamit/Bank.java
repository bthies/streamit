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

import streamit.*;
import streamit.io.*;

/**
 * Class FirFilter
 *
 * Implements an FIR Filter
 */

public class Bank extends StreamIt {
 

    
class source extends Filter {
    int N;
    float[] r;
    public source(float[] r) {super(r);}
    public void init(float[] r){
	output = new Channel(Float.TYPE,r.length);
	this.r=r;
	N=r.length;
    }
    public void work(){
	for(int i=0;i<N;i++)
	     output.pushFloat(r[i]);
    }
}

class sink extends Filter{
    int N;
    public sink(int N) {super(N);}
    public void init(int N){
	input = new Channel(Float.TYPE, N);
	this.N=N;
	//setPop(N);
    }
    public void work() {
	System.out.println("Starting");

	for (int i=0; i< N;i++)
	    {
		System.out.print("This is ");
		System.out.print(i);
		System.out.print(" : ");
		System.out.println(input.popFloat());
	    }
	    
    }
}


        
    static public void main(String[] t)
    {
	Bank test=new Bank();
	test.run(t);
    }


    /*   public Bank (int N,float[] H,float[] F)
    {
        //super (N,H,F);
	}*/

    public void init( /* int N,float[] H,float[] F */) {
	
	  float[] H={1,2,3};
          float[] F={2,1,3};
	  float[] r={1,2,3,4,1,2};
	  int N=2;

  
	add (new source(r));
	add (new FIR(H));
	add (new DownSamp(N));
	add (new UpSamp(N));
	add (new FIR(F));
	add (new sink(r.length));
    }
    

}























































