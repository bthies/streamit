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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Translates enqueue statements to old syntax.  enqueue statements
 * only appear in initialization code for feedback loops; they push an
 * item on to the output tape of the loop stream.  In the old syntax,
 * this functionality was implemented by the init function calling
 * setDelay with the number of enqueued elements, and the runtime code
 * subsequently calling the correct initPathType function.
 *
 * <p> This currently is a minimalist implementation of the
 * translation necessary. It only notices enqueue statements outside
 * of control flow, and only statements that enqueue a literal value.
 * This is probably enough to handle most simple cases of feedback
 * loops, though.
 * 
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TranslateEnqueue.java,v 1.9 2003-10-09 19:51:02 dmaze Exp $
 */
public class TranslateEnqueue extends FEReplacer
{
    /** true if the current statement is inside control flow */
    private boolean inControl;

    /** values we want to enqueue */
    private List vals;

    /* What do we visit?  While we can ignore filters, other StreamSpecs
     * can contain nested streams that might include feedback loops.  So
     * we need to remember the prevailing StreamSpec, since we might
     * alter it, but we always want a recursive visit. */

    /** Creates a helper <code>Function</code> that returns a value out of
     * <code>vals</code>. */
    private Function makeInitPath(StreamSpec ss)
    {
        FEContext context = ss.getContext();
        List stmts = new ArrayList();
        Expression n = new ExprVar(context, "n");
        int i = 0;
        for (Iterator iter = vals.iterator(); iter.hasNext(); )
        {
            Expression val = (Expression)iter.next();
            Expression cond = new ExprBinary(context, ExprBinary.BINOP_EQ,
                                             n, new ExprConstInt(context, i));
            stmts.add(new StmtIfThen(context, cond,
                                     new StmtReturn(context, val), null));
            i++;
        }
        StmtBlock body = new StmtBlock(context, stmts);
        // Figure out the return type and name; use the loop type, which
        // should be assigned by the AssignLoopTypes pass.
        Type returnType = ss.getStreamType().getLoop();
        // Claim: it's a primitive type, we can't deal otherwise.
        TypePrimitive tp = (TypePrimitive)returnType;
        String name = null;
        if (tp.getType() == TypePrimitive.TYPE_FLOAT)
        {
            name = "initPathFloat";
            stmts.add(new StmtReturn(context,
                                     new ExprConstFloat(context, 0.0)));
        }
        else if (tp.getType() == TypePrimitive.TYPE_INT)
        {
            returnType = new TypePrimitive(TypePrimitive.TYPE_INT);
            name = "initPathInt";
            stmts.add(new StmtReturn(context,
                                     new ExprConstInt(context, 0)));
        }
        else if (tp.getType() == TypePrimitive.TYPE_COMPLEX)
        {
            name = "initPath";
            stmts.add(new StmtReturn(context,
                                     new ExprConstFloat(context, 0.0)));
        }
        else
        {
            // char, string don't have Types.  Yay corner cases.
            throw new IllegalStateException("can't translate enqueue: " +
                                            "stream's input type is " + tp);
        }
        Parameter param =
            new Parameter(new TypePrimitive(TypePrimitive.TYPE_INT), "n");
        return Function.newHelper(context, name, returnType,
                                  Collections.singletonList(param), body);
    }

    public Object visitStreamSpec(StreamSpec ss)
    {
        List lastVals = vals;
        vals = new ArrayList();
        StreamSpec ssNew = (StreamSpec) super.visitStreamSpec(ss);
        // If we have extra values, then generate an initPath function.
        if (!vals.isEmpty())
        {
            List fns = new ArrayList(ssNew.getFuncs());
            fns.add(makeInitPath(ss));
            ssNew = new StreamSpec(ssNew.getContext(), ssNew.getType(),
                                   ssNew.getStreamType(), ssNew.getName(),
                                   ssNew.getParams(), ssNew.getVars(), fns);
        }
        vals = lastVals;
        return ssNew;
    }

    public Object visitFunction(Function fn)
    {
        // Function bodies are outside of control flow.
        boolean lastControl = inControl;
        inControl = false;
        Function fnNew = (Function) super.visitFunction(fn);
        inControl = lastControl;
        // If we have enqueued values, call setDelay().  Pretend enqueues
        // only happen in one function, which should generally be the
        // case anyways.
        if (!vals.isEmpty())
        {
            Expression count = new ExprConstInt(fn.getContext(), vals.size());
            Expression delay =
                new ExprFunCall(fn.getContext(), "setDelay", count);
            Statement call = new StmtExpr(delay);
            // Now add the statement to the function.
            StmtBlock body = (StmtBlock)fnNew.getBody();
            List stmts = new ArrayList(body.getStmts());
            stmts.add(call);
            body = new StmtBlock(body.getContext(), stmts);
            fnNew = new Function(fnNew.getContext(), fnNew.getCls(),
                                 fnNew.getName(), fnNew.getReturnType(),
                                 fnNew.getParams(), body);
        }
        return fnNew;
    }

    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
        boolean lastControl = inControl;
        inControl = true;
        Object rtn = super.visitStmtDoWhile(stmt);
        inControl = lastControl;
        return rtn;
    }

    public Object visitStmtEnqueue(StmtEnqueue stmt)
    {
        // If we're inside control flow, that's bad.
        if (inControl)
            throw new InvalidControlFlowException
                ("enqueue statement inside any control flow not handled");
        // Check the argument; it should be a literal.
        Expression val = stmt.getValue();
        if (!(val instanceof ExprConstChar ||
              val instanceof ExprConstFloat ||
              val instanceof ExprConstInt ||
              val instanceof ExprConstStr))
            throw new InvalidControlFlowException
                ("enqueue statement with non-constant not handled");
        // Okay, push the expression on to the list, and drop the statement.
        vals.add(val);
        return null;
    }

    public Object visitStmtFor(StmtFor stmt)
    {
        boolean lastControl = inControl;
        inControl = true;
        Object rtn = super.visitStmtFor(stmt);
        inControl = lastControl;
        return rtn;
    }

    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        boolean lastControl = inControl;
        inControl = true;
        Object rtn = super.visitStmtIfThen(stmt);
        inControl = lastControl;
        return rtn;
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
        boolean lastControl = inControl;
        inControl = true;
        Object rtn = super.visitStmtWhile(stmt);
        inControl = lastControl;
        return rtn;
    }

}
