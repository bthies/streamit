/*
 * StmtJavaConstructor.java: IR node for constructors
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtJavaConstructor.java,v 1.1 2002-09-20 17:08:45 dmaze Exp $
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
    private String varName;
    private Type type;
    
    public StmtJavaConstructor(FEContext context, String varName, Type type)
    {
        super(context);
        this.varName = varName;
        this.type = type;
    }
    
    public String getVarName()
    {
        return varName;
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

    
