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

package streamit.frontend.nodes;

import java.util.Collections;

/**
 * A StreamIt work or phase function.  A work function always returns
 * void and may or may not have a name.  It takes no parameters.
 * Additionally, it has rate declarations; there are expressions
 * corresponding to the number of items peeked at, popped, and pushed
 * per steady-state execution.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: FuncWork.java,v 1.6 2003-10-09 19:50:59 dmaze Exp $
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

