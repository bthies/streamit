package streamit.frontend.tojava;
import streamit.frontend.nodes.*;

/**
 * Pass to convert StreamIt enqueue statements to similar function
 * calls.  The StreamIt Java library has <code>enqueueFloat()</code>,
 * etc. functions in the {@link streamit.FeedbackLoop} class.  This
 * pass replaces each StreamIt <code>enqueue</code> statement with a
 * call to the appropriate enqueue function; this requires more
 * support in the compiler after unrolling than generating the older
 * <code>initPath</code> function, but can be done regardless of
 * surrounding control flow.
 *
 * @see     streamit.frontend.TranslateEnqueue
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: EnqueueToFunction.java,v 1.1 2003-09-02 19:12:09 dmaze Exp $
 */
public class EnqueueToFunction extends FEReplacer
{
    // Type that enqueue statements should accept
    private Type enqType;
    
    public Object visitStreamSpec(StreamSpec ss)
    {
        Type lastEnqType = enqType;
        enqType = ss.getStreamType().getLoop();
        
        Object result = super.visitStreamSpec(ss);
        
        enqType = lastEnqType;

        return result;
    }
    
    public Object visitStmtEnqueue(StmtEnqueue stmt)
    {
        // The goal here is to generate a StmtExpr containing an
        // ExprFunCall.  Find the name of the function:
        String fnName;
        if (enqType instanceof TypePrimitive)
        {
            int t = ((TypePrimitive)enqType).getType();
            if (t == TypePrimitive.TYPE_BIT)
                fnName = "enqueueInt";
            else if (t == TypePrimitive.TYPE_COMPLEX)
                fnName = "enqueueObject";
            else if (t == TypePrimitive.TYPE_FLOAT)
                fnName = "enqueueFloat";
            else if (t == TypePrimitive.TYPE_INT)
                fnName = "enqueueInt";
            else
            throw new IllegalStateException("can't translate enqueue: " +
                                            "stream's loop type is " +
                                            enqType);
        }
        else
            fnName = "enqueueObject";
        Expression theFn = new ExprFunCall(stmt.getContext(), fnName,
                                           stmt.getValue());
        Statement result = new StmtExpr(stmt.getContext(), theFn);
        return result;
    }
}
