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
import streamit.scheduler2.constrained.Scheduler;
import streamit.scheduler2.constrained.NoPathException;
import streamit.library.iriter.Iterator;
import streamit.library.iriter.SDEPIterFactory;

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
 * @version $Id: Portal.java,v 1.17 2006-02-20 22:32:17 thies Exp $
 */
public abstract class Portal
{
    // counter to assign each stream a consistent identifier
    private static int MAX_ID = 0;
    // identifier of this stream (used for hashcode)
    private int id = (MAX_ID++);

    protected ArrayList receivers; //List of Filters
    private int minLat,maxLat;
    /**
     * SDEP info for this portal.
     */
    private SDEPInfo sdepInfo;
    /**
     * A general scheduler to determine upstream/downstream relations
     * between filters.
     */
    private static Scheduler upDownScheduler;
    
    public Portal() {
        this.receivers = new ArrayList();
        this.sdepInfo = null;
    }

    /**
     * Use the identifier of this stream as the hashcode, to ensure
     * deterministic behavior in sets and containers (was causing
     * unpredictable exceptions).
     */
    public int hashCode() {
        return id;
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
        // get SDEP to receivers
        SDEPInfo sdep = getSDEP(sender);
        // for each receiver...
        for (int i=0; i<receivers.size(); i++) {
            Stream receiver = (Stream)receivers.get(i);
            // make message -- for now just send with maximum latency
            // to make it easiest to schedule
            Message m;
            if (sdep.downstream[i]) {
                // schedule downstream messages using DstPhase4Src.
                /*
                System.err.println("Sending message " + 
                                   (sender.getSDEPExecutions(false, true)+maxLat+1) + " -> " + 
                                   sdep.data[i].getDstPhase4SrcPhase(sender.getSDEPExecutions(false, true)+maxLat+1));
                */
                m = new Message(sdep.data[i].getDstPhase4SrcPhase(sender.getSDEPExecutions(false, true)+maxLat+1),
                                sdep.downstream[i], handlerName, args);
            } else {
                // schedule upstream messages messages using SrcPhase4Dst
                /*
                System.err.println("Sending message " + 
                                   (sender.getSDEPExecutions(false, true)+maxLat+1) + " -> " + 
                                   sdep.data[i].getSrcPhase4DstPhase(sender.getSDEPExecutions(false, false)+maxLat+1));
                */
                m = new Message(sdep.data[i].getSrcPhase4DstPhase(sender.getSDEPExecutions(false, false)+maxLat+1),
                                sdep.downstream[i], handlerName, args);
            }
            //System.err.println("Enqueuing message <" + handlerName + "> for deliver at time " + m.getDeliveryTime() + " in " + receiver);
            // enqueue message
            receiver.enqueueMessage(m);
        }
    }

    /**
     * Returns SDEP data from sender to each receiver.
     */
    private SDEPInfo getSDEP(Stream sender) {
        if (sdepInfo != null) {
            return sdepInfo;
        } else {
            // initialize general scheduler if needed.  (can't do this
            // in constructor because graph may not be setup yet.)
            if (upDownScheduler == null) {
                upDownScheduler = Scheduler.createForSDEP(new Iterator(Stream.toplevel));
            }

            // for each receiver, calculate up/downstream info
            boolean[] downstream = new boolean[receivers.size()];
            for (int i=0; i<receivers.size(); i++) {
                Stream receiver = (Stream)receivers.get(i);
                switch (Operator.compareStreamPosition(sender, receiver)) {
                case -1: // receiver -> ... -> sender
                    downstream[i] = false;
                    break;
                case 0:  // receiver | sender
                    // fail -- no path between sender and receiver
                    new RuntimeException("No path between message sender and receiver in stream graph.")
                        .printStackTrace();
                    break;
                case 1:  // sender -> ... -> receiver
                    downstream[i] = true;
                    break;
                default:
                    assert false : "Unexpected comparison value.";
                }
            }

            // see if any receivers are phased.  If they are, will
            // have to rebuild scheduler / SDEP graph for each
            // receiver (see SDEPIterFactory for details)
            boolean phasedReceiver = false;
            for (int i=0; i<receivers.size(); i++) {
                if (((Stream)receivers.get(i)) instanceof PhasedFilter) {
                    phasedReceiver = true;
                }
            }

            // for each receiver, calculate sdep data
            SDEPData[] data = new SDEPData[receivers.size()];
            SDEPIterFactory factory = null;
            Scheduler scheduler = null;
            for (int i=0; i<receivers.size(); i++) {
                Stream receiver = (Stream)receivers.get(i);
                // make sdep scheduler.  Need to redo for each receiver if phased.
                if (i==0 || phasedReceiver) {
                    factory = new SDEPIterFactory(sender, receiver, downstream[i]);
                    scheduler = Scheduler.createForSDEP(new Iterator(Stream.toplevel, factory));
                }
                try {
                    if (downstream[i]) {
                        // if downstream path, compute dependences upstream
                        data[i] = scheduler.computeSDEP(new Iterator(sender, factory),
                                                        new Iterator(receiver, factory));
                    } else {
                        // if upstream path, compute dependences upstream
                        data[i] = scheduler.computeSDEP(new Iterator(receiver, factory),
                                                        new Iterator(sender, factory));
                    }
                } catch (NoPathException e) {
                    // should not happen because we checked that there is a path
                    e.printStackTrace();
                }
            }

            sdepInfo = new SDEPInfo(data, downstream);
            return sdepInfo;
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
        // make a copy to mimick pass-by-value
        return Cloner.doCopy(o);
    }
    public Short wrapInObject(short s) {
        return new Short(s);
    }

    /**
     * Just a bundle of the SDEP data and whether or not a given
     * message travels upstream or downstream.
     */
    class SDEPInfo {
        // table of dependence info
        public SDEPData[] data;
        // true iff the receiver is downstream of the sender
        public boolean[] downstream;
    
        public SDEPInfo(SDEPData[] data, boolean[] downstream) {
            this.data = data;
            this.downstream = downstream;
        }
    }
    
}

