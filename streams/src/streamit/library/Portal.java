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
import streamit.library.iriter.IterFactory;
import streamit.library.iriter.BasicIterFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A portal that messages can be sent to.  For the StreamIt compiler,
 * the set of messages that can be sent should be defined in an
 * interface.  A class derived from <pre>code</pre>Portal</code> and
 * implementing the interface whose name ends in "Portal" should be
 * defined; that class is the portal object.  Receiver objects should
 * also implement the interface.
 *
 * @version $Id: Portal.java,v 1.20 2006-08-23 23:01:15 thies Exp $
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
     * Enqueues a message to <pre>handlerName</pre> method in all receivers of
     * this, from <pre>sender</pre>, and with arguemnts <pre>args</pre>.
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
     * Calculates which receivers are downstream of the sender.  In
     * returned array, element of "true" means the i'th receiver is
     * downstream.
     */
    public boolean[] calcDownstream(Stream sender) {
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
        return downstream;
    }

    /**
     * Returns list of receivers that are upstream of the sender.
     * Requires that upstream/downstream info has been calculated.
     */
    private List getUpstreamReceivers(boolean[] downstream) {
        List result = new LinkedList();
        for (int i=0; i<receivers.size(); i++) {
            if (!downstream[i]) {
                result.add(receivers.get(i));
            }
        }
        return result;
    }
    
    /**
     * Given whether or not each receiver is <pre>downstream</pre> of the
     * sender, return mapping from filters to SDEP data for the
     * upstream messages.
     */
    private HashMap getUpstreamSDEPs(Stream sender, boolean[] downstream) {
        HashMap result = new HashMap();
        // identify list of upstream receivers.
        List upstreamReceivers = getUpstreamReceivers(downstream);
        // for now have to do each one separately
        for (java.util.Iterator i = upstreamReceivers.iterator(); i.hasNext(); ) {
            Stream receiver = (Stream)i.next();

            // make factory (this used to be used for phased filters,
            // but now is more general than really needed.)
            IterFactory factory = new BasicIterFactory();
            Scheduler scheduler = Scheduler.createForSDEP(new Iterator(Stream.toplevel, factory));

            // compute dependences upstream
            SDEPData data = null;
            try {
                data = scheduler.computeSDEP(new Iterator(receiver, factory),
                                             new Iterator(sender, factory));
            } catch (NoPathException e) {
                // should not happen because we checked that there is a path
                e.printStackTrace();
            }
            result.put(receiver, data);
        }
        return result;
    }

    /**
     * Returns list of HashSets of receivers that are downstream of
     * the sender.  Each set is safe to calculate in parallel.
     *
     * Currently two SDEP receivers can be calculated together if they
     * are both downstream of the sender. This procedure was
     * simplified considerably when phased filters were removed from
     * the language.
     *
     * Requires that upstream/downstream info has been calculated.
     */
    private List getDownstreamReceiverSets(boolean[] downstream) {
        LinkedList result = new LinkedList();

        // build set downstream filters
        HashSet filters = new HashSet();
        for (int i=0; i<receivers.size(); i++) {
            if (downstream[i]) {
                filters.add(receivers.get(i));
            }
        }
        
        // prepend the list the list of sets (we only maintain non-empty sets)
        if (filters.size()>0) {
            result.addFirst(filters);
        }
        
        return result;
    }

    /**
     * Given whether or not each receiver is <pre>downstream</pre> of the
     * sender, return mapping from filters to SDEP data for the
     * downstream messages.
     */
    private HashMap getDownstreamSDEPs(Stream sender, boolean[] downstream) {
        HashMap result = new HashMap();
        // calculate list of HashSets of downstream receivers that can
        // be calcualted in parallel
        List downstreamReceivers = getDownstreamReceiverSets(downstream);
        // do one calculation per set
        for (java.util.Iterator i = downstreamReceivers.iterator(); i.hasNext(); ) {
            HashSet receiverSet = (HashSet)i.next();
            assert receiverSet.size() > 0;

            // make factory (this used to be used for phased filters,
            // but now is more general than really needed.)
            IterFactory factory = new BasicIterFactory();
            Scheduler scheduler = Scheduler.createForSDEP(new Iterator(Stream.toplevel, factory));

            // map receiverSet (holds Streams) to receiverIters (holds Iterators)
            HashSet receiverIters = new HashSet();
            for (java.util.Iterator setIter = receiverSet.iterator(); setIter.hasNext(); ) {
                receiverIters.add(new Iterator((Stream)setIter.next(), factory));
            }

            // compute dependences downstream
            HashMap resultIters = null;
            try {
                resultIters = scheduler.computeSDEP(new Iterator(sender, factory),
                                                    receiverIters);
            } catch (NoPathException e) {
                // should not happen because we checked that there is a path
                e.printStackTrace();
            }

            // map resultIters (keyed on Iterators) to data (keyed on Streams)
            for (java.util.Iterator resultIter = resultIters.keySet().iterator(); resultIter.hasNext(); ) {
                Iterator iter = (Iterator)resultIter.next();
                assert resultIters.get(iter)!=null : "Null data for " + iter.getObject();
                result.put(iter.getObject(), resultIters.get(iter));
            }
        }
        return result;
    }


    /**
     * Toplevel procedure for getting SDEP data, from sender to each
     * receiver.
     */
    private SDEPInfo getSDEP(Stream sender) {
        if (sdepInfo != null) {
            return sdepInfo;
        } else {
            // for each receiver, calculate up/downstream info
            boolean[] downstream = calcDownstream(sender);

            // get upstream and downstream sdep data
            HashMap upstreamSDEPs = getUpstreamSDEPs(sender, downstream);
            HashMap downstreamSDEPs = getDownstreamSDEPs(sender, downstream);

            // combine data into single map (receiver Stream -> SDEP data)
            HashMap data = new HashMap();
            data.putAll(upstreamSDEPs);
            data.putAll(downstreamSDEPs);

            // dump hashmap of data into array of sdepInfo
            SDEPData[] dataArray = new SDEPData[receivers.size()];
            for (int i=0; i<dataArray.length; i++) {
                // find data for receiver[i]
                Stream receiver = (Stream)receivers.get(i);
                assert data.containsKey(receiver) : "No data for " + receiver;
                dataArray[i] = (SDEPData)data.get(receiver);
            }
            sdepInfo = new SDEPInfo(dataArray, downstream);

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

