/*
 * StmtJavaConstructor.java: IR node for constructors
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtJavaConstructor.java,v 1.2 2003-06-25 15:18:38 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

/**
 * IR node for Java constructor statements.  This appears in this package
 * because it is only used for frontend to Java conversion; specifically,
 * it needs to appear at the front of init functions, before anything
 * can use variables that need to have constructors.  The constructor
 * applies to a single variable, whose type can be looked up in a
 * symbol table.  It creates a statement like
 *
 *   varName = new TypeOfVarName();
 */
class StmtJavaConstructor extends streamit.frontend.nodes.Statement
{
    private Expression lhs;
    private Type type;
    
    public StmtJavaConstructor(FEContext context, Expression lhs, Type type)
    {
        super(context);
        this.lhs = lhs;
        this.type = type;
    }
    
    public Expression getLHS()
    {
        return lhs;
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

    
