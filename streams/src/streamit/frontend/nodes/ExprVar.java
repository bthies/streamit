package streamit.frontend.nodes;

/**
 * A name-indexed variable reference.  In <code>i++</code>, it's the
 * <code>i</code>.  The exact meaning of this depends on the scope in
 * which it exists; some external analysis is needed to disambiguate
 * variables and determine the types of variables.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprVar.java,v 1.4 2003-07-30 19:58:33 dmaze Exp $
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
