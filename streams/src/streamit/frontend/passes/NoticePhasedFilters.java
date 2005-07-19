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

package streamit.frontend.passes;

import streamit.frontend.nodes.*;

import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Convert filters to phased filters where appropriate.  This pass
 * replaces StreamSpecs corresponding to filters with StreamSpecs
 * corresponding to phased filters, but only if the work function has
 * no declared I/O rates.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: NoticePhasedFilters.java,v 1.6 2005-07-19 18:09:10 madrake Exp $
 */
public class NoticePhasedFilters extends FEReplacer
{
    private StreamSpec ss;
    private boolean libraryFormat;

    public NoticePhasedFilters(boolean lib) {
        this.libraryFormat = lib;
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        if (spec.getType() != StreamSpec.STREAM_FILTER)
            return spec;

        // Find the work function.  There should be exactly one;
        // count on StreamSpec to DTRT.
        FuncWork fw = spec.getWorkFunc();
        
        if (spec.getPhasedFuncs().size() > 0) {

            // Check...we have a phased filter now.  We need to revisit
            // the functions and rewrite the work function...
            ss = spec;
            ArrayList newFuncs = new ArrayList(spec.getFuncs());
            newFuncs.remove(fw);
            newFuncs.add(fw.accept(this));

            StreamSpec newSpec =
                new StreamSpec(spec.getContext(),
                               StreamSpec.STREAM_FILTER,
                               spec.getStreamType(),
                               spec.getName(),
                               spec.getParams(),
                               spec.getVars(),
                               newFuncs);
            return newSpec;
        }

        return spec;
    }

    // Now, within this, we only expect to be recursing deeper within
    // the main work function of a phased filter.  This means we can
    // use the extant FEReplacer engine; the only thing we care about
    // is changing expression statements that are function calls into
    // phase invocations.
    public Object visitStmtExpr(StmtExpr stmt)
    {
        Expression expr = stmt.getExpression();
        if (!(expr instanceof ExprFunCall))
            return stmt;
        ExprFunCall fc = (ExprFunCall)expr;
        Function target = ss.getFuncNamed(fc.getName());
        if (libraryFormat) {
            List stmtList = new ArrayList();
            stmtList.add(stmt);
            stmtList.add(new StmtExpr(stmt.getContext(), 
                                      new ExprFunCall(stmt.getContext(), 
                                                      "contextSwitch", 
                                                      new ArrayList())));
            return new StmtBlock(stmt.getContext(), stmtList);
        } else {
            if (target.getCls() != Function.FUNC_PHASE)
                return stmt;
            return new StmtPhase(stmt.getContext(), fc);
        }
    }
}




