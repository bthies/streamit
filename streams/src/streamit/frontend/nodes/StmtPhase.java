/*
 * StmtPhase.java: a statement that invokes a work function phase
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtPhase.java,v 1.1 2003-01-10 18:22:01 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A statement containing a call to a phase function.  This statement
 * should only appear inside a work function with no I/O rates, and then
 * only within a phased filter StreamSpec.  The expected execution model
 * is that this should invoke the specified phased function and then
 * suspend the work function, though this may not be the most accurate
 * description or anything even close to an actual implementation.
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
