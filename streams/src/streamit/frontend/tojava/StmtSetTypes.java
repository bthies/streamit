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
 * A new-old-syntax <code>setIOTypes</code> statement.  This statement
 * is used by the Java library to declare the I/O types for the Java
 * library.  It has an input and output type; the statement translates
 * into something like
 * <pre>
 * setIOTypes(Float.TYPE, Float.TYPE);
 * </pre>
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtSetTypes.java,v 1.2 2003-10-09 19:51:02 dmaze Exp $
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
