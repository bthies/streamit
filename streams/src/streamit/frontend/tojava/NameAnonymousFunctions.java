/*
 * NameAnonymousFunctions.java: replace anonymous functions with named ones
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: NameAnonymousFunctions.java,v 1.1 2002-09-12 15:54:31 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

public class NameAnonymousFunctions extends FEReplacer
{
    public Object visitFunction(Function func)
    {
        if (func.getName() != null) return func;
        String name = null;
        switch(func.getCls())
        {
        case Function.FUNC_INIT: name = "init"; break;
        case Function.FUNC_WORK: name = "work"; break;
        case Function.FUNC_HANDLER: return func;
        case Function.FUNC_HELPER: return func;
        default: return func;
        }
        return new Function(func.getContext(), func.getCls(),
                            name, func.getReturnType(),
                            func.getParams(), func.getBody());
    }
    
    public Object visitFuncWork(FuncWork func)
    {
        if (func.getName() != null) return func;
        return new FuncWork(func.getContext(), "work",
                            func.getBody(), func.getPeekRate(),
                            func.getPopRate(), func.getPushRate());
    }
}
