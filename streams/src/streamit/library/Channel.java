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

// a channel is an I/O FIFO queue to go between filters

// A number of modifications have been made to this class to make it
// use WrappableGrowableArrays for passing basic data types between
// components instead of the linked list implementation. The linked
// list implementation is still part of the file and can be used for
// objects or other special cases, but the primitive types - int, short
// double, float, char, boolean, and bit, will work much faster with the
// new objects. - Matthew Drake (madrake@gmail.com)

package streamit.library;

import streamit.misc.*;

public class Channel extends streamit.misc.DestroyedClass
{
    protected Class type;
    Operator source = null, sink = null;

    Integer popPushCount = null;
    Integer peekCount = null;

    int maxSize = -1;
    boolean passThrough = false;
    
    int totalItemsPushed = 0, totalItemsPopped = 0;

    protected WrappableGrowableQueue_int wgqueue_int, wgqueue_bit;
    protected WrappableGrowableQueue_float wgqueue_float;
    protected WrappableGrowableQueue_double wgqueue_double;
    protected WrappableGrowableQueue_boolean wgqueue_boolean;
    protected WrappableGrowableQueue_short wgqueue_short;
    protected WrappableGrowableQueue_char wgqueue_char;
    protected WrappableGrowableQueue_obj wgqueue_obj;

    protected WrappableGrowableQueue myqueue;

    // the channel should be constructed with a 0-length array
    // indicating the type that will be held in this channel.
    void setupChannel (Class channelType)
    {
        assert channelType != null;
        type = channelType;
        if (type == Integer.TYPE) {
            wgqueue_int =  new WrappableGrowableQueue_int();
            myqueue = wgqueue_int;
        } else if (type == Float.TYPE) {
            wgqueue_float = new WrappableGrowableQueue_float(); 
            myqueue = wgqueue_float;
        } else if (type == Double.TYPE) {
            wgqueue_double = new WrappableGrowableQueue_double();
            myqueue = wgqueue_double;
        } else if (type == Boolean.TYPE) {
            wgqueue_boolean = new WrappableGrowableQueue_boolean();
            myqueue = wgqueue_boolean;
        } else if (type == Short.TYPE) {
            wgqueue_short = new WrappableGrowableQueue_short();
            myqueue = wgqueue_short;
        } else if (type == Character.TYPE) {
            wgqueue_char = new WrappableGrowableQueue_char();
            myqueue = wgqueue_char;
        } else if (type == Bit.TYPE) {
            wgqueue_bit = new WrappableGrowableQueue_int();
            myqueue = wgqueue_bit;
        } else {
            wgqueue_obj = new WrappableGrowableQueue_obj();
            myqueue = wgqueue_obj;
        }
    }
    
    public Channel(Class channelType)
    {
        setupChannel (channelType);
    }

    public Channel (Class channelType, Rate popPush)
    {
        setupChannel (channelType);

        // if we detect a dynamic rate, make sure we're in unscheduled
        // mode
        if (!popPush.isStatic()) {
            Stream.ensureUnscheduled();
        }

        // store the popPush (use the maximum in case it's dynamic)
        popPushCount = new Integer (popPush.max);
    }
    /**
     * Same as above, for fixed I/O rates (and backwards compatibility
     * with old Java benchmarks)
     */
    public Channel (Class channelType, int popPush) {
        this(channelType, new RateStatic(popPush));
    }

    public Channel (Class channelType, Rate pop, Rate peek)
    {
        setupChannel (channelType);

        // if we detect a dynamic rate, make sure we're in unscheduled
        // mode
        if (!pop.isStatic() || !peek.isStatic()) {
            Stream.ensureUnscheduled();
        }

        // store the popPush (use the maximum in case it's dynamic)
        popPushCount = new Integer (pop.max);
        peekCount = new Integer (peek.max);
    }
    /**
     * Same as above, for fixed I/O rates (and backwards compatibility
     * with old Java benchmarks)
     */
    public Channel (Class channelType, int pop, int peek) {
        this(channelType, new RateStatic(pop), new RateStatic(peek));
    }
    public Channel (Class channelType, Rate pop, int peek) {
        this(channelType, pop, new RateStatic(peek));
    }
    public Channel (Class channelType, int pop, Rate peek) {
        this(channelType, new RateStatic(pop), peek);
    }

    public Channel (Channel original)
    {
        assert original != null;

        type = original.getType ();

        if (type == Integer.TYPE) {
            wgqueue_int = new WrappableGrowableQueue_int();
            myqueue = wgqueue_int;
        } else if (type == Float.TYPE) {
            wgqueue_float = new WrappableGrowableQueue_float(); 
            myqueue = wgqueue_float;
        } else if (type == Double.TYPE) {
            wgqueue_double = new WrappableGrowableQueue_double();
            myqueue = wgqueue_double;
        } else if (type == Boolean.TYPE) {
            wgqueue_boolean = new WrappableGrowableQueue_boolean();
            myqueue = wgqueue_boolean;
        } else if (type == Short.TYPE) {
            wgqueue_short = new WrappableGrowableQueue_short();
            myqueue = wgqueue_short;
        } else if (type == Character.TYPE) {
            wgqueue_char = new WrappableGrowableQueue_char();
            myqueue = wgqueue_char;
        } else if (type == Bit.TYPE) {
            wgqueue_bit = new WrappableGrowableQueue_int();
            myqueue = wgqueue_bit;
        } else {
            wgqueue_obj = new WrappableGrowableQueue_obj();
            myqueue = wgqueue_obj;
        }

        // copy pop/push/peek values
        popPushCount = original.popPushCount;
        peekCount = original.peekCount;
    }

    void ensureData (int amount)
    {
        boolean tempval = myqueue.size() < amount;

        while (tempval)
            {
                assert source != null;

                if (maxSize!=-1) {
                    // give a better error message
                    ERROR("ERROR:\n" + 
                          "Trying to pop or peek beyond the declared rate in stream " + sink + "\n" +
                          "Make sure that your pop/peek statements match the declared rates.\n" +
                          "\n" +
                          "Internal message:\n" +
                          "maxSize should equal -1 " +
                          "(representing not a scheduled buffer)\n" +
                          "Queue: " + myqueue +
                          ".size: " + myqueue.size() +
                          " amount is: " + amount +
                          " and maxSize is: " + maxSize);
                }

                source.doWork();
                tempval = myqueue.size() < amount;
            }
    }

    void ensureData ()
    {
        ensureData (1);
    }

    private void enqueue (int i) {
        wgqueue_int.enqueue(i);
        totalItemsPushed++;
        source.registerPush();
        if (passThrough) {
            sink.doWork();
        }        
    }

    private void enqueue_bit (int i) {
        wgqueue_bit.enqueue(i);
        totalItemsPushed++;
        source.registerPush();
        if (passThrough) {
            sink.doWork();
        }
    }

    private void enqueue (float i) {
        wgqueue_float.enqueue(i);
        totalItemsPushed++;
        source.registerPush();
        if (passThrough) {
            sink.doWork();
        }                
    }

    private void enqueue(double i) {
        wgqueue_double.enqueue(i);
        totalItemsPushed++;
        source.registerPush();
        if (passThrough) {
            sink.doWork();
        }
    }

    private void enqueue(boolean i) {
        wgqueue_boolean.enqueue(i);
        totalItemsPushed++;
        source.registerPush();
        if (passThrough) {
            sink.doWork();
        }
    }

    private void enqueue(short i) {
        wgqueue_short.enqueue(i);
        totalItemsPushed++;
        source.registerPush();
        if (passThrough) {
            sink.doWork();
        }
    }

    private void enqueue(char i) {
        wgqueue_char.enqueue(i);
        totalItemsPushed++;
        source.registerPush();
        if (passThrough) {
            sink.doWork();
        }
    }     

    private void enqueue (Object o)
    {
        wgqueue_obj.enqueue(o);
        totalItemsPushed++;
        source.registerPush();
        if (passThrough) {
            sink.doWork();
        }
    }

    private int dequeue_int () {
        totalItemsPopped++;
        sink.registerPop();
        return wgqueue_int.dequeue();
    }

    private float dequeue_float () {
        totalItemsPopped++;
        sink.registerPop();
        return wgqueue_float.dequeue();
    }

    private double dequeue_double () {
        totalItemsPopped++;
        sink.registerPop();
        return wgqueue_double.dequeue();
    }

    private boolean dequeue_boolean () {
        totalItemsPopped++;
        sink.registerPop();
        return wgqueue_boolean.dequeue();
    }

    private short dequeue_short () {
        totalItemsPopped++;
        sink.registerPop();
        return wgqueue_short.dequeue();
    }

    private char dequeue_char () {
        totalItemsPopped++;
        sink.registerPop();
        return wgqueue_char.dequeue();
    }

    private int dequeue_bit () {
        totalItemsPopped++;
        sink.registerPop();
        return wgqueue_bit.dequeue();
    }

    private Object dequeue () {
        totalItemsPopped++;
        sink.registerPop();
        return wgqueue_obj.dequeue();
    }


    // PUSH OPERATIONS ----------------------------------------------
    
    public int getItemsPushed () { return totalItemsPushed; }

    // push something of type <pre>type</pre>
    public void push(Object o)
    {
        assert o.getClass () == type;

        // make a copy of structures in case they are subsequently
        // modified -- the changes should not appear in the consumer
        if (o instanceof Structure) {
            o = (Structure)Cloner.doCopy(o);
        }
        
        enqueue (o);
    }
    
    // push a boolean
    public void pushBool (boolean b)
    {
        assert type == Boolean.TYPE;
        enqueue(b);
    }

    // push an int
    public void pushInt(int i)
    {
        assert type == Integer.TYPE;
        enqueue(i);
    }

    // push a short
    public void pushShort(short s)
    {
        assert type == Short.TYPE;
        enqueue(s);
    }
    //push a bit
    public void pushBit(int i)
    {
        assert type == Bit.TYPE;
        enqueue_bit(i);
    }

    // push a char
    public void pushChar(char c)
    {
        assert type == Character.TYPE;
        enqueue(c);
    }

    // push a double
    public void pushDouble(double d)
    {
        assert type == Double.TYPE;
        enqueue(d);
    }

    // push a float
    public void pushFloat(float d)
    {
        assert type == Float.TYPE;
        enqueue(d);
    }

    // push a 2-d float array
    public void push2DFloat(float[][] d)
    {
        assert type == new float[0][0].getClass();
    
        // copy the array to maintain copy-semantics
        float[][] copy = new float[d.length][d[0].length];
        for (int i=0; i<d.length; i++) {
            for (int j=0; j<d[0].length; j++) {
                copy[i][j] = d[i][j];
            }
        }

        enqueue (copy);
    }

    // push a String
    public void pushString(String str)
    {
        push (str);
    }

    // POP OPERATIONS ----------------------------------------------

    public int getItemsPopped () { return totalItemsPopped; }

    // pop something of type <pre>type</pre>
    public Object pop()
    {
        ensureData ();

        Object data;
        data = dequeue ();
        assert data != null;

        return data;
    }
    
    // pop a boolean
    public boolean popBool ()
    {
        assert type == Boolean.TYPE;
        ensureData();
        return dequeue_boolean();
    }

    // pop an int
    public int popInt()
    {
        assert type == Integer.TYPE;
        ensureData ();
        return dequeue_int();
    }

    // pop a short
    public short popShort()
    {
        assert type == Short.TYPE;
        ensureData();
        return dequeue_short();
    }

    // pop a char
    public char popChar()
    {
        assert type == Character.TYPE;
        ensureData();
        return dequeue_char();
    }

    //pop a bit
    public int popBit(){
        assert type == Bit.TYPE;
        ensureData();
        return dequeue_bit();
    }

    // pop a double
    public double popDouble()
    {
        assert type == Double.TYPE;
        ensureData ();
        return dequeue_double();
    }

    // pop a float
    public float popFloat()
    {
        assert type == Float.TYPE;
        ensureData ();
        return dequeue_float();
    }

    // pop a float
    public float[][] pop2DFloat()
    {
        assert type == new float[0][0].getClass();

        float[][] data = (float[][]) pop ();
        assert data != null;

        return data;
    }


    // pop a String
    public String popString()
    {
        String data = (String) pop ();;
        assert data != null;

        return data;
    }

    // PEEK OPERATIONS ----------------------------------------------



    // peek at something of type <pre>type</pre>
    public Object peek(int index)
    {
        sink.registerPeek(index);
        ensureData (index + 1);

        if (type == Boolean.TYPE) {
            return new Boolean(wgqueue_boolean.elem(index));
        } else {

            Object data;
            data = wgqueue_obj.elem(index);
            assert data != null;
            
            // make a copy of structures in case they are subsequently
            // modified -- the changes should not appear in the producer
            // or in subsequent peek / pop's
            if (data instanceof Structure) {
                data = (Structure)Cloner.doCopy(data);
            }

            return data;
        }
    }

    // peek at an int
    public int peekInt(int index)
    {
        assert type == Integer.TYPE;   
        ensureData (index + 1);
        sink.registerPeek(index);
        return wgqueue_int.elem(index);

    }

    //peek a bit
    public int peekBit(int index){
        assert type == Bit.TYPE;
        ensureData(index + 1);
        sink.registerPeek(index);
        return wgqueue_bit.elem(index);
    }


    // peek at a short
    public short peekShort (int index)
    {
        assert type == Short.TYPE;
        ensureData(index+1);
        sink.registerPeek(index);
        return wgqueue_short.elem(index);
    }

    // peek at a char
    public char peekChar(int index)
    {
        assert type == Character.TYPE;
        ensureData(index+1);
        sink.registerPeek(index);
        return wgqueue_char.elem(index);
    }

    // peek at a double
    public double peekDouble(int index)
    {
        assert type == Double.TYPE;
        ensureData (index + 1);
        sink.registerPeek(index);
        return wgqueue_double.elem(index);
    }

    // peek at a float
    public float peekFloat(int index)
    {
        assert type == Float.TYPE;
        ensureData (index + 1);
        sink.registerPeek(index);
        return wgqueue_float.elem(index);    
    }

    // peek at a float
    public float[][] peek2DFloat(int index)
    {
        assert type == new float[0][0].getClass();

        float[][] data = (float[][]) peek (index);
        assert data != null;

        return data;
    }

    // peek at a String
    public String peekString(int index)
    {
        String data;
        data = (String) peek (index);
        assert data != null;

        return data;
    }

    /**
     * Get the number amount of data popped from this channel
     * on every iteration
     */

    public int getPopCount ()
    {
        assert popPushCount != null;

        return popPushCount.intValue ();
    }

    /**
     * Get the number amount of data peeked from this channel
     * on every iteration.
     * If the peek amount hasn't been set, make it the pop count
     * If the pop count is smaller than the peek amount, assert
     */

    public int getPeekCount ()
    {
        if (peekCount != null)
            {
                assert peekCount.intValue () >= popPushCount.intValue ():
                    "The peek count of " + peekCount.intValue() +
                    " is less than the pop count of " + popPushCount.intValue() +
                    " in channel connecting " + source + " and " + sink;
                return peekCount.intValue ();
            } else {
                return getPopCount ();
            }
    }

    /**
     * Get the number amount of data pushed from this channel
     * on every iteration
     */

    public int getPushCount ()
    {
        assert popPushCount != null;

        return popPushCount.intValue ();
    }

    // ------------------------------------------------------------------
    //                  syntax checking functions
    // ------------------------------------------------------------------

    public Class getType () { return type; }

    // ------------------------------------------------------------------
    //                  graph keeping functions
    // ------------------------------------------------------------------

    Operator getSource () { return source; }
    Operator getSink () { return sink; }

    void setSource (Operator _source) { source = _source; }
    void setSink (Operator _sink) { sink = _sink; }

    void setChannelSize (int size)
    {
        assert size > 0;
        maxSize = size;
        if (type == Integer.TYPE) {
            wgqueue_int.setBufferSize(size);
        } else if (type == Float.TYPE) {
            wgqueue_float.setBufferSize(size);
        } else if (type == Double.TYPE) {
            wgqueue_double.setBufferSize(size);
        } else if (type == Boolean.TYPE) {
            wgqueue_boolean.setBufferSize(size);
        } else if (type == Short.TYPE) {
            wgqueue_short.setBufferSize(size);
        } else if (type == Character.TYPE) {
            wgqueue_char.setBufferSize(size);
        } else if (type == Bit.TYPE) {
            wgqueue_bit.setBufferSize(size);
        } else {
            assert wgqueue_obj.size () <= maxSize;
        }
    }

    void makePassThrough ()
    {
        passThrough = true;
    }
}















