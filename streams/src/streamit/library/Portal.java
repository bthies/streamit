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

import java.util.ArrayList;

/**
 * A portal that messages can be sent to.  This class is not currently
 * useful in the Java library.  For the StreamIt compiler, the set of
 * messages that can be sent should be defined in an interface.  A
 * class derived from <code>Portal</code> and implementing the interface
 * whose name ends in "Portal" should be defined; that class is the portal
 * object.  Receiver objects should also implement the interface.
 *
 * @version $Id: Portal.java,v 1.5 2004-07-28 20:15:00 jasperln Exp $
 */
public abstract class Portal
{
    protected ArrayList receivers; //List of Filters
    private int minLat,maxLat;

    public Portal() {
	receivers=new ArrayList();
    }

    public void setAnyLatency()
    {
        throw new UnsupportedOperationException
            ("StreamIt Java library does not support setting AnyLatency yet");
    }
    
    public void setMaxLatency(int b)
    {
        throw new UnsupportedOperationException
            ("StreamIt Java library does not support setting MaxLatency yet");
    }

    public void setLatency(int a, int b)
    {
        //throw new UnsupportedOperationException
	//("StreamIt Java library does not support messaging");
	System.out.println("Setting Latency: "+a+", "+b);
	minLat=a;
	maxLat=b;
    }

    public void regReceiver(Object o)
    {
        //throw new UnsupportedOperationException
	//("StreamIt Java library does not support messaging");
	System.out.println("Registering Receiver: "+o);
	receivers.add(o);
    }

    protected void enqueueMessage(int message,Object[] args) {
	System.out.println("Enqueue Message: "+message);
    }

    protected abstract void fireMessage(int message,Object[] args);
}
