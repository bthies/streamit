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
 * An old-syntax I/O rate declaration.  This has a name, a type, and one
 * or two integer rates.  It gets translated to a statement like
 * <pre>
 * name = new Channel(type.class, rate1, rate2);
 * </pre>
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtIODecl.java,v 1.4 2003-10-09 19:51:02 dmaze Exp $
 */
public class StmtIODecl extends streamit.frontend.nodes.Statement
{
    private String name;
    private Type type;
    private Expression rate1, rate2;
    
    /**
     * Creates a new I/O rate declaration.  The name should be either
     * "input" or "output".  If it is "output", <code>rate1</code> is the
     * push rate.  If the name is "input", <code>rate1</code> is the
     * pop rate, and <code>rate2</code> is the peek rate.  The second rate
     * can be <code>null</code> if it is unnecessary or it is equal to the
     * first rate.
     *
     * @param context  Context this statement appears in
     * @param name     Name of the channel
     * @param type     Type of objects on this channel
     * @param rate1    Push or pop rate
     * @param rate2    Peek rate or null
     */
    public StmtIODecl(FEContext context, String name, Type type,
                      Expression rate1, Expression rate2)
    {
        super(context);
        this.name = name;
        this.type = type;
        this.rate1 = rate1;
        this.rate2 = rate2;
    }
    
    /**
     * Creates a new I/O rate declaration with a single rate.
     *
     * @param context  Context this statement appears in
     * @param name     Name of the channel
     * @param type     Type of objects on this channel
     * @param rate     Push or pop rate
     */
    public StmtIODecl(FEContext context, String name, Type type,
                      Expression rate)
    {
        this(context, name, type, rate, null);
    }
    
    /** Returns the name of the channel being declared. */
    public String getName()
    {
        return name;
    }
    
    /** Returns the type of items on the channel. */
    public Type getType()
    {
        return type;
    }
    
    /** Returns the pop or push rate of the stream. */
    public Expression getRate1()
    {
        return rate1;
    }
    
    /** Returns the peek rate of the stream.  Returns null if this is an
     * output channel or if the peek rate is undeclared and implicitly
     * equal to the pop rate. */
    public Expression getRate2()
    {
        return rate2;
    }
    
    public Object accept(FEVisitor v)
    {
        return v.visitOther(this);
    }
}
