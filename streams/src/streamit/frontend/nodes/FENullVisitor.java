/*
 * FENullVisitor.java: visitor implementation that always returns null
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: FENullVisitor.java,v 1.7 2003-07-07 18:59:49 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * FENullVisitor is an implementation of FEVisitor that always returns
 * null.  It is intended to be a base class for other visitors that
 * only visit a subset of the node tree, and don't want to return
 * objects of the same type as the parameter.  FEReplacer is a better
 * default for transformations on the tree.
 */
public class FENullVisitor implements FEVisitor
{
    public Object visitExprArray(ExprArray exp) { return null; }
    public Object visitExprBinary(ExprBinary exp) { return null; }
    public Object visitExprComplex(ExprComplex exp) { return null; }
    public Object visitExprConstBoolean(ExprConstBoolean exp) { return null; }
    public Object visitExprConstChar(ExprConstChar exp) { return null; }
    public Object visitExprConstFloat(ExprConstFloat exp) { return null; }
    public Object visitExprConstInt(ExprConstInt exp) { return null; }
    public Object visitExprConstStr(ExprConstStr exp) { return null; }
    public Object visitExprField(ExprField exp) { return null; }
    public Object visitExprFunCall(ExprFunCall exp) { return null; }
    public Object visitExprPeek(ExprPeek exp) { return null; }
    public Object visitExprPop(ExprPop exp) { return null; }
    public Object visitExprTernary(ExprTernary exp) { return null; }
    public Object visitExprTypeCast(ExprTypeCast exp) { return null; }
    public Object visitExprUnary(ExprUnary exp) { return null; }
    public Object visitExprVar(ExprVar exp) { return null; }
    public Object visitFieldDecl(FieldDecl field) { return null; }
    public Object visitFunction(Function func) { return null; }
    public Object visitFuncWork(FuncWork func) { return null; }
    public Object visitProgram(Program prog) { return null; }
    public Object visitSCAnon(SCAnon creator) { return null; }
    public Object visitSCSimple(SCSimple creator) { return null; }
    public Object visitSJDuplicate(SJDuplicate sj) { return null; }
    public Object visitSJRoundRobin(SJRoundRobin sj) { return null; }
    public Object visitSJWeightedRR(SJWeightedRR sj) { return null; }
    public Object visitStmtAdd(StmtAdd stmt) { return null; }
    public Object visitStmtAssign(StmtAssign stmt) { return null; }
    public Object visitStmtBlock(StmtBlock stmt) { return null; }
    public Object visitStmtBody(StmtBody stmt) { return null; }
    public Object visitStmtBreak(StmtBreak stmt) { return null; }
    public Object visitStmtContinue(StmtContinue stmt) { return null; }
    public Object visitStmtDoWhile(StmtDoWhile stmt) { return null; }
    public Object visitStmtEnqueue(StmtEnqueue stmt) { return null; }
    public Object visitStmtExpr(StmtExpr stmt) { return null; }
    public Object visitStmtFor(StmtFor stmt) { return null; }
    public Object visitStmtIfThen(StmtIfThen stmt) { return null; }
    public Object visitStmtJoin(StmtJoin stmt) { return null; }
    public Object visitStmtLoop(StmtLoop stmt) { return null; }
    public Object visitStmtPhase(StmtPhase stmt) { return null; }
    public Object visitStmtPush(StmtPush stmt) { return null; }
    public Object visitStmtReturn(StmtReturn stmt) { return null; }
    public Object visitStmtSendMessage(StmtSendMessage stmt) { return null; }
    public Object visitStmtSplit(StmtSplit stmt) { return null; }
    public Object visitStmtVarDecl(StmtVarDecl stmt) { return null; }
    public Object visitStmtWhile(StmtWhile stmt) { return null; }
    public Object visitStreamSpec(StreamSpec spec) { return null; }
    public Object visitStreamType(StreamType type) { return null; }
    public Object visitOther(FENode node) { return null; }
}
