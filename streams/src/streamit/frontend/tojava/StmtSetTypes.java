package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

/**
 * A new-old-syntax <code>setIOTypes</code> statement.  This statement
 * is used by the Java library to declare the I/O types for the Java
 * library.  It has an input and output type; the statement translates
 * into something like
 * <pre>
 * setIOTypes(Float.TYPE, Float.TYPE);
 * </pre>
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtSetTypes.java,v 1.1 2003-02-12 22:24:57 dmaze Exp $
 */
public class StmtSetTypes extends Statement
{
    private Type inType, outType;
    
    /**
     * Creates a new I/O type declaration from the input and output types.
     *
     * @param context  Context this statement appears in
     * @param inType   Input type of the stream
     * @param outType  Output type of the stream
     */
    public StmtSetTypes(FEContext context, Type inType, Type outType)
    {
        super(context);
        this.inType = inType;
        this.outType = outType;
    }

    /**
     * Creates a new I/O type declaration from a stream type.
     *
     * @param context  Context this statement appears in
     * @param st       I/O types of the stream
     */
    public StmtSetTypes(FEContext context, StreamType st)
    {
        super(context);
        this.inType = st.getIn();
        this.outType = st.getOut();
    }

    /** Returns the input type of the stream this declaration is in. */
    public Type getInType()
    {
        return inType;
    }
    
    /** Returns the output type of the stream this declaration is in. */
    public Type getOutType()
    {
        return outType;
    }
    
    public Object accept(FEVisitor v)
    {
        return v.visitOther(this);
    }
}
