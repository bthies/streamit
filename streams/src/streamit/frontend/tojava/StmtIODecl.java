/*
 * StmtIODecl.java: an old-syntax I/O rate declaration
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtIODecl.java,v 1.2 2002-09-23 16:32:54 dmaze Exp $
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
    private Expression rate1, rate2;
    
    public StmtIODecl(FEContext context, String name, Type type,
                      Expression rate1, Expression rate2)
    {
        super(context);
        this.name = name;
        this.type = type;
        this.rate1 = rate1;
        this.rate2 = rate2;
    }
    
    public StmtIODecl(FEContext context, String name, Type type,
                      Expression rate)
    {
        this(context, name, type, rate, null);
    }
    
    public String getName()
    {
        return name;
    }
    
    public Type getType()
    {
        return type;
    }
    
    public Expression getRate1()
    {
        return rate1;
    }
    
    public Expression getRate2()
    {
        return rate2;
    }
    
    public Object accept(FEVisitor v)
    {
        return v.visitOther(this);
    }
}
