/*
 * StmtExpr.java: a statement just containing an expression
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtExpr.java,v 1.3 2003-06-24 21:40:14 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A statement containing only an expression.  This is generally
 * evaluated only for its side effects; a typical such statement
 * might be 'x++;' or 'pop();'.
 */
public class StmtExpr extends Statement
{
    private Expression expr;
    
    public StmtExpr(FEContext context, Expression expr)
    {
        super(context);
        this.expr = expr;
    }
    
    /**
     * Create an expression statement corresponding to a single expression,
     * using that expression's context as our own.
     */
    public StmtExpr(Expression expr)
    {
        this(expr.getContext(), expr);
    }

    public Expression getExpression()
    {
        return expr;
    }
    
    public Object accept(FEVisitor v)
    {
        return v.visitStmtExpr(this);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof StmtExpr))
            return false;
        return expr.equals(((StmtExpr)other).getExpression());
    }
    
    public int hashCode()
    {
        return expr.hashCode();
    }
    
    public String toString()
    {
        return expr.toString();
    }
}

