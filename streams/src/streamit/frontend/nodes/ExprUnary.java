/*
 * ExprUnary.java: a unary expression
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprUnary.java,v 1.1 2002-07-10 18:03:31 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A unary expression.  This is a child expression with a modifier;
 * it could be "!a" or "-b" or "c++".
 */
public class ExprUnary extends Expression
{
    // Operators:
    public static final int UNOP_NOT = 1;
    public static final int UNOP_NEG = 2;
    public static final int UNOP_PREINC = 3;
    public static final int UNOP_POSTINC = 4;
    public static final int UNOP_PREDEC = 5;
    public static final int UNOP_POSTDEC = 6;
    
    private int op;
    private Expression expr;
    
    /** Creates a new ExprUnary applying the specified operator to the
     * specified expression. */
    public ExprUnary(int op, Expression expr)
    {
        this.op = op;
        this.expr = expr;
    }
    
    /** Returns the operator of this. */
    public int getOp() { return op; }
    
    /** Returns the expression this modifies. */
    public Expression getExpr() { return expr; }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprUnary(this);
    }
}
