package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

/**
 * A Java constructor expression.  This appears in this package
 * because it is only used for frontend to Java conversion; specifically,
 * it needs to appear at the front of init functions, before anything
 * can use variables that need to have constructors.  This is just
 * a Java 'new' expression for some single type.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprJavaConstructor.java,v 1.1 2003-08-26 19:31:41 dmaze Exp $
 */
class ExprJavaConstructor extends Expression
{
    private Type type;
    
    public ExprJavaConstructor(FEContext context, Type type)
    {
        super(context);
        this.type = type;
    }
    
    public Type getType()
    {
        return type;
    }
    
    public Object accept(FEVisitor v)
    {
        return v.visitOther(this);
    }
}

    
