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

package streamit.scheduler2.base;

import streamit.misc.DestroyedClass;
import streamit.scheduler2.iriter.Iterator;

/**
 * This class provides the basic functionality for
 * all future stream classes.  This will ensure that streams can
 * be used interchangably.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

abstract class Stream extends DestroyedClass implements StreamInterface
{
    final private Iterator streamIter;
    
    protected Stream (Iterator _streamIter)
    {
        ASSERT (_streamIter != null);
        streamIter = _streamIter;
    }
    
    public Iterator getStreamIter () { return streamIter; }
    
    private int steadyPeek, steadyPop, steadyPush;
    
    public int getSteadyPeek () { return steadyPeek; }
    public int getSteadyPop () { return steadyPop; }
    public int getSteadyPush () { return steadyPush; }
    
    /**
     * set the steady peek value
     */
    void setSteadyPeek (int _steadyPeek)
    {
        steadyPeek = _steadyPeek;
    }

    /**
     * set the steady pop value
     */
    void setSteadyPop (int _steadyPop)
    {
        steadyPop = _steadyPop;
    }

    /**
     * set the steady push value
     */
    void setSteadyPush (int _steadyPush)
    {
        steadyPush = _steadyPush;
    }
}
