/*
 * StmtIODecl.java: an old-syntax I/O rate declaration
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtIODecl.java,v 1.1 2002-09-20 19:03:06 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

/**
 * An old-syntax I/O rate declaration.  This has a name, a type, and one
 * or two integer rates.  It gets translated to a statement like
 *   name = new Channel(type.class, rate1, rate2);
 */
public class StmtIODecl extends streamit.frontend.nodes.Statement
{
    private String name;
    private Type type;
    private int rate1, rate2;
    
    public StmtIODecl(FEContext context, String name, Type type,
                      int rate1, int rate2)
    {
        super(context);
        this.name = name;
        this.type = type;
        this.rate1 = rate1;
        this.rate2 = rate2;
    }
    
    public StmtIODecl(FEContext context, String name, Type type, int rate)
    {
        this(context, name, type, rate, -1);
    }
    
    public String getName()
    {
        return name;
    }
    
    public Type getType()
    {
        return type;
    }
    
    public int getRate1()
    {
        return rate1;
    }
    
    public int getRate2()
    {
        return rate2;
    }
    
    public Object accept(FEVisitor v)
    {
        return v.visitOther(this);
    }
}
