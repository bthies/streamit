/**
 * Note that the first N/2 outputs of this are bogus depending on your
 * definition of FIR -- they represent only a partial convolution of
 * the weights with the inputs.  FIRcoarse in this directory has
 * output starting at N/2 of this.
 */

import streamit.*;
import streamit.io.*;

public class FIRfine extends StreamIt
{
    public static void main (String [] args)
    {
        new FIRfine ().run (args);
    }
    
    public void init ()
    {
        add (new FloatSource (10000));
        add (new FIR (128));
	//add (new FileWriter("output.dat", Float.TYPE));
	add (new FloatPrinter (10000));
    }
}

class FIR extends Pipeline
{
    FIR (int N)
    {
	super(N);
    }

    public void init (final int N)
    {
	int i;
	add(new Filter() {
		public void init() {
		    this.input = new Channel(Float.TYPE, 1, 1);
		    this.output = new Channel(Float.TYPE, 2);
		}
		public void work() {
		    this.output.pushFloat(0);
		    this.output.pushFloat(this.input.popFloat());
		}
	    });
	for(i=0; i<N; i++)
	    add (new SingleMultiply(i));
	add(new Filter() {
		public void init() {
		    this.input = new Channel(Float.TYPE, 2, 2);
		    this.output = new Channel(Float.TYPE, 1);
		}
		public void work() {
		    this.output.pushFloat(this.input.popFloat());
		    this.input.popFloat();
		}
	    });
    }
}

class SingleMultiply extends Filter
{
    SingleMultiply(int i)
    {
	super(i);
    }

    float W;
    float last;
    public void init(final int i) {
	last = 0;
	W = 2*i*i/((float)i+1);
	this.input = new Channel(Float.TYPE, 12);
	this.output = new Channel(Float.TYPE, 12);
    }
    
    public void work() {
	for (int i=0; i<6;i++) {
	float s; 
	s = this.input.popFloat();
	this.output.pushFloat(s+last*W);
	this.output.pushFloat(last);
	last = this.input.popFloat();
	}
    }
}

class FloatSource extends Filter
{
    FloatSource (float max)
    {
	super(max);
    }
    
    float num;
    float maxNum;
    
    public void init (float maxNum2)
    {
        output = new Channel (Float.TYPE, 1);
        this.maxNum = maxNum2;
        this.num = 0;
    }
    
    public void work ()
    {
        output.pushFloat (num);
        num++;
        if (num == maxNum) num = 0;
    }
}

class FloatPrinter extends Filter 
{
    FloatPrinter (int x)
    {
	super(x);
    }
    int x;
    public void init (int x2)
    {
        input = new Channel (Float.TYPE, 1);
        this.x = x2;

    }
    public void work ()
    {
      System.out.println(input.popFloat());
    }
}

