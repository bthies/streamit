/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.library;

import java.util.*;

import streamit.scheduler2.Scheduler;
import streamit.library.iriter.Iterator;

/**
 *  implements a pipeline - stream already has all the functionality,
 *  so there's no need to put it in Pipeline - just inherit Stream :)
 */

public class Pipeline extends Stream
{
    /**
     * Default constructor
     */
    public Pipeline () { super(); }

    public Pipeline(float a, float b, int c)
    {
        super(a, b, c);
    }

    public Pipeline(int a, float b)
    {
        super(a, b);
    }

    public Pipeline(float[] b)
    {
        super(b);
    }

    public Pipeline(int a, float[] b)
    {
        super(a, b);
    }

    public Pipeline(int a, int[] b)
    {
        super(a, b);
    }

    public Pipeline(int a, float[][] b)
    {
        super(a, b);
    }

    public Pipeline(int i1, int i2, float f) {
        super(i1, i2, f);
    }

    public Pipeline(int i1, int i2, int i3, float[] f) {
        super(i1, i2, i3, f);
    }

    public Pipeline(int i1, int i2, float f1, float f2) {
        super(i1, i2, f1, f2);
    }

    public Pipeline(int a, int b, float[] c)
    {
        super(a, b, c);
    }

    public Pipeline(int a, int b, float[][] c)
    {
        super(a, b, c);
    }

    public Pipeline(int a, float[] c, float[] d) 
    { 
        super (a, c, d); 
    }

    public Pipeline(int a, int b, int c, int d, float[][] e)
    {
        super(a, b, c, d, e);
    }

    public Pipeline(int a, int b, int c, int d, float[][] e, float[][] f)
    {
        super(a, b, c, d, e, f);
    }

    public Pipeline(int a, int b, int c, float[][] e, float[][] f)
    {
        super(a, b, c, e, f);
    }

    public Pipeline(int a, int b, float[][] c, float[] d) {
	super(a,b,c,d);
    }

    public Pipeline(int a, int b, int c, float[][] d, float[] e) {
	super(a,b,c,d,e);
    }

    public Pipeline(int a, boolean b, float c, float d, float[][] e, float[] f) {
	super(a,b,c,d,e,f);
    }

    public Pipeline(int a, int b, int c, int d, int e, float f)
    {
        super(a, b, c, d, e, f);
    }

    public Pipeline(int a, int b, int c, int d, int e, int f, float g, float h)
    {
        super(a, b, c, d, e, f, g, h);
    }

    public Pipeline(int a, int b, int c, int d, int e, int f, int g, float h, float i)
    {
        super(a, b, c, d, e, f, g, h, i);
    }

    public Pipeline(float a, int b)
    {
        super(a, b);
    }

    public Pipeline(float a, float b)
    {
        super(a, b);
    }

    public Pipeline(float a, float b, float c)
    {
        super(a, b, c);
    }

    public Pipeline(float a, float b, float c, float d)
    {
        super(a, b, c, d);
    }

    public Pipeline(float a, float b, float c, float d, int e, int f)
    {
        super(a, b, c, d, e, f);
    }

    public Pipeline(float a, float b, int c, int d)
    {
        super(a, b, c, d);
    }

    public Pipeline(float x, float y, float z, int a, float b)
    {
        super(x,y,z,a,b);
    }

    public Pipeline(int a, int b, int c, float d, int e)
    {
        super (a,b,c,d,e);
    }

    public Pipeline(int a, int b, int c, float d, float e)
    {
        super (a,b,c,d,e);
    }

    public Pipeline(float a, float b, float c, int d, int e, int f)
    {
        super (a,b,c,d,e,f);
    }

    public Pipeline(float a, float b, int c, int d, int e, int f)
    {
        super (a,b,c,d,e,f);
    }

    public Pipeline(float a, float b, int c, int d, int e, int f, int g)
    {
        super (a,b,c,d,e,f,g);
    }

    public Pipeline(int a, int b, float c, int d, float e)
    {
        super (a,b,c,d,e);
    }

    public Pipeline(float x, float y, float z, int a)
    {
        super(x,y,z,a);
    }

    public Pipeline(float x, float y, float z, int a, int b)
    {
        super(x,y,z,a,b);
    }

    public Pipeline(float x, float y, int a, int b, int c)
    {
        super(x,y,a,b,c);
    }

    public Pipeline(float f1, int i1, float[] f2, float[] f3, int i2) {
        super(f1, i1, f2, f3, i2);
    }

    public Pipeline(char c)
    {
        super (c);
    }

    public Pipeline(int n)
    {
        super (n);
    }

    public Pipeline(boolean b1)
    {
        super(b1);
    }

    public Pipeline(int n1, int n2, boolean b1)
    {
        super(n1, n2, b1);
    }

    public Pipeline(int n1, boolean b1)
    {
        super(n1, b1);
    }

    public Pipeline(int x, int y)
    {
        super (x, y);
    }

    public Pipeline(int x, int y, int z)
    {
        super (x, y, z);
    }

    public Pipeline(int x, int y, int z, float[][] f)
    {
        super (x, y, z, f);
    }

    public Pipeline(int x, int y, int z, int a)
    {
        super (x, y, z, a);
    }

    public Pipeline(int a, int b, int c, int d, int e) { super(a, b, c, d, e); }

    public Pipeline(int a, int b, int c, int d, int e, int f, int g) 
    {
	super (a, b, c, d, e, f, g);
    }

    public Pipeline(int n1, int n2, int n3,
		  int n4, float f1) {
      super(n1, n2, n3, n4, f1);
    }

    public Pipeline(int x, int y, int z,
		   int a, int b, int c)
    {
        super (x, y, z, a, b, c);
    }

    public Pipeline(int x, int y, int z,
		   int a, int b, int c, int d, float f)
    {
        super (x, y, z, a, b, c, d, f);
    }

    public Pipeline(int n1, int n2, int n3,
		   int n4, int n5, int n6, int n7, int n8, 
		   int n9, int n10, float f)
    {
        super (n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, f);
    }

    public Pipeline(int n1, int n2, int n3,
		   int n4, int n5, int n6, int n7, int n8, 
		   int n9)
    {
        super (n1, n2, n3, n4, n5, n6, n7, n8, n9);
    }

    public Pipeline(float f)
    {
        super (f);
    }

    public Pipeline(String str)
    {
        super (str);
    }

    public Pipeline(ParameterContainer params)
    {
        super (params);
    }

    public Pipeline( int i1, 
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

    public Pipeline( int i1, 
		   int i2, 
		   int i3, 
		   int i4, 
		   int i5, 
		   int i6, 
		   float f) {
	super(i1, i2, i3, i4, i5, i6, f);
    }

    public Pipeline(int n1, int n2, float f1[], float f2[])
    {
        super(n1, n2, f1, f2);
    }

    public Pipeline(short s1, short s2, short s3) {
	super(s1, s2, s3);
    }

    public Pipeline(int i1,int i2,int i3,float f1) {super(i1,i2,i3,f1);}

    public Pipeline(Object o1) {super(o1);}
    public Pipeline(Object o1, int i1) {super(o1, i1);}
    public Pipeline(int i1, int i2, Object o1) {super(i1,i2,o1);}
    public Pipeline(Object o1,Object o2) {super(o1,o2);}

    public Pipeline(Object o1,Object o2,Object o3) {super(o1,o2,o3);}

     // allow access to the children of this pipeline
     
    public int getNumChildren () { return streamElements.size (); }
    public Stream getChildN (int n) { return (Stream) streamElements.get (n); }
    
    public Stream getChild(int nChild)
    {
        return getChildN (nChild);
    }
     
    // connectGraph will walk the entire subgraph (so it should be called
    // on the "master", encapsulating Stream) and give each element
    // this function works in the following way:
    //

    // goal:
    // Channels need to connect TWO separate Operators
    // 1. try to assign the same channel to both operators
    // 2. can be done by first comparing the two separate operators
    //    for matching types (error check) and then copying one of the
    //    channels over to the other operator
    // 3. should copy over the source's operator (overwrite the dest
    //    operator's channel).  reason for this is that the source's
    //    operator should know the REAL source of data (particular
    //    Filer which is producing this data)
    // 4. this operation is done in-order:
    //    - an Operator gets a proper value for its input
    //    - this Operator processes all its children
    //    - the output from the last child is copied over
    //      to the Operator and the operation is finished

    public void connectGraph ()
    {
        // make sure I have some elements - not sure what to do otherwise
        assert !streamElements.isEmpty ();

        // go through the list and connect it together:
        ListIterator childIter;
        childIter = (ListIterator) streamElements.iterator ();
        Stream source = null;

        while (childIter.hasNext ())
        {
            // advance the iterator:
            Stream sink = (Stream) childIter.next ();
            assert sink != null;

            // setup the sink itself
            sink.setupOperator ();

            if (source != null && source.getOutputChannel () != null)
            {
                // create and connect a pass filter
                ChannelConnectFilter connect = new ChannelConnectFilter ();
                Channel in = source.getOutputChannel ();
                Channel out = sink.getInputChannel ();
                connect.useChannels (in, out);
            }
            source = sink;
        }

        // set myself up with proper input and output
        {
            input = ((Stream)streamElements.getFirst ()).getInputChannel ();
            output = ((Stream)streamElements.getLast ()).getOutputChannel ();
        }
    }

    void setupBufferLengths (Scheduler buffers)
    {
        ListIterator childIter;
        childIter = (ListIterator) streamElements.iterator ();
        Stream source = null;
        Stream sink = null;

        // go through all the children
        while (childIter.hasNext ())
        {
            // advance the iterator:
            Stream child = (Stream) childIter.next ();
            assert child != null;
            child.setupBufferLengths (buffers);

            source = sink;
            sink = child;

            if (source != null)
            {
                assert sink != null;

                int buffSize = buffers.getBufferSizeBetween (new Iterator(source), new Iterator(sink));
                assert buffSize != 0;

                StreamIt.totalBuffer += buffSize;
                
                source.getOutputChannel ().makePassThrough ();
                sink.getInputChannel ().setChannelSize (buffSize);
            }
        }
    }
}
