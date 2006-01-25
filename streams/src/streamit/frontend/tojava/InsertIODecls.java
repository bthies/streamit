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
 * @version $Id: InsertIODecls.java,v 1.12 2006-01-25 17:04:30 thies Exp $
 */
public class InsertIODecls extends InitMunger
{
    private boolean libraryFormat;
    
    /**
     * Creates a new pass object.  If <code>library</code> is true,
     * create code suitable for the Java library.  This affects the
     * output of phased filters; a filter's work function must either
     * declare I/O rates or consist entirely of phase calls for
     * library-form output.
     *
     * @param library  Generate code for the Java library
     */
    public InsertIODecls(boolean library)
    {
        libraryFormat = library;
    }

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
        translateWork(spec, (FuncWork)findWork(fns, true), true, newStmts);
        translateWork(spec, (FuncWork)findWork(fns, false), false, newStmts);
        fns = replaceInitWithPrepended(spec.getContext(), fns, newStmts);
        
        return new StreamSpec(spec.getContext(), spec.getType(),
                              spec.getStreamType(), spec.getName(),
                              spec.getParams(), spec.getVars(), fns);
    }

    private void translateWork(StreamSpec spec, FuncWork work,
                               boolean init, List newStmts)
    {
        // Do nothing if we didn't actually find the function.
        if (work == null)
            return;

        final List phaseList = new ArrayList(); 
        final StreamSpec specfinal = spec;
        {
            StmtBlock body = (StmtBlock) work.getBody();
            body.accept(new FEReplacer() {
                    public Object visitExprFunCall(ExprFunCall exp) {
                        Function aFunction = specfinal.getFuncNamed(exp.getName());
                        if (aFunction != null && aFunction.getCls() == Function.FUNC_PHASE) {
                            phaseList.add(aFunction);
                        }
                        return exp;
                    }
                });    
        }

        // We need to add phases.  Is this a phased filter?
        if (phaseList.size() > 0)
            {
                // This will ignore control flow in the work function which may cause
                // problems outside of the library.
                for (Iterator iter = phaseList.iterator(); iter.hasNext();) {
                    newStmts.add(new StmtAddPhase(work.getContext(), init, (FuncWork) iter.next())); 
                }
            }
        // Also add the work function as a phase.  Except in the
        // library, for now (which only evaluates work() in phased
        // filters in -nosched mode, as of 8/05.  This should change.)
        if (!libraryFormat || phaseList.size()==0) {
            newStmts.add(new StmtAddPhase(work.getContext(), init, work));
        }
    }
}
