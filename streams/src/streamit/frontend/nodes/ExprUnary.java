/*
 * ExprUnary.java: a unary expression
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprUnary.java,v 1.3 2003-06-24 21:40:14 dmaze Exp $
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
    public ExprUnary(FEContext context, int op, Expression expr)
    {
        super(context);
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

    public boolean equals(Object other)
    {
        if (!(other instanceof ExprUnary))
            return false;
        ExprUnary eu = (ExprUnary)other;
        if (op != eu.getOp())
            return false;
        if (!(expr.equals(eu.getExpr())))
            return false;
        return true;
    }
    
    public int hashCode()
    {
        return new Integer(op).hashCode() ^ expr.hashCode();
    }
    
    public String toString()
    {
        String preOp = "", postOp = "";
        switch(op)
        {
        case UNOP_NOT: preOp = "!"; break;
        case UNOP_NEG: preOp = "-"; break;
        case UNOP_PREINC: preOp = "++"; break;
        case UNOP_POSTINC: postOp = "++"; break;
        case UNOP_PREDEC: preOp = "--"; break;
        case UNOP_POSTDEC: postOp = "--"; break;
        default: preOp = "?(" + op + ")"; break;
        }
        return preOp + "(" + expr.toString() + ")" + postOp;
    }
}
