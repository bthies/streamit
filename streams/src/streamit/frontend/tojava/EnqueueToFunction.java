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
 * @version $Id: EnqueueToFunction.java,v 1.3 2003-10-09 19:51:02 dmaze Exp $
 */
public class EnqueueToFunction extends FEReplacer
{
    // Type that enqueue statements should accept
    private Type enqType;
    
    public Object visitStreamSpec(StreamSpec ss)
    {
        Type lastEnqType = enqType;
        // NB: feedback loops should always have stream types,
        // from AssignLoopTypes pass.  Non-feedback loops might
        // not, but they also shouldn't have enqueue statements.
        if (ss.getStreamType() != null)
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
