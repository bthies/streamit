/*
 * NoticePhasedFilters.java: convert filters to phased ones where appropriate
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: NoticePhasedFilters.java,v 1.4 2003-02-21 15:34:57 dmaze Exp $
 */

package streamit.frontend.passes;

import streamit.frontend.nodes.*;

import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Front-end visitor pass that replaces StreamSpecs corresponding to
 * filters with StreamSpecs corresponding to phased filters, but only
 * if the work function has no declared I/O rates.
 */
public class NoticePhasedFilters extends FEReplacer
{
    private StreamSpec ss;
    
    public Object visitStreamSpec(StreamSpec spec)
    {
        if (spec.getType() != StreamSpec.STREAM_FILTER)
            return spec;

        // Find the work function.  There should be exactly one;
        // count on StreamSpec to DTRT.
        FuncWork fw = spec.getWorkFunc();
        
        if (fw.getPeekRate() == null &&
            fw.getPopRate() == null &&
            fw.getPushRate() == null)
        {
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
        if (target.getCls() != Function.FUNC_PHASE)
            return stmt;
        return new StmtPhase(stmt.getContext(), fc);
    }
}
