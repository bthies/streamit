package streamit.scheduler.base;

import streamit.misc.DestroyedClass;

/* $Id: Stream.java,v 1.1 2002-05-27 03:18:51 karczma Exp $ */

/**
 * This class provides the basic functionality for
 * all future stream classes.  This will ensure that streams can
 * be used interchangably.  I have to do this because Java doesn't
 * have multi-inheritance :(
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

abstract class Stream extends DestroyedClass implements StreamInterface
{
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
