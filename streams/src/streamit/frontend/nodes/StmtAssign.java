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
 * A statement that assigns a value to an expression.  This has a
 * left-hand side and a right-hand side.  The right-hand side can be
 * any expression; the left-hand side must be a composition of variable
 * references, field references, and array references.  The left-hand
 * side is not evaluated for its value.  Example statements might be
 * 'a=b;', 'p=pop();', and 'c[k].imag=peek(3)+peek(4)*2;'.  This may
 * also be an assignment that uses the current value of the left-hand
 * side, such as 'a+=b;', equivalent to 'a=a+b;'.  In this case, the
 * constants from ExprBinary are used to specify a binary operation to
 * be performed on both sides.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtAssign.java,v 1.4 2003-10-09 19:51:00 dmaze Exp $
 */
public class StmtAssign extends Statement
{
    private Expression lhs, rhs;
    private int op;
    
    /** Creates a new assignment statement with the specified left-
     * and right-hand sides and operation (0 for none). */
    public StmtAssign(FEContext context, Expression lhs, Expression rhs,
                      int op)
    {
        super(context);
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = op;
    }

    /** Creates a new assignment statement with the specified left-
     * and right-hand sides and no operation (i.e., 'lhs=rhs;'). */
    public StmtAssign(FEContext context, Expression lhs, Expression rhs)
    {
        this(context, lhs, rhs, 0);
    }
    
    /** Returns the left-hand side of this. */
    public Expression getLHS()
    {
        return lhs;
    }
    
    /** Returns the right-hand side of this. */
    public Expression getRHS()
    {
        return rhs;
    }
    
    /** Returns the operation for this.  This will be one of the constants
     * in ExprBinary or 0 if this is a simple assignment. */
    public int getOp()
    {
        return op;
    }

    public Object accept(FEVisitor v)
    {
        return v.visitStmtAssign(this);
    }

    public String toString()
    {
        String theOp;
        switch (op)
        {
        case 0: theOp = "="; break;
        case ExprBinary.BINOP_ADD: theOp = "+="; break;
        case ExprBinary.BINOP_SUB: theOp = "-="; break;
        case ExprBinary.BINOP_MUL: theOp = "*="; break;
        case ExprBinary.BINOP_DIV: theOp = "/="; break;
        default: theOp = "?= (" + op + ")"; break;
        }
        return lhs + " " + theOp + " " + rhs;
    }
}
