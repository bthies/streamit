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

import streamit.scheduler2.Scheduler;
import streamit.library.iriter.Iterator;
import java.util.List;

// the feedback loop
public class FeedbackLoop extends Stream
{
    Joiner joiner;
    Splitter splitter;
    SplitJoin.SplitJoinType splitType, joinType;
    List enqueued = new java.util.ArrayList();
    int delay;
    Stream body, loop;

    public FeedbackLoop(float a, float b, int c)
    {
        super(a, b, c);
    }

    public FeedbackLoop(int a, float b)
    {
        super(a, b);
    }

    public FeedbackLoop(float[] b)
    {
        super(b);
    }

    public FeedbackLoop(int a, float[] b)
    {
        super(a, b);
    }

    public FeedbackLoop(int a, int[] b)
    {
        super(a, b);
    }

    public FeedbackLoop(int a, float[][] b)
    {
        super(a, b);
    }

    public FeedbackLoop(int i1, int i2, float f) {
        super(i1, i2, f);
    }

    public FeedbackLoop(int i1, int i2, int i3, float[] f) {
        super(i1, i2, i3, f);
    }

    public FeedbackLoop(int i1, int i2, float f1, float f2) {
        super(i1, i2, f1, f2);
    }

    public FeedbackLoop(int a, int b, float[] c)
    {
        super(a, b, c);
    }

    public FeedbackLoop(int a, int b, float[][] c)
    {
        super(a, b, c);
    }

    public FeedbackLoop(int a, float[] c, float[] d) 
    { 
        super (a, c, d); 
    }

    public FeedbackLoop(int a, int b, int c, int d, float[][] e)
    {
        super(a, b, c, d, e);
    }

    public FeedbackLoop(int a, int b, int c, int d, float[][] e, float[][] f)
    {
        super(a, b, c, d, e, f);
    }

    public FeedbackLoop(int a, int b, int c, float[][] e, float[][] f)
    {
        super(a, b, c, e, f);
    }

    public FeedbackLoop(int a, int b, int c, int d, int e, float f)
    {
        super(a, b, c, d, e, f);
    }

    public FeedbackLoop(int a, int b, int c, int d, int e, int f, float g, float h)
    {
        super(a, b, c, d, e, f, g, h);
    }

    public FeedbackLoop(int a, int b, int c, int d, int e, int f, int g, float h, float i)
    {
        super(a, b, c, d, e, f, g, h, i);
    }

    public FeedbackLoop(float a, int b)
    {
        super(a, b);
    }

    public FeedbackLoop(float a, float b)
    {
        super(a, b);
    }

    public FeedbackLoop(float a, float b, float c)
    {
        super(a, b, c);
    }

    public FeedbackLoop(float a, float b, float c, float d)
    {
        super(a, b, c, d);
    }

    public FeedbackLoop(float a, float b, float c, float d, int e, int f)
    {
        super(a, b, c, d, e, f);
    }

    public FeedbackLoop(float a, float b, int c, int d)
    {
        super(a, b, c, d);
    }

    public FeedbackLoop(float x, float y, float z, int a, float b)
    {
        super(x,y,z,a,b);
    }

    public FeedbackLoop(int a, int b, int c, float d, int e)
    {
        super (a,b,c,d,e);
    }

    public FeedbackLoop(int a, int b, int c, float d, float e)
    {
        super (a,b,c,d,e);
    }

    public FeedbackLoop(float a, float b, float c, int d, int e, int f)
    {
        super (a,b,c,d,e,f);
    }

    public FeedbackLoop(float a, float b, int c, int d, int e, int f)
    {
        super (a,b,c,d,e,f);
    }

    public FeedbackLoop(float a, float b, int c, int d, int e, int f, int g)
    {
        super (a,b,c,d,e,f,g);
    }

    public FeedbackLoop(int a, int b, float c, int d, float e)
    {
        super (a,b,c,d,e);
    }

    public FeedbackLoop(float x, float y, float z, int a)
    {
        super(x,y,z,a);
    }

    public FeedbackLoop(float x, float y, float z, int a, int b)
    {
        super(x,y,z,a,b);
    }

    public FeedbackLoop(float x, float y, int a, int b, int c)
    {
        super(x,y,a,b,c);
    }

    public FeedbackLoop(float f1, int i1, float[] f2, float[] f3, int i2) {
        super(f1, i1, f2, f3, i2);
    }

    public FeedbackLoop()
    {
        super ();
    }

    public FeedbackLoop(char c)
    {
        super (c);
    }

    public FeedbackLoop(int n)
    {
        super (n);
    }

    public FeedbackLoop(boolean b1)
    {
        super(b1);
    }

    public FeedbackLoop(int n1, int n2, boolean b1)
    {
        super(n1, n2, b1);
    }

    public FeedbackLoop(int n1, boolean b1)
    {
        super(n1, b1);
    }

    public FeedbackLoop(int x, int y)
    {
        super (x, y);
    }

    public FeedbackLoop(int x, int y, int z)
    {
        super (x, y, z);
    }

    public FeedbackLoop(int x, int y, int z, float[][] f)
    {
        super (x, y, z, f);
    }

    public FeedbackLoop(int x, int y, int z, int a)
    {
        super (x, y, z, a);
    }

    public FeedbackLoop(int a, int b, int c, int d, int e) { super(a, b, c, d, e); }

    public FeedbackLoop(int a, int b, int c, int d, int e, int f, int g) 
    {
	super (a, b, c, d, e, f, g);
    }

    public FeedbackLoop(int n1, int n2, int n3,
		  int n4, float f1) {
      super(n1, n2, n3, n4, f1);
    }

    public FeedbackLoop(int x, int y, int z,
		   int a, int b, int c)
    {
        super (x, y, z, a, b, c);
    }

    public FeedbackLoop(int x, int y, int z,
		   int a, int b, int c, int d, float f)
    {
        super (x, y, z, a, b, c, d, f);
    }

    public FeedbackLoop(int n1, int n2, int n3,
		   int n4, int n5, int n6, int n7, int n8, 
		   int n9, int n10, float f)
    {
        super (n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, f);
    }

    public FeedbackLoop(int n1, int n2, int n3,
		   int n4, int n5, int n6, int n7, int n8, 
		   int n9)
    {
        super (n1, n2, n3, n4, n5, n6, n7, n8, n9);
    }

    public FeedbackLoop(float f)
    {
        super (f);
    }

    public FeedbackLoop(String str)
    {
        super (str);
    }

    public FeedbackLoop(ParameterContainer params)
    {
        super (params);
    }

    public FeedbackLoop( int i1, 
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

    public FeedbackLoop( int i1, 
		   int i2, 
		   int i3, 
		   int i4, 
		   int i5, 
		   int i6, 
		   float f) {
	super(i1, i2, i3, i4, i5, i6, f);
    }

    public FeedbackLoop(int n1, int n2, float f1[], float f2[])
    {
        super(n1, n2, f1, f2);
    }

    public FeedbackLoop(short s1, short s2, short s3) {
	super(s1, s2, s3);
    }

    public FeedbackLoop(int i1,int i2,int i3,float f1) {super(i1,i2,i3,f1);}

    public FeedbackLoop(Object o1) {super(o1);}
    public FeedbackLoop(Object o1, int i1) {super(o1, i1);}
    public FeedbackLoop(int i1, int i2, Object o1) {super(i1,i2,o1);}
    public FeedbackLoop(Object o1,Object o2) {super(o1,o2);}

    public FeedbackLoop(Object o1,Object o2,Object o3) {super(o1,o2,o3);}

    // set delay of feedback loop--that is, difference in original
    // stream position between items that arrive to joiner at same time,
    // assuming that all blocks in loop are 1-to-1 (otherwise we need to
    // work at defining exactly what it means).
    public void setDelay(int delay)
    {
        this.delay = delay;
    }
    
    public int getDelay() { return delay; }

    // specifies the header
    public void setJoiner(SplitJoin.SplitJoinType type)
    {
        assert joiner == null && type != null;
        joiner = type.getJoiner ();
	joinType = type;
    }

    // specifies the body of the feedback loop
    public void setBody (Stream body)
    {
        assert this.body == null && body != null;
        this.body = body;
    }

    // specifies the splitter
    public void setSplitter (SplitJoin.SplitJoinType type)
    {
        assert splitter == null && type != null;
        splitter = type.getSplitter ();
	splitType = type;
    }

    // specifies the feedback path stream
    public void setLoop (Stream loop)
    {
        assert this.loop == null && loop != null;
        this.loop = loop;
    }
    
    public Stream getBody () { return body; }
    public Stream getLoop () { return loop; }
    public Splitter getSplitter () { return splitter; }
    public Joiner getJoiner () { return joiner; }


    public Stream getChild(int nChild)
    {
        if (nChild == 0) return getBody ();
        if (nChild == 0) return getLoop ();
        
        ERROR ("FeedbackLoop only has two children!");
        
        return null;
    }
     
    /**
     * initialize the loop - push index-th element into the feedback loop channel.
     * these initPath functions are implemented for many different types that
     * one can use - if you're missing yours, implement it!
     */

    public int initPathInt (int index)
    {
        throw new UnsupportedOperationException();
    }

    public short initPathShort (int index)
    {
        throw new UnsupportedOperationException();
    }

    public float initPathFloat (int index)
    {
        throw new UnsupportedOperationException();
    }

    public char initPathChar (int index)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * initialize the path with an object.
     * If this isn't what you wanted (and it asserts here), then you
     * need to add an appropriate if-else in connectGraph (there is a comment
     * about it in the appropriate place)
     */
    public Object initPathObject (int index)
    {
        throw new UnsupportedOperationException();
    }

    /*
     * alternate loop initialization -- push individual items on to
     * the output channel of the loop stream at init time.
     */

    public void enqueueInt (int value)
    {
        enqueued.add(new Integer(value));
    }
    
    public void enqueueShort (short value)
    {
        enqueued.add(new Short(value));
    }
    
    public void enqueueFloat (float value)
    {
        enqueued.add(new Float(value));
    }

    public void enqueueChar (char value)
    {
        enqueued.add(new Character(value));
    }

    public void enqueueObject (Object value)
    {
        enqueued.add(value);
    }

    // not used here!
    public void add(Stream s)
    {
        throw new UnsupportedOperationException();
    }

    // connect all the elements of this FeedbackLoop
    public void connectGraph ()
    {
        // make sure that I have the minimal elements to construct this loop
        assert joiner != null;
        assert splitter != null;
        assert body != null;

        // okay, initialize the body and figure out the type of data
        // passed around this loop
        body.setupOperator();
        Channel bodyInput = body.getInputChannel ();
        Channel bodyOutput = body.getOutputChannel ();
        assert bodyInput != null;
        assert bodyOutput != null;
        assert bodyOutput.getType ().getName ()
            .equals(bodyInput.getType ().getName ());

        // if I don't have a feedback path, just give myself an identity body
        // and initialize whatever the feedback path is
        if (loop == null) loop = new Identity (bodyOutput.getType ());
        loop.setupOperator ();
        Channel loopInput = loop.getInputChannel ();
        Channel loopOutput = loop.getOutputChannel ();
        assert loopInput != null;
        assert loopOutput != null;
        assert loopOutput.getType ().getName ()
            .equals(loopInput.getType ().getName ());

        // create some extra Identities and give them to the split
        // and the join so they have a Filter for every input/output
        // they have to deal with
        {
            // the joiner:
            Channel channelIn = null;
            if (joiner.isInputUsed (0))
            {
                Filter joinerIn = new Identity (bodyInput.getType ());
                joinerIn.setupOperator ();
                joiner.add (joinerIn);
                channelIn = joinerIn.getIOField ("input");
            } else {
                joiner.add (null);
            }

            joiner.add (loop);
            joiner.setupOperator ();

            {
                ChannelConnectFilter connect = new ChannelConnectFilter ();
                Channel in = joiner.getIOField ("output", 0);
                Channel out = body.getInputChannel ();
                connect.useChannels (in, out);
            }

            // the splitter:
            Channel channelOut = null;
            if (splitter.isOutputUsed (0))
            {
                Filter splitterOut = new Identity (bodyOutput.getType ());
                splitterOut.setupOperator ();
                splitter.add (splitterOut);
                channelOut = splitterOut.getIOField ("output");
            } else {
                splitter.add (null);
            }
            splitter.add (loop);
            splitter.setupOperator ();

            {
                ChannelConnectFilter connect = new ChannelConnectFilter ();
                Channel in = body.getOutputChannel ();
                Channel out = splitter.getIOField ("input", 0);
                connect.useChannels (in, out);
            }

            // copy the input/output from the identities to the input/output
            // fields of the feedback loop
            input = channelIn;
            output = channelOut;
        }

        // now fill up the feedback path with precomputed data:
        assert delay == 0 || enqueued.isEmpty();
        if (delay != 0)
        {
            Channel feedbackChannel = loop.getOutputChannel ();
            for (int index = 0; index < delay; index++)
            {
                Class type = feedbackChannel.getType ();
                if (type == Integer.TYPE)
                {
                    feedbackChannel.pushInt (initPathInt (index));
                } else
                if (type == Short.TYPE)
                {
                    feedbackChannel.pushShort (initPathShort (index));
                } else
                if (type == Character.TYPE)
                {
                    feedbackChannel.pushChar (initPathChar (index));
                } else
                if (type == Float.TYPE)
                {
                    feedbackChannel.pushFloat (initPathFloat (index));
                } else
                {
                    // this is essentially a default
                    // if this isn't what you want, you should implement
                    // another if-else clause!
                    feedbackChannel.push (initPathObject (index));
                }
            }
        }
        else
        {
            Channel feedbackChannel = loop.getOutputChannel ();
            for (java.util.Iterator iter = enqueued.iterator();
                 iter.hasNext(); )
            {
                Class type = feedbackChannel.getType ();
                if (type == Integer.TYPE)
                {
                    Integer value = (Integer)iter.next();
                    feedbackChannel.pushInt (value.intValue());
                } else
                if (type == Short.TYPE)
                {
                    Short value = (Short)iter.next();
                    feedbackChannel.pushShort (value.shortValue());
                } else
                if (type == Character.TYPE)
                {
                    Character value = (Character)iter.next();
                    feedbackChannel.pushChar (value.charValue());
                } else
                if (type == Float.TYPE)
                {
                    Float value = (Float)iter.next();
                    feedbackChannel.pushFloat (value.floatValue());
                } else
                {
                    // this is essentially a default
                    // if this isn't what you want, you should implement
                    // another if-else clause!
                    feedbackChannel.push (iter.next());
                }
                delay++;
            }
            // Throw out the enqueued list now.
            enqueued = null;
        }
    }


    // this is silly, but I need to put these here to make them easily
    // accessible - they're just wrappers for SplitJoin
    public static SplitJoin.SplitJoinType WEIGHTED_ROUND_ROBIN(int w1)
    {
        return new SplitJoin.SplitJoinType(2).addWeight(w1);
    }

    public static SplitJoin.SplitJoinType WEIGHTED_ROUND_ROBIN(int w1, int w2)
    {
        return new SplitJoin.SplitJoinType(2).addWeight(w1).addWeight(w2);
    }

    public static SplitJoin.SplitJoinType WEIGHTED_ROUND_ROBIN(int w1, int w2, int w3)
    {
        return new SplitJoin.SplitJoinType(2).addWeight(w1).addWeight(w2).addWeight(w3);
    }

    public static SplitJoin.SplitJoinType WEIGHTED_ROUND_ROBIN(
        int w1,
        int w2,
        int w3,
        int w4)
    {
        return new SplitJoin.SplitJoinType(2)
            .addWeight(w1)
            .addWeight(w2)
            .addWeight(w3)
            .addWeight(w4);
    }

    public static SplitJoin.SplitJoinType WEIGHTED_ROUND_ROBIN(
        int w1,
        int w2,
        int w3,
        int w4,
	int w5)
    {
        return new SplitJoin.SplitJoinType(2)
            .addWeight(w1)
            .addWeight(w2)
            .addWeight(w3)
            .addWeight(w4)
            .addWeight(w5);
    }

    public static SplitJoin.SplitJoinType WEIGHTED_ROUND_ROBIN(
        int w1,
        int w2,
        int w3,
        int w4,
        int w5,
        int w6,
        int w7)
    {
        return new SplitJoin.SplitJoinType(2)
            .addWeight(w1)
            .addWeight(w2)
            .addWeight(w3)
            .addWeight(w4)
            .addWeight(w5)
            .addWeight(w6)
            .addWeight(w7);
    }

    public static SplitJoin.SplitJoinType WEIGHTED_ROUND_ROBIN(
        int w1,
        int w2,
        int w3,
        int w4,
        int w5,
        int w6,
        int w7,
        int w8,
        int w9,
        int w10,
        int w11,
        int w12)
    {
        return new SplitJoin.SplitJoinType(2)
            .addWeight(w1)
            .addWeight(w2)
            .addWeight(w3)
            .addWeight(w4)
            .addWeight(w5)
            .addWeight(w6)
            .addWeight(w7)
            .addWeight(w8)
            .addWeight(w9)
            .addWeight(w10)
            .addWeight(w11)
            .addWeight(w12);
    }

    public static SplitJoin.SplitJoinType WEIGHTED_ROUND_ROBIN(
        int w1,
        int w2,
        int w3,
        int w4,
        int w5,
        int w6,
        int w7,
        int w8,
        int w9,
        int w10,
        int w11,
        int w12,
        int w13,
        int w14,
        int w15,
        int w16,
        int w17,
        int w18)
    {
        return new SplitJoin.SplitJoinType(2)
            .addWeight(w1)
            .addWeight(w2)
            .addWeight(w3)
            .addWeight(w4)
            .addWeight(w5)
            .addWeight(w6)
            .addWeight(w7)
            .addWeight(w8)
            .addWeight(w9)
            .addWeight(w10)
            .addWeight(w11)
            .addWeight(w12)
            .addWeight(w13)
            .addWeight(w14)
            .addWeight(w15)
            .addWeight(w16)
            .addWeight(w17)
            .addWeight(w18);
    }

    public static SplitJoin.SplitJoinType ROUND_ROBIN()
    {
        return new SplitJoin.SplitJoinType(1).addWeight(1);
    }

    public static SplitJoin.SplitJoinType ROUND_ROBIN(int weight)
    {
        return new SplitJoin.SplitJoinType(1).addWeight(weight);
    }

    public static SplitJoin.SplitJoinType DUPLICATE()
    {
        return new SplitJoin.SplitJoinType(3);
    }

    public static SplitJoin.SplitJoinType NULL()
    {
        return new SplitJoin.SplitJoinType(4);
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    void setupBufferLengths (Scheduler buffers)
    {
        // some of these asserts are restrictions on functionality
        // of this function and not on what a correct streamit structure would
        // be
        assert body != null;
        assert loop != null;
        assert joiner != null;
        assert splitter != null;

        // just go through and init the bloody buffers

        // start with getting body and loop to initialize their interal buffers
        body.setupBufferLengths (buffers);
        loop.setupBufferLengths (buffers);

        // now setup the buffer sizes:
        int s;

        // between joiner and body
        s = buffers.getBufferSizeBetween (new Iterator(this), new Iterator(body));
        StreamIt.totalBuffer += s;
        joiner.getIOField ("output", 0).makePassThrough ();
        body.getInputChannel ().setChannelSize (s);

        // between body and splitter
        s = buffers.getBufferSizeBetween (new Iterator(body), new Iterator(this));
        StreamIt.totalBuffer += s;
        splitter.getIOField ("input", 0).setChannelSize (s);
        body.getOutputChannel ().makePassThrough ();

        // between splitter and loop
        s = buffers.getBufferSizeBetween (new Iterator(this), new Iterator(loop));
        StreamIt.totalBuffer += s;
        loop.getInputChannel ().setChannelSize (s);

        // between loop and joiner
        s = buffers.getBufferSizeBetween (new Iterator(loop), new Iterator(this));
        StreamIt.totalBuffer += s;
        loop.getOutputChannel ().setChannelSize (s);

        // make sure that the input/output channels push data through right away:
        if (getInputChannel () != null) getInputChannel ().makePassThrough ();
        if (getOutputChannel () != null) splitter.getIOField ("output", 0).makePassThrough ();
    }
}

