/*
 * StmtVarDecl.java: a variable-declaration statement
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtVarDecl.java,v 1.3 2003-05-12 21:07:58 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.Collections;
import java.util.List;

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
    private List types, names, inits;

    /**
     * Create a new variable declaration with corresponding lists of
     * types, names, and initialization values.  The three lists
     * passed in are duplicated, and may be mutated after calling this
     * constructor without changing the value of this object.  The
     * types and names must all be non-null; if a particular variable
     * is uninitialized, the corresponding initializer value may be
     * null.
     *
     * @param  context  Context indicating what line and file this
     *                  statement is created at
     * @param  types    List of <code>Type</code> of the variables
     *                  declared here
     * @param  names    List of <code>String</code> of the names of the
     *                  variables declared here
     * @param  inits    List of <code>Expression</code> (or
     *                  <code>null</code>) containing initializers of
     *                  the variables declared here
     */
    public StmtVarDecl(FEContext context, List types, List names,
                       List inits)
    {
        super(context);
        // TODO: check for validity, including types of object
        // in the lists and that all three are the same length.
        this.types = new java.util.ArrayList(types);
        this.names = new java.util.ArrayList(names);
        this.inits = new java.util.ArrayList(inits);
    }

    /**
     * Create a new variable declaration with exactly one variable
     * in it.  If the variable is uninitialized, the initializer may
     * be <code>null</code>.
     *
     * @param  context  Context indicating what line and file this
     *                  statement is created at
     * @param  type     Type of the variable
     * @param  name     Name of the variable
     * @param  init     Expression initializing the variable, or
     *                  <code>null</code> if the variable is uninitialized
     */
    public StmtVarDecl(FEContext context, Type type, String name,
                       Expression init)
    {
        this(context,
             Collections.singletonList(type),
             Collections.singletonList(name),
             Collections.singletonList(init));
    }
    
    /**
     * Get the type of the nth variable declared by this.
     *
     * @param  n  Number of variable to retrieve (zero-indexed).
     * @returns   Type of the nth variable.
     */
    public Type getType(int n)
    {
        return (Type)types.get(n);
    }

    /**
     * Get an immutable list of the types of all of the variables
     * declared by this.
     *
     * @returns  Unmodifiable list of <code>Type</code> of the
     *           variables in this
     */
    public List getTypes()
    {
        return Collections.unmodifiableList(types);
    }
    
    /**
     * Get the name of the nth variable declared by this.
     *
     * @param  n  Number of variable to retrieve (zero-indexed).
     * @returns   Name of the nth variable.
     */
    public String getName(int n)
    {
        return (String)names.get(n);
    }
    
    /**
     * Get an immutable list of the names of all of the variables
     * declared by this.
     *
     * @returns  Unmodifiable list of <code>String</code> of the
     *           names of the variables in this
     */
    public List getNames()
    {
        return Collections.unmodifiableList(names);
    }
    
    /**
     * Get the initializer of the nth variable declared by this.
     *
     * @param  n  Number of variable to retrieve (zero-indexed).
     * @returns   Expression initializing the nth variable, or
     *            <code>null</code> if the variable is
     *            uninitialized.
     */
    public Expression getInit(int n)
    {
        return (Expression)inits.get(n);
    }
    
    /**
     * Get an immutable list of the initializers of all of the
     * variables declared by this.  Members of the list may be
     * <code>null</code> if a particular variable is uninitialized.
     *
     * @returns  Unmodifiable list of <code>Expression</code> (or
     *           <code>null</code>) of the initializers of the
     *           variables in this
     */
    public List getInits()
    {
        return Collections.unmodifiableList(inits);
    }
    
    /**
     * Get the number of variables declared by this.  This value should
     * always be at least 1.
     *
     * @returns  Number of variables declared
     */
    public int getNumVars()
    {
        // CLAIM: the three lists have the same length.
        return types.size();
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtVarDecl(this);
    }
}
