/*
 * InsertIODecls.java: insert rate declarations in init functions
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: InsertIODecls.java,v 1.1 2002-09-23 17:00:11 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Insert I/O rate declarations into filters' init functions.
 */
public class InsertIODecls extends InitMunger
{
    // Look for a work function.  Preferentially return an anonymous
    // work function, if one exists; otherwise, use the first one we
    // find.  Return null if there is no work function in fns.
    public static Function findWork(List fns)
    {
        Function work = null;
        for (Iterator iter = fns.iterator(); iter.hasNext(); )
        {
            Function fn = (Function)iter.next();
            if (fn.getCls() == Function.FUNC_WORK)
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
        // Only visit filters.
        if (spec.getType() != StreamSpec.STREAM_FILTER)
            return spec;
        List fns = new ArrayList(spec.getFuncs());
        // Assert that this class cast works.
        FuncWork work = (FuncWork)findWork(fns);
        StreamType st = spec.getStreamType();
        List newStmts = new ArrayList();
        if (!(st.getIn() instanceof TypePrimitive) ||
            ((TypePrimitive)st.getIn()).getType() != TypePrimitive.TYPE_VOID)
        {
            newStmts.add(new StmtIODecl(work.getContext(), "input",
                                        st.getIn(), work.getPopRate(),
                                        work.getPeekRate()));
        }
        if (!(st.getOut() instanceof TypePrimitive) ||
            ((TypePrimitive)st.getOut()).getType() != TypePrimitive.TYPE_VOID)
        {
            newStmts.add(new StmtIODecl(work.getContext(), "output",
                                        st.getOut(), work.getPushRate()));
        }
        if (newStmts.isEmpty())
            return spec;
        
        fns = replaceInitWithPrepended(spec.getContext(), fns, newStmts);
        
        return new StreamSpec(spec.getContext(), spec.getType(),
                              spec.getStreamType(), spec.getName(),
                              spec.getParams(), spec.getVars(), fns);
    }
}
