package streamit.scheduler.base;

import streamit.misc.DestroyedClass;
import streamit.scheduler.iriter.Iterator;

/* $Id: Stream.java,v 1.3 2002-12-02 17:49:37 karczma Exp $ */

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
