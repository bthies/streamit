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

/**
 * A statement containing a call to a phase function.  This statement
 * should only appear inside a work function with no I/O rates, and then
 * only within a phased filter StreamSpec.  The expected execution model
 * is that this should invoke the specified phased function and then
 * suspend the work function, though this may not be the most accurate
 * description or anything even close to an actual implementation.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtPhase.java,v 1.2 2003-10-09 19:51:00 dmaze Exp $
 */
public class StmtPhase extends Statement
{
    private ExprFunCall fc;
    
    /** Creates a new phase statement that calls the specified function. */
    public StmtPhase(FEContext context, ExprFunCall fc)
    {
        super(context);
        this.fc = fc;
    }
    
    /** Create a phase statement using the specified function call, using
     * that call's context as our own. */
    public StmtPhase(ExprFunCall fc)
    {
        this(fc.getContext(), fc);
    }
    
    /** Gets the function call associated with the phase invocation. */
    public ExprFunCall getFunCall()
    {
        return fc;
    }
    
    public Object accept(FEVisitor v)
    {
        return v.visitStmtPhase(this);
    }
}
