/*
 * StmtVarDecl.java: a variable-declaration statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtVarDecl.java,v 1.2 2002-09-06 16:28:43 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * StmtVarDecl is a variable-declaration statement.  It allows you to
 * declare a single variable with a single type, possibly with an
 * initialization value.
 *
 * This should also take into account the possibility of having multiple
 * declarations within a single statement, e.g.
 *
 *   int a, b=0, c;
 */
public class StmtVarDecl extends Statement
{
    private Type type;
    private String name;
    private Expression init;
    
    /** Create a new variable declaration with a type, name, and optional
     * initialization value.  If there is no initialization, the init
     * value should be null. */
    public StmtVarDecl(FEContext context, Type type, String name,
                       Expression init)
    {
        super(context);
        this.type = type;
        this.name = name;
        this.init = init;
    }
    
    /** Get the type of variable declared by this. */
    public Type getType()
    {
        return type;
    }
    
    /** Get the name of the variable declared by this. */
    public String getName()
    {
        return name;
    }
    
    /** Get the initialization value of this, or null if there is no
     * initializer. */
    public Expression getInit()
    {
        return init;
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtVarDecl(this);
    }
}
