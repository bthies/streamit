/*
 * ExprVar.java: a named variable reference
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprVar.java,v 1.1 2002-07-10 18:03:31 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A name-indexed variable reference.  In "i++", it's the "i".
 * The exact meaning of this depends on the scope in which it exists;
 * some external analysis is needed to disambiguate variables and
 * determine the types of variables.
 */
public class ExprVar extends Expression
{
    private String name;
    
    /** Create a new ExprVar for a particular named variable. */
    public ExprVar(String name)
    {
        this.name = name;
    }
    
    /** Return the name of the variable referenced. */
    public String getName() { return name; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprVar(this);
    }
}
