import streamit.StreamIt;
import streamit.Pipeline;
import streamit.SplitJoin;
import streamit.Filter;
import streamit.Channel;

public class FIRfine extends StreamIt
{
    public static void main (String [] args)
    {
        new FIRfine ().run (args);
    }
    
    public void init ()
    {
        add (new FloatSource (10000));
        add (new FIR (64));
	add (new FloatPrinter (10000));
    }
}

class FIR extends Pipeline
{
    FIR (int N)
    {

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
		    this.output.pushFloat(this.input.popFloat());
		    this.output.pushFloat(0);
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
		    this.input.popFloat();
		    this.output.pushFloat(this.input.popFloat());
		}
	    });
    }
}

class SingleMultiply extends Filter
{
    SingleMultiply(int i)
    {

    }

    float W;
    public void init(final int i) {
	W = 2*i*i/((float)i+1);
	this.input = new Channel(Float.TYPE, 2, 2);
	this.output = new Channel(Float.TYPE, 2);
    }
    
    public void work() {
	float c, s; 
	c = this.input.popFloat();
	s = this.input.popFloat();
	this.output.pushFloat(c);
	this.output.pushFloat(s+c*W);
    }
}

class FloatSource extends Filter
{
    FloatSource (float maxNum)
    {

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

    }
    int x;
    public void init (int x2)
    {
        input = new Channel (Float.TYPE, 1);
        this.x = x2;

    }
    public void work ()
    {
      System.out.println(input.popFloat ());
    }
}

class FloatIdentity extends Filter
{
    public void init ()
    {
        input = new Channel (Float.TYPE, 1);
        output = new Channel (Float.TYPE, 1);
    }
    public void work ()
    {
        float i = input.popFloat ();
        output.pushFloat (i);
    }
}
