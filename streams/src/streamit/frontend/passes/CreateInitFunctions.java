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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Create init functions in filters that do not have them.  Later passes
 * can require every stream to have an init function; filters are not
 * required to have one in the syntax, though.  This pass adds empty
 * init functions to filters without any.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: CreateInitFunctions.java,v 1.2 2003-10-09 19:51:01 dmaze Exp $
 */
public class CreateInitFunctions extends FEReplacer
{
    public Object visitStreamSpec(StreamSpec ss)
    {
        boolean hasInit = false;
        for (Iterator iter = ss.getFuncs().iterator(); iter.hasNext(); )
        {
            Function func = (Function)iter.next();
            if (func.getCls() == Function.FUNC_INIT)
                hasInit = true;
        }
        if (!hasInit)
        {
            List newFuncs = new java.util.ArrayList();
            newFuncs.addAll(ss.getFuncs());
            Statement body = new StmtBlock(ss.getContext(),
                                           Collections.EMPTY_LIST);
            newFuncs.add(Function.newInit(ss.getContext(), body));
            ss = new StreamSpec(ss.getContext(), ss.getType(),
                                ss.getStreamType(), ss.getName(),
                                ss.getParams(), ss.getVars(), newFuncs);
        }
        // might have anonymous child filters with no init functions
        return super.visitStreamSpec(ss);
    }
}
