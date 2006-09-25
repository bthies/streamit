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

import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Insert I/O rate declarations into filters' init functions.  For
 * programs destined for the Java library, this also inserts the phase
 * declarations into the init function.  For programs headed for the
 * StreamIt compiler, phase construction happens implicitly with calls
 * inserted in <code>NodesToJava</code>.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: InsertIODecls.java,v 1.15 2006-09-25 13:54:54 dimock Exp $
 */
public class InsertIODecls extends InitMunger
{
    /**
     * Creates a new pass object.
     */
    public InsertIODecls() {}

    /**
     * Given a list of functions, find a work function.  If the list
     * contains an anonymous work function, return that; otherwise,
     * return the first work function, or null if there are no work
     * functions at all.
     *
     * @param fns   List of functions to search
     * @return      Primary work function in fns
     */
    public static Function findWork(List fns, boolean init)
    {
        Function work = null;
        for (Iterator iter = fns.iterator(); iter.hasNext(); )
            {
                Function fn = (Function)iter.next();
                int cls = init ? Function.FUNC_PREWORK : Function.FUNC_WORK;
                if (fn.getCls() == cls)
                    {
                        if (fn.getName() == null)
                            work = fn;
                        if (work == null)
                            work = fn;
                    }
            }
        return work;
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        spec = (StreamSpec)super.visitStreamSpec(spec);

        // Only visit filters.
        if (spec.getType() != StreamSpec.STREAM_FILTER)
            return spec;

        List fns = new ArrayList(spec.getFuncs());
        StreamType st = spec.getStreamType();
        List newStmts = new ArrayList();
        newStmts.add(new StmtSetTypes(spec.getContext(), st));
        translateWork((FuncWork)findWork(fns, true), true, newStmts);
        translateWork((FuncWork)findWork(fns, false), false, newStmts);
        translateHelpers(spec.getHelperIOFuncs(), newStmts);
        fns = replaceInitWithPrepended(spec.getContext(), fns, newStmts);
        
        return new StreamSpec(spec.getContext(), spec.getType(),
                              spec.getStreamType(), spec.getName(),
                              spec.getParams(), spec.getVars(), fns);
    }

    private void translateWork(FuncWork work, boolean init, List newStmts)
    {
        // Do nothing if we didn't actually find the function.
        if (work == null)
            return;
        newStmts.add(new StmtIODecl(work.getContext(), init, work));
    }

    private void translateHelpers(List<Function> helpers, List newStmts)
    {
        for (Iterator<Function> iter = helpers.iterator(); iter.hasNext(); ) {
            Function helper = iter.next();
            newStmts.add(new StmtIODecl(helper.getContext(), helper));
        }
    }

}
