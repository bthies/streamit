/*
 * ExprTernary.java: a ternary expression
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprTernary.java,v 1.2 2002-08-20 20:04:28 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A ternary expression; that is, one with three children.  C and Java
 * have exactly one of these, which is the conditional expression
 * (a ? b : c), which is an expression equivalent of "if (a) b else c".
 */
public class ExprTernary extends Expression
{
    // Operators: (for consistency, really)
    public static final int TEROP_COND = 1;
    
    private int op;
    private Expression a, b, c;
    
    /** Creates a new ExprTernary with the specified operation and
     * child expressions. */
    public ExprTernary(FEContext context,
                       int op, Expression a, Expression b, Expression c)
    {
        super(context);
        this.op = op;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    /** Returns the operation of this. */
    public int getOp() { return op; }

    /** Returns the first child of this. */
    public Expression getA() { return a; }

    /** Returns the second child of this. */
    public Expression getB() { return b; }

    /** Returns the third child of this. */
    public Expression getC() { return c; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprTernary(this);
    }
}
