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
 * Pass to replace anonymous functions with named ones.  This assigns
 * names to functions without explicit names in the syntax, such as
 * work and init functions.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: NameAnonymousFunctions.java,v 1.6 2003-10-09 19:51:02 dmaze Exp $
 */
public class NameAnonymousFunctions extends FEReplacer
{
    public Object visitFunction(Function func)
    {
        func = (Function)super.visitFunction(func);
        if (func.getName() != null) return func;
        String name = null;
        switch(func.getCls())
        {
        case Function.FUNC_INIT: name = "init"; break;
        case Function.FUNC_WORK: name = "work"; break;
        case Function.FUNC_PREWORK: name = "prework"; break;
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
        func = (FuncWork)super.visitFuncWork(func);
        if (func.getName() != null) return func;
	String name = null;
        switch(func.getCls())
        {
        case Function.FUNC_WORK: name = "work"; break;
        case Function.FUNC_PREWORK: name = "prework"; break;
        default: return func;
        }
        return new FuncWork(func.getContext(), func.getCls(), name,
                            func.getBody(), func.getPeekRate(),
                            func.getPopRate(), func.getPushRate());
    }
}
