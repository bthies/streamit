/*
 * FEVisitor.java: visit a tree of front-end nodes
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: FEVisitor.java,v 1.3 2002-07-11 20:58:23 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A FEVisitor implements part of the "visitor" design pattern for
 * StreamIt front-end nodes.  The pattern basically exchanges type
 * structures for function calls, so a different function in the visitor
 * is called depending on the run-time type of the object being visited.
 * Calling visitor methods returns some value, the type of which
 * depends on the semantics of the visitor in question.  In general,
 * you will create a visitor object, and then pass it to the accept()
 * method of the object in question.
 */
public interface FEVisitor
{
    public Object visitExprArray(ExprArray exp);
    public Object visitExprBinary(ExprBinary exp);
    public Object visitExprComplex(ExprComplex exp);
    public Object visitExprConstChar(ExprConstChar exp);
    public Object visitExprConstFloat(ExprConstFloat exp);
    public Object visitExprConstInt(ExprConstInt exp);
    public Object visitExprConstStr(ExprConstStr exp);
    public Object visitExprField(ExprField exp);
    public Object visitExprFunCall(ExprFunCall exp);
    public Object visitExprPeek(ExprPeek exp);
    public Object visitExprPop(ExprPop exp);
    public Object visitExprTernary(ExprTernary exp);
    public Object visitExprUnary(ExprUnary exp);
    public Object visitExprVar(ExprVar exp);
}
