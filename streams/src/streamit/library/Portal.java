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

import streamit.scheduler2.SDEPData;
import streamit.library.iriter.Iterator;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A portal that messages can be sent to.  For the StreamIt compiler,
 * the set of messages that can be sent should be defined in an
 * interface.  A class derived from <code>Portal</code> and
 * implementing the interface whose name ends in "Portal" should be
 * defined; that class is the portal object.  Receiver objects should
 * also implement the interface.
 *
 * @version $Id: Portal.java,v 1.7 2005-07-19 22:25:22 thies Exp $
 */
public abstract class Portal
{
    protected ArrayList receivers; //List of Filters
    private int minLat,maxLat;
    /**
     * Stream -> SDEPData[].  Maps a given sender to the SDEP data
     * for each receiver in <receivers>.
     */
    private HashMap SDEPCache;
    /**
     * Constrained scheduler, run on toplevel stream.
     */
    private streamit.scheduler2.constrained.Scheduler scheduler;
    
    public Portal() {
	this.receivers = new ArrayList();
	this.SDEPCache = new HashMap();
    }

    public void setAnyLatency() // default to zero latency
    {
	setLatency(0,0);
	/*
        throw new UnsupportedOperationException
            ("StreamIt Java library does not support setting AnyLatency yet");
	*/
    }
    
    public void setMaxLatency(int b) // default to zero minimum latency
    {
	setLatency(0,b);
	/*
        throw new UnsupportedOperationException
            ("StreamIt Java library does not support setting MaxLatency yet");
	*/
    }

    public void setLatency(int a, int b)
    {
	//System.out.println("Setting Latency: "+a+", "+b);
	minLat=a;
	maxLat=b;
    }

    public void regReceiver(Object o)
    {
	//System.out.println("Registering Receiver: "+o);
	receivers.add(o);
    }

    /**
     * Enqueues a message to <handlerName> method in all receivers of
     * this, from <sender>, and with arguemnts <args>.
     */
    public void enqueueMessage(Stream sender, String handlerName, Object[] args) {
	// setup scheduler if it's not ready yet (have to do it here
	// instead of constructor because scheduler depends on
	// structure of stream graph, which is not ready at the time
	// this is constructed.)
	if (this.scheduler==null) {
	    this.scheduler = streamit.scheduler2.constrained.Scheduler.createForSDEP(new Iterator(Stream.toplevel));
	}
	// get SDEP to receivers
	SDEPData[] sdep = getSDEP(sender);
	// for each receiver...
	for (int i=0; i<receivers.size(); i++) {
	    Stream receiver = (Stream)receivers.get(i);
	    // make message
	    Message m = new Message(sdep[i].getDstPhase4SrcPhase(sender.getNumExecutions()+minLat+1),
				    handlerName, args);
	    //System.err.println("Enqueuing message <" + handlerName + "> for deliver at time " + m.getDeliveryTime() + " in " + receiver);
	    // enqueue message
	    receiver.enqueueMessage(m);
	}
    }

    /**
     * Returns SDEP data from sender to each receiver.
     */
    private SDEPData[] getSDEP(Stream sender) {
	if (SDEPCache.containsKey(sender)) {
	    return (SDEPData[])SDEPCache.get(sender);
	} else {
	    SDEPData[] result = new SDEPData[receivers.size()];
	    for (int i=0; i<receivers.size(); i++) {
		Stream receiver = (Stream)receivers.get(i);
		try {
		    result[i] = scheduler.computeSDEP(new Iterator(sender),
						      new Iterator(receiver));
		} catch (streamit.scheduler2.constrained.NoPathException e) {
		    System.err.println("No path between message sender and receiver in stream graph.");
		    e.printStackTrace();
		}
	    }
	    SDEPCache.put(sender, result);
	    return result;
	}
    }
    
    /**
     * For wrapping primitives into objects, so that message senders
     * can have uniform interface for queueing up messaging arguments.
     */
    public Byte wrapInObject(byte b) { 
	return new Byte(b);
    }
    public Boolean wrapInObject(boolean b) { 
	return new Boolean(b); 
    }
    public Character wrapInObject(char c) { 
	return new Character(c); 
    }
    public Float wrapInObject(float f) { 
	return new Float(f); 
    }
    public Double wrapInObject(double d) { 
	return new Double(d);
    }
    public Integer wrapInObject(int i) { 
	return new Integer(i); 
    }
    public Long wrapInObject(long l) {
	return new Long(l);
    }
    public Object wrapInObject(Object o) {
	return o;
    }
    public Short wrapInObject(short s) {
	return new Short(s);
    }
    
}
