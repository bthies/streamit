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
 * Implementation of FEVisitor that always returns <code>null</code>.
 * This is intended to be a base class for other visitors that only
 * visit a subset of the node tree, and don't want to return objects
 * of the same type as the parameter.  {@link
 * streamit.frontend.nodes.FEReplacer} is a better default for
 * transformations on the tree.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: FENullVisitor.java,v 1.10 2004-07-08 05:45:35 thies Exp $
 */
public class FENullVisitor implements FEVisitor
{
    public Object visitExprArray(ExprArray exp) { return null; }
    public Object visitExprArrayInit(ExprArrayInit exp) { return null; }
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
    public Object visitStmtEmpty(StmtEmpty stmt) { return null; }
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
