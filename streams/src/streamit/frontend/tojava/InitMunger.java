/*
 * InitMunger.java: base class for visitors that add init statements
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: InitMunger.java,v 1.2 2002-09-23 16:05:11 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Base class for visitors that add statements to classes' init functions.
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
