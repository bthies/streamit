/*
 * WorkFunction.java: container class to represent a work function
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: WorkFunction.java,v 1.1 2002-06-12 17:57:41 dmaze Exp $
 */

package streamit.frontend.tojava;

public class WorkFunction
{
    // I/O rates:
    public String pushRate, peekRate, popRate;
    
    // Bits of declaration:
    public String body;

    public WorkFunction()
    {
        pushRate = "0";
        peekRate = "0";
        popRate = "0";
    }
}
