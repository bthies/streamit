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

package streamit.library;

import java.util.*;

public class Channel extends streamit.misc.DestroyedClass
{
    Class type;
    Operator source = null, sink = null;
    boolean declaredFull = false;

    LinkedList queue;

    Integer popPushCount = null;
    Integer peekCount = null;

    int maxSize = -1;
    boolean passThrough = false;
    
    int totalItemsPushed = 0, totalItemsPopped = 0;

    // the channel should be constructed with a 0-length array
    // indicating the type that will be held in this channel.
    void setupChannel (Class channelType)
    {
        assert channelType != null;
        type = channelType;
        queue = new LinkedList ();
    }
    public Channel(Class channelType)
    {
        setupChannel (channelType);
    }

    public Channel (Class channelType, int popPush)
    {
        setupChannel (channelType);

        // store the popPush
        popPushCount = new Integer (popPush);
    }

    public Channel (Class channelType, int pop, int peek)
    {
        setupChannel (channelType);

        // store the popPush
        popPushCount = new Integer (pop);
        peekCount = new Integer (peek);
    }

    public Channel (Channel original)
    {
        assert original != null;

        type = original.getType ();
        queue = new LinkedList ();

        // copy pop/push/peek values
        popPushCount = original.popPushCount;
        peekCount = original.peekCount;
    }

    void ensureData (int amount)
    {
        while (queue.size () < amount)
        {
            assert source != null;

            // if I need to get data from my source I better not be a scheduled
            // buffer.
            assert maxSize == -1:
                "maxSize should equal -1 " +
                "(representing not a scheduled buffer)\n" +
                "Queue: " + queue +
                ".size: " + queue.size() +
                " amount is: " + amount +
                " and maxSize is: " + maxSize;

	    source.prepareToWork();
            source.work ();
        }
    }

    void ensureData ()
    {
        ensureData (1);
    }

    private void enqueue (Object o)
    {
        queue.addLast (o);
        
		totalItemsPushed++;
		source.registerPush();

        // overflow at 50 chars in the queue
        if (queue.size () > 100 && !declaredFull)
        {
                source.addFullChannel (this);
            declaredFull = true;
        }

        // make sure that the channel isn't overflowing
        //if (queue.size () == maxSize) System.out.print ("*");
        assert queue.size () <= maxSize || maxSize == -1:
            "Expecting queue.size () <= maxSize || maxSize == -1,\n" +
            "   but queue.size()==" + queue.size() + " and maxSize==" + 
            maxSize;
		
        if (passThrough) {
	    sink.prepareToWork();
	    sink.work ();
	}
    }

    private Object dequeue ()
    {
        if (queue.size () < 50 && declaredFull)
        {
            source.removeFullChannel (this);
            declaredFull = false;
        }

        totalItemsPopped++;
	sink.registerPop();

        return queue.removeFirst ();
    }


    // PUSH OPERATIONS ----------------------------------------------
    
    public int getItemsPushed () { return totalItemsPushed; }

    // push something of type <type>
    public void push(Object o)
    {
        assert o.getClass () == type;

        enqueue (o);
    }
    
    // push a boolean
    public void pushBool (boolean b)
    {
        assert type == Boolean.TYPE;
        
        enqueue (new Boolean (b));
    }

    // push an int
    public void pushInt(int i)
    {
        assert type == Integer.TYPE;

        enqueue (new Integer (i));
    }

    // push a short
    public void pushShort(short s)
    {
        assert type == Short.TYPE;

        enqueue (new Short (s));
    }
    //push a bit
    public void pushBit(int i)
    {
	assert type == Bit.TYPE;
	enqueue(new Integer(i));
    }

    // push a char
    public void pushChar(char c)
    {
        assert type == Character.TYPE;

        enqueue (new Character  (c));
    }

    // push a double
    public void pushDouble(double d)
    {
        assert type == Double.TYPE;

        enqueue (new Double (d));
    }

    // push a float
    public void pushFloat(float d)
    {
        assert type == Float.TYPE;

        enqueue (new Float (d));
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

    // pop something of type <type>
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
        
        Boolean data;
        data = (Boolean) pop ();
        assert data != null;
        
        return data.booleanValue ();
    }

    // pop an int
    public int popInt()
    {
        assert type == Integer.TYPE;

        Integer data;
        data = (Integer) pop ();
        assert data != null;

        return data.intValue ();
    }

    // pop a short
    public short popShort()
    {
        assert type == Short.TYPE;

        Short s;
        s = (Short) pop ();
        assert s != null;

        return s.shortValue ();
    }

    // pop a char
    public char popChar()
    {
        assert type == Character.TYPE;

        Character c;
        c = (Character) pop ();
        assert c != null;

        return c.charValue ();
    }
    //pop a bit
    public int popBit(){
	assert type == Bit.TYPE;
	Integer data;
	data = (Integer)pop();
	assert data !=null;
	return data.intValue();
    }

    // pop a double
    public double popDouble()
    {
        assert type == Double.TYPE;

        Double data;
        data = (Double) pop ();
        assert data != null;

        return data.doubleValue ();
    }

    // pop a float
    public float popFloat()
    {
        assert type == Float.TYPE;

        Float data;
        data = (Float) pop ();
        assert data != null;

        return data.floatValue ();
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

    // peek at something of type <type>
    public Object peek(int index)
    {
	sink.registerPeek(index);
        ensureData (index + 1);

        Object data;
        data = queue.get (index);
        assert data != null;

        return data;
    }

    // peek at an int
    public int peekInt(int index)
    {
        assert type == Integer.TYPE;

        Integer data;
        data = (Integer) peek (index);
        assert data != null;

        return data.intValue ();
    }
    //peek a bit
    public int peekBit(int index){
	assert type == Bit.TYPE;
	Integer data;
	data = (Integer) peek(index);
	assert data!=null;
	return data.intValue();
    }


    // peek at a short
    public short peekShort (int index)
    {
        assert type == Short.TYPE;

        Short data;
        data = (Short) peek (index);
        assert data != null;

        return data.shortValue ();
    }

    // peek at a char
    public char peekChar(int index)
    {
        assert type == Character.TYPE;

        Character data;
        data = (Character) peek (index);
        assert data != null;

        return data.charValue ();
    }

    // peek at a double
    public double peekDouble(int index)
    {
        assert type == Double.TYPE;

        Double data;
        data = (Double) peek (index);
        assert data != null;

        return data.doubleValue ();
    }

    // peek at a float
    public float peekFloat(int index)
    {
        assert type == Float.TYPE;

        Float data;
        data = (Float) peek (index);
        assert data != null;

        return data.floatValue ();
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

    Class getType () { return type; }

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
        assert queue.size () <= maxSize;
    }

    void makePassThrough ()
    {
        passThrough = true;
    }
}
