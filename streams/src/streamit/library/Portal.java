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

/**
 * A portal that messages can be sent to.  This class is not currently
 * useful in the Java library.  For the StreamIt compiler, the set of
 * messages that can be sent should be defined in an interface.  A
 * class derived from <code>Portal</code> and implementing the interface
 * whose name ends in "Portal" should be defined; that class is the portal
 * object.  Receiver objects should also implement the interface.
 *
 * @version $Id: Portal.java,v 1.4 2003-10-09 20:51:27 dmaze Exp $
 */
public class Portal
{
    public void setAnyLatency()
    {
        throw new UnsupportedOperationException
            ("StreamIt Java library does not support messaging");
    }
    
    public void setMaxLatency(int b)
    {
        throw new UnsupportedOperationException
            ("StreamIt Java library does not support messaging");
    }

    public void setLatency(int a, int b)
    {
        throw new UnsupportedOperationException
            ("StreamIt Java library does not support messaging");
    }

    public void regReceiver(Object o)
    {
        throw new UnsupportedOperationException
            ("StreamIt Java library does not support messaging");
    }
}
