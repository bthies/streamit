package streamit.frontend.nodes;

import java.util.List;
import java.util.Collections;

/**
 * Declaration of a set of fields in a filter or structure.  This
 * describes the declaration of a list of variables, each of which has
 * a type, a name, and an optional initialization value.  This is
 * explicitly not a <code>Statement</code>; declarations that occur
 * inside functions are local variable declarations, not field
 * declarations.  Similarly, this is not a stream parameter (in
 * StreamIt code; it may be in Java code).
 *
 * @see     StmtVarDecl
 * @see     Parameter
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: FieldDecl.java,v 1.4 2003-07-24 16:58:37 dmaze Exp $
 */
public class FieldDecl extends FENode
{
    private List types, names, inits;

    /**
     * Create a new field declaration with corresponding lists of
     * types, names, and initialization values.  The three lists
     * passed in are duplicated, and may be mutated after calling this
     * constructor without changing the value of this object.  The
     * types and names must all be non-null; if a particular field is
     * uninitialized, the corresponding initializer value may be null.
     *
     * @param  context  Context indicating what line and file this
     *                  field is created at
     * @param  types    List of <code>Type</code> of the fields
     *                  declared here
     * @param  names    List of <code>String</code> of the names of the
     *                  fields declared here
     * @param  inits    List of <code>Expression</code> (or
     *                  <code>null</code>) containing initializers of
     *                  the fields declared here
     */
    public FieldDecl(FEContext context, List types, List names,
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
     * Create a new field declaration with exactly one variable in it.
     * If the field is uninitialized, the initializer may be
     * <code>null</code>.
     *
     * @param  context  Context indicating what line and file this
     *                  field is created at
     * @param  type     Type of the field
     * @param  name     Name of the field
     * @param  init     Expression initializing the field, or
     *                  <code>null</code> if the field is uninitialized
     */
    public FieldDecl(FEContext context, Type type, String name,
                     Expression init)
    {
        this(context,
             Collections.singletonList(type),
             Collections.singletonList(name),
             Collections.singletonList(init));
    }
    
    /**
     * Get the type of the nth field declared by this.
     *
     * @param  n  Number of field to retrieve (zero-indexed)
     * @return    Type of the nth field
     */
    public Type getType(int n)
    {
        return (Type)types.get(n);
    }

    /**
     * Get an immutable list of the types of all of the fields
     * declared by this.
     *
     * @return  Unmodifiable list of <code>Type</code> of the
     *          fields in this
     */
    public List getTypes()
    {
        return Collections.unmodifiableList(types);
    }
    
    /**
     * Get the name of the nth field declared by this.
     *
     * @param  n  Number of field to retrieve (zero-indexed)
     * @return    Name of the nth field
     */
    public String getName(int n)
    {
        return (String)names.get(n);
    }
    
    /**
     * Get an immutable list of the names of all of the fields
     * declared by this.
     *
     * @return  Unmodifiable list of <code>String</code> of the
     *          names of the fields in this
     */
    public List getNames()
    {
        return Collections.unmodifiableList(names);
    }
    
    /**
     * Get the initializer of the nth field declared by this.
     *
     * @param  n  Number of field to retrieve (zero-indexed)
     * @return    Expression initializing the nth field, or
     *            <code>null</code> if the field is
     *            uninitialized
     */
    public Expression getInit(int n)
    {
        return (Expression)inits.get(n);
    }
    
    /**
     * Get an immutable list of the initializers of all of the field
     * declared by this.  Members of the list may be <code>null</code>
     * if a particular field is uninitialized.
     *
     * @return  Unmodifiable list of <code>Expression</code> (or
     *          <code>null</code>) of the initializers of the
     *          fields in this
     */
    public List getInits()
    {
        return Collections.unmodifiableList(inits);
    }
    
    /**
     * Get the number of fields declared by this.  This value should
     * always be at least 1.
     *
     * @return  Number of fields declared
     */
    public int getNumFields()
    {
        // CLAIM: the three lists have the same length.
        return types.size();
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitFieldDecl(this);
    }
}
