/**
 * FuncWork.java: a work function declaration
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: FuncWork.java,v 1.5 2003-01-09 22:38:12 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.Collections;

/**
 * FuncWork represents a StreamIt work function.  A work function always
 * returns void and may or may not have a name.  It takes no parameters.
 * Additionally, it has rate declarations; there are expressions corresponding
 * to the number of items peeked at, popped, and pushed per steady-state
 * execution.
 */
public class FuncWork extends Function
{
    private Expression peekRate, popRate, pushRate;
    
    /** Creates a new work function given its name (or null), body,
     * and I/O rates.  The I/O rates may be null if declarations are
     * omitted from the original source. */
    public FuncWork(FEContext context, int cls,
                    String name, Statement body,
                    Expression peek, Expression pop, Expression push)
    {
        super(context, cls, name,
              new TypePrimitive(TypePrimitive.TYPE_VOID),
              Collections.EMPTY_LIST, body);
        peekRate = peek;
        popRate = pop;
        pushRate = push;
    }

    /** Gets the peek rate of this. */
    public Expression getPeekRate() 
    {
        return peekRate;
    }
    
    /** Gets the pop rate of this. */
    public Expression getPopRate()
    {
        return popRate;
    }
    
    /** Gets the push rate of this. */
    public Expression getPushRate()
    {
        return pushRate;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitFuncWork(this);
    }
}

