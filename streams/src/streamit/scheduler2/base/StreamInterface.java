package streamit.scheduler.base;

/* $Id: StreamInterface.java,v 1.2 2002-06-09 22:38:47 karczma Exp $ */

/**
 * This interface will provide the basic functionality for
 * all future stream classes.  This will ensure that streams can
 * be used interchangably.  I have to make this an interface because
 * Java doesn't have multi-inheritance :(
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public interface StreamInterface
{
    /**
     * return number of data peeked in a minimal steady execution
     * of this element.
     * @return number of data peeked in a steady execution.
     */
    public int getSteadyPeek ();

    /**
     * return number of data popped in a minimal steady execution
     * of this element.
     * @return number of data popped in a steady execution.
     */
    public int getSteadyPop ();

    /**
     * return number of data pushed in a minimal steady execution
     * of this element.
     * @return number of data pushed in a steady execution.
     */
    public int getSteadyPush ();
    
    /**
     * compute the steady state peek/pop/push values.
     * This will compute the number of executions of each
     * subcomponent in steady state.
     */
    void computeSteadyState ();
}
