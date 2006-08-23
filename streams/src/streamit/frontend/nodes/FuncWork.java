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

package streamit.frontend.nodes;

import java.util.Collections;

/**
 * A StreamIt work or prework function.  A work function always
 * returns void and may or may not have a name.  It takes no
 * parameters.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: FuncWork.java,v 1.7 2006-08-23 23:01:08 thies Exp $
 */
public class FuncWork extends Function
{
    /** Creates a new work function given its name (or null), body,
     * and I/O rates.*/
    public FuncWork(FEContext context, int cls,
                    String name, Statement body,
                    Expression peek, Expression pop, Expression push)
    {
        super(context, cls, name,
              new TypePrimitive(TypePrimitive.TYPE_VOID),
              Collections.EMPTY_LIST, body,
              peek, pop, push);
    }

    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitFuncWork(this);
    }
}

