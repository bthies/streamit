package streamit;

import streamit.scheduler2.ScheduleBuffers;

// a filter is the lowest-level block of streams
public abstract class Filter extends Stream
{

    public Filter(float a, float b, int c, int d)
    {
        super(a, b, c, d);
    }

    public Filter(int a, float b)
    {
        super(a, b);
    }

    public Filter(float[] b)
    {
        super(b);
    }

    public Filter(int a, float[] b)
    {
        super(a, b);
    }

    public Filter(int a, int[] b)
    {
        super(a, b);
    }

    public Filter(int a, float[][] b)
    {
        super(a, b);
    }

    public Filter(int a, int b, float[][] c)
    {
        super(a, b, c);
    }

    public Filter(float a, int b)
    {
        super(a, b);
    }

    public Filter(float a, float b)
    {
        super(a, b);
    }

    public Filter(float a, float b, int c)
    {
        super(a, b, c);
    }

    public Filter(float a, float b, float c)
    {
        super(a, b, c);
    }

    public Filter(float x, float y, float z, int a, float b)
    {
        super(x,y,z,a,b);
    }

    public Filter(int a, int b, float c, int d, float e)
    {
        super (a,b,c,d,e);
    }

    public Filter(int a, int b, int c, float d, float e)
    {
        super (a,b,c,d,e);
    }

    public Filter(int a, int b, int c, int d)
    {
        super (a,b,c,d);
    }

    public Filter(int a, int b, int c, float d, int e)
    {
        super (a,b,c,d,e);
    }

    public Filter()
    {
        super();
    }

    public Filter(char c)
    {
        super(c);
    }

    public Filter(int i)
    {
        super(i);
    }

    public Filter(int n1, int n2) {
        super(n1, n2);
    }

    public Filter(int n1, int n2, int n3) {
        super(n1, n2, n3);
    }

    public Filter(int n1, int n2, int n3,
                  int n4, float f1) {
        super(n1, n2, n3, n4, f1);
    }

    public Filter(int n1, int n2, int n3,
                  int n4, int n5, int n6) {
        super(n1, n2, n3, n4, n5, n6);
    }

    public Filter(boolean b1)
    {
        super(b1);
    }

     public Filter(float f)
    {
        super(f);
    }

    public Filter(String str)
    {
        super(str);
    }

    // add was present in Operator, but is not defined in Filter anymore
    public void add(Stream s) { ASSERT (false); }

    // data for support of syntax in the CC paper
    int peekAmount = -1, popAmount = -1, pushAmount = -1;
    Class inputType=null, outputType=null;
    boolean ccStyleInit = false;
    
    // functions for support of syntax in the CC paper
    public void setPeek (int peek)
    {
        peekAmount = peek;
        ccStyleInit = true;
    }
    
    public void setPop (int pop)
    {
        popAmount = pop;
        ccStyleInit = true;
    }
    
    public void setPush (int push)
    {
        pushAmount = push;
        ccStyleInit = true;
    }
    
    public void setOutput (Class type)
    {
        outputType = type;
        ccStyleInit = true;
    }

    public void setInput (Class type)
    {
        inputType = type;
        ccStyleInit = true;
    }

    // connectGraph doesn't connect anything for a Filter,
    // but it can register all sinks:
    // also make sure that any input/output point to the filter itself
    public void connectGraph ()
    {
    	if (ccStyleInit)
    	{
    	    if (outputType != null)
    	    {
                output = new Channel (outputType, pushAmount);
    	    }
    	    
    	    if (inputType != null)
    	    {
    	        if (peekAmount == -1) peekAmount = popAmount;
    	        input = new Channel (inputType, popAmount, peekAmount);
    	    }
    	}
    	
        Channel myInput = getInputChannel ();
        Channel myOutput = getOutputChannel ();

        if (myOutput != null)
        {
            myOutput.setSource (this);
        } else {
            addSink ();
        }

        if (myInput != null)
        {
            myInput.setSink (this);
        }

        addFilter ();
        initCount ();
    }

    public abstract void work();

    // provide some empty functions to make writing filters a bit easier
    public void init () { invalidInitError (); }

    // some constants necessary for calculating a steady flow:
    public int popCount = 0, pushCount = 0, peekCount = 0;

    // and the function that is supposed to initialize the constants above
    final void initCount ()
    {
        if (getInputChannel () != null)
        {
            popCount = getInputChannel ().getPopCount ();
            peekCount = getInputChannel ().getPeekCount ();
        }

        if (getOutputChannel () != null)
        {
            pushCount = getOutputChannel ().getPushCount ();
        }
    }

    void setupBufferLengths (ScheduleBuffers buffers)
    {
        // this function doesn't need to do anything
    }
}
