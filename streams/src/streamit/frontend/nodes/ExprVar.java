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

/**
 * A name-indexed variable reference.  In <code>i++</code>, it's the
 * <code>i</code>.  The exact meaning of this depends on the scope in
 * which it exists; some external analysis is needed to disambiguate
 * variables and determine the types of variables.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprVar.java,v 1.6 2003-10-09 19:50:59 dmaze Exp $
 */
public class ExprVar extends Expression
{
    private String name;
    
    /** Create a new ExprVar for a particular named variable. */
    public ExprVar(FEContext context, String name)
    {
        super(context);
        this.name = name;
    }
    
    /** Return the name of the variable referenced. */
    public String getName() { return name; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprVar(this);
    }

    /**
     * Determine if this expression can be assigned to.  Variables can
     * generally be assigned to, particularly if they are local
     * variables.  Determining whether a variable is a (constant)
     * stream parameter is beyond the intended use of this function.
     *
     * @return always true
     */
    public boolean isLValue()
    {
        return true;
    }

    public String toString()
    {
        return name;
    }

    public int hashCode()
    {
        return name.hashCode();
    }
    
    public boolean equals(Object o)
    {
        if (!(o instanceof ExprVar))
            return false;
        return name.equals(((ExprVar)o).name);
    }
}
