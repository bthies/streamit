/**
 * FuncWork.java: a work function declaration
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: FuncWork.java,v 1.2 2002-09-06 16:28:43 dmaze Exp $
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
     * and I/O rates.  If the pop or push rates are null, they are
     * replaced with a constant 0; if the peek rate is null, it is
     * replaced with the pop rate. */
    public FuncWork(FEContext context, String name, Statement body,
                    Expression peek, Expression pop, Expression push)
    {
        super(context, FUNC_WORK, name,
              new TypePrimitive(TypePrimitive.TYPE_VOID),
              Collections.EMPTY_LIST, body);
        peekRate = peek;
        popRate = pop;
        pushRate = push;
        // Update null parameters:
        if (popRate == null) popRate = new ExprConstInt(context, 0);
        if (peekRate == null) peekRate = popRate;
        if (pushRate == null) pushRate = new ExprConstInt(context, 0);
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

