package streamit;

import java.util.*;
import streamit.scheduler2.Scheduler;

// the basic stream class (pipe's).  has 1 input and 1 output.
public abstract class Stream extends Operator
{

    public Channel input = null;
    public Channel output = null;

    LinkedList streamElements = new LinkedList ();

    // CONSTRUCTORS --------------------------------------------------------------------
    public Stream(float a, float b, int c)
    {
        super(a, b, c);
    }

    public Stream(int a, float b)
    {
        super(a, b);
    }

    public Stream(float[] b)
    {
        super(b);
    }

    public Stream(int a, float[] b)
    {
        super(a, b);
    }

    public Stream(int a, int[] b)
    {
        super(a, b);
    }

    public Stream(int a, float[][] b)
    {
        super(a, b);
    }

    public Stream (int i1, int i2, float f) {
        super(i1, i2, f);
    }

    public Stream (int i1, int i2, float f1, float f2) {
        super(i1, i2, f1, f2);
    }

    public Stream(int a, int b, float[] c)
    {
        super(a, b, c);
    }

    public Stream(int a, int b, float[][] c)
    {
        super(a, b, c);
    }

    public Stream (int a, float[] c, float[] d) 
    { 
        super (a, c, d); 
    }

    public Stream(int a, int b, int c, int d, float[][] e)
    {
        super(a, b, c, d, e);
    }

    public Stream(int a, int b, int c, int d, float[][] e, float[][] f)
    {
        super(a, b, c, d, e, f);
    }

    public Stream(int a, int b, int c, float[][] e, float[][] f)
    {
        super(a, b, c, e, f);
    }

    public Stream(float a, int b)
    {
        super(a, b);
    }

    public Stream(float a, float b)
    {
        super(a, b);
    }

    public Stream(float a, float b, float c)
    {
        super(a, b, c);
    }

    public Stream(float a, float b, float c, float d)
    {
        super(a, b, c, d);
    }

    public Stream(float a, float b, int c, int d)
    {
        super(a, b, c, d);
    }

    public Stream(float x, float y, float z, int a, float b)
    {
        super(x,y,z,a,b);
    }

    public Stream(int a, int b, int c, float d, int e)
    {
        super (a,b,c,d,e);
    }

    public Stream(int a, int b, int c, float d, float e)
    {
        super (a,b,c,d,e);
    }

    public Stream(float a, float b, float c, int d, int e, int f)
    {
        super (a,b,c,d,e,f);
    }

    public Stream(float a, float b, int c, int d, int e, int f)
    {
        super (a,b,c,d,e,f);
    }

    public Stream(int a, int b, float c, int d, float e)
    {
        super (a,b,c,d,e);
    }

    public Stream(float x, float y, float z, int a)
    {
        super(x,y,z,a);
    }

    public Stream ()
    {
        super ();
    }

    public Stream(char c)
    {
        super (c);
    }

    public Stream(int n)
    {
        super (n);
    }

    public Stream(boolean b1)
    {
        super(b1);
    }

    public Stream(int n1, int n2, boolean b1)
    {
        super(n1, n2, b1);
    }

    public Stream(int n1, boolean b1)
    {
        super(n1, b1);
    }

    public Stream (int x, int y)
    {
        super (x, y);
    }

    public Stream (int x, int y, int z)
    {
        super (x, y, z);
    }

    public Stream (int x, int y, int z, float[][] f)
    {
        super (x, y, z, f);
    }

    public Stream (int x, int y, int z, int a)
    {
        super (x, y, z, a);
    }

    public Stream (int a, int b, int c, int d, int e) { super(a, b, c, d, e); }

    public Stream(int n1, int n2, int n3,
		  int n4, float f1) {
      super(n1, n2, n3, n4, f1);
    }

    public Stream (int x, int y, int z,
		   int a, int b, int c)
    {
        super (x, y, z, a, b, c);
    }

    public Stream (int x, int y, int z,
		   int a, int b, int c, int d, float f)
    {
        super (x, y, z, a, b, c, d, f);
    }

    public Stream (int n1, int n2, int n3,
		   int n4, int n5, int n6, int n7, int n8, 
		   int n9, int n10, float f)
    {
        super (n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, f);
    }

    public Stream(float f)
    {
        super (f);
    }

    public Stream(String str)
    {
        super (str);
    }

    public Stream(ParameterContainer params)
    {
        super (params);
    }

    public Stream( int i1, 
		   int i2, 
		   int i3, 
		   int i4, 
		   int i5, 
		   int i6, 
		   int i7, 
		   int i8, 
		   int i9, 
		   float f) {
	super(i1, i2, i3, i4, i5, i6, i7, i8, i9, f);
    }

    public Stream( int i1, 
		   int i2, 
		   int i3, 
		   int i4, 
		   int i5, 
		   int i6, 
		   float f) {
	super(i1, i2, i3, i4, i5, i6, f);
    }

    public Stream(int n1, int n2, float f1[], float f2[])
    {
        super(n1, n2, f1, f2);
    }

    public Stream(short s1, short s2, short s3) {
	super(s1, s2, s3);
    }

    public Stream(int i1,int i2,int i3,float f1) {super(i1,i2,i3,f1);}

    public Stream(Object o1) {super(o1);}
    
    public Stream(Object o1,Object o2) {super(o1,o2);}

    public Stream(Object o1,Object o2,Object o3) {super(o1,o2,o3);}

    // RESET FUNCTIONS

    public MessageStub reset()
    {
        ASSERT (false);
        return MESSAGE_STUB;
    }

    public MessageStub reset(int n)
    {
        ASSERT (false);
        return MESSAGE_STUB;
    }

    public MessageStub reset(String str)
    {
        ASSERT (false);
        return MESSAGE_STUB;
    }

    // ------------------------------------------------------------------
    //                  graph handling functions
    // ------------------------------------------------------------------

    // tells me if this component has already been connected
    boolean isConnected = false;

    // adds something to the pipeline
    public void add(Stream s)
    {
        ASSERT (s != null);
        streamElements.add (s);
    }

    // get my input.
    // makes sure that I only have ONE input
    // return null if no input present
    Channel getIOField (String fieldName)
    {
        return getIOField (fieldName, 0);
    }

    void setIOField (String fieldName, Channel newChannel)
    {
        setIOField (fieldName, 0, newChannel);
    }

    Channel getOutputChannel ()
    {
        return output;
    }

    Channel getInputChannel ()
    {
        return input;
    }

    public abstract void connectGraph ();

    /**
     * Initialize the buffer lengths for a stream, given a schedule:
     */
    abstract void setupBufferLengths (Scheduler buffers);
}
