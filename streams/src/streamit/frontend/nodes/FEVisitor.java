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
 * Visitor interface for StreamIt front-end nodes.  This class
 * implements part of the "visitor" design pattern for StreamIt
 * front-end nodes.  The pattern basically exchanges type structures
 * for function calls, so a different function in the visitor is
 * called depending on the run-time type of the object being visited.
 * Calling visitor methods returns some value, the type of which
 * depends on the semantics of the visitor in question.  In general,
 * you will create a visitor object, and then pass it to the
 * <code>FENode.accept()</code> method of the object in question.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: FEVisitor.java,v 1.13 2003-10-09 19:50:59 dmaze Exp $
 */
public interface FEVisitor
{
    public Object visitExprArray(ExprArray exp);
    public Object visitExprBinary(ExprBinary exp);
    public Object visitExprComplex(ExprComplex exp);
    public Object visitExprConstBoolean(ExprConstBoolean exp);
    public Object visitExprConstChar(ExprConstChar exp);
    public Object visitExprConstFloat(ExprConstFloat exp);
    public Object visitExprConstInt(ExprConstInt exp);
    public Object visitExprConstStr(ExprConstStr exp);
    public Object visitExprField(ExprField exp);
    public Object visitExprFunCall(ExprFunCall exp);
    public Object visitExprPeek(ExprPeek exp);
    public Object visitExprPop(ExprPop exp);
    public Object visitExprTernary(ExprTernary exp);
    public Object visitExprTypeCast(ExprTypeCast exp);
    public Object visitExprUnary(ExprUnary exp);
    public Object visitExprVar(ExprVar exp);
    public Object visitFieldDecl(FieldDecl field);
    public Object visitFunction(Function func);
    public Object visitFuncWork(FuncWork func);
    public Object visitProgram(Program prog);
    public Object visitSCAnon(SCAnon creator);
    public Object visitSCSimple(SCSimple creator);
    public Object visitSJDuplicate(SJDuplicate sj);
    public Object visitSJRoundRobin(SJRoundRobin sj);
    public Object visitSJWeightedRR(SJWeightedRR sj);
    public Object visitStmtAdd(StmtAdd stmt);
    public Object visitStmtAssign(StmtAssign stmt);
    public Object visitStmtBlock(StmtBlock stmt);
    public Object visitStmtBody(StmtBody stmt);
    public Object visitStmtBreak(StmtBreak stmt);
    public Object visitStmtContinue(StmtContinue stmt);
    public Object visitStmtDoWhile(StmtDoWhile stmt);
    public Object visitStmtEnqueue(StmtEnqueue stmt);
    public Object visitStmtExpr(StmtExpr stmt);
    public Object visitStmtFor(StmtFor stmt);
    public Object visitStmtIfThen(StmtIfThen stmt);
    public Object visitStmtJoin(StmtJoin stmt);
    public Object visitStmtLoop(StmtLoop stmt);
    public Object visitStmtPhase(StmtPhase stmt);
    public Object visitStmtPush(StmtPush stmt);
    public Object visitStmtReturn(StmtReturn stmt);
    public Object visitStmtSendMessage(StmtSendMessage stmt);
    public Object visitStmtSplit(StmtSplit stmt);
    public Object visitStmtVarDecl(StmtVarDecl stmt);
    public Object visitStmtWhile(StmtWhile stmt);
    public Object visitStreamSpec(StreamSpec spec);
    public Object visitStreamType(StreamType type);
    public Object visitOther(FENode node);
}
