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
 * Base class for visitors that add statements to classes' init functions.
 * This provides functions to locate an init function in a set of
 * functions, and to add statements to the start of a stream object's
 * init function.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: InitMunger.java,v 1.3 2003-10-09 19:51:02 dmaze Exp $
 */
abstract public class InitMunger extends FEReplacer
{
    public static Function findInit(FEContext context, List fns)
    {
        for (Iterator iter = fns.iterator(); iter.hasNext(); )
        {
            Function fn = (Function)iter.next();
            if (fn.getCls() == Function.FUNC_INIT)
                return fn;
        }
        
        // No init function; create an empty one.
        return Function.newInit(context,
                                new StmtBlock(context,
                                              Collections.EMPTY_LIST));
    }

    // Finds an init function in fns, or creates one using context.
    // Removes it from fns, and replaces it with an equivalent function
    // with stmts at the start of its body.  Returns fns.
    public static List replaceInitWithPrepended(FEContext context,
                                                List fns, List stmts)
    {
        Function init = findInit(context, fns);
        fns.remove(init);
        StmtBlock oldBody = (StmtBlock)init.getBody();
        List newStmts = new ArrayList(stmts);
        newStmts.addAll(oldBody.getStmts());
        Statement newBody = new StmtBlock(oldBody.getContext(), newStmts);
        init = new Function(init.getContext(), init.getCls(),
                            init.getName(), init.getReturnType(),
                            init.getParams(), newBody);
        fns.add(init);
        return fns;
    }
}
