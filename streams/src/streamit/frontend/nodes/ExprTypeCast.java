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

package streamit.frontend.nodes;

/**
 * An expression directing one expression to be interpreted as a different
 * (primitive) type.  This has a child instruction and the type that is
 * being cast to.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprTypeCast.java,v 1.2 2003-10-09 19:50:59 dmaze Exp $
 */
public class ExprTypeCast extends Expression
{
    private Type type;
    private Expression expr;
    
    /**
     * Create a new ExprTypeCast with a specified type and child
     * expression.
     *
     * @param context  Context indicating file and line number
     *                 this expression was created in
     * @param type     Type the expression is being cast to
     * @param expr     Expression being cast
     */
    public ExprTypeCast(FEContext context, Type type, Expression expr)
    {
        super(context);
        this.type = type;
        this.expr = expr;
    }
    
    /**
     * Get the type the expression is being cast to.
     *
     * @return  Type the expression is cast to
     */
    public Type getType()
    {
        return type;
    }
    
    /**
     * Get the expression being cast.
     *
     * @return  The expression being cast
     */
    public Expression getExpr()
    {
        return expr;
    }
    
    /**
     * Accept a front-end visitor.
     */
    public Object accept(FEVisitor v)
    {
        return v.visitExprTypeCast(this);
    }
}

