/*
 * FEReplacer.java: run through a front-end tree and replace nodes
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: FEReplacer.java,v 1.9 2002-09-20 15:09:55 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Replace nodes in a front-end tree.  This is a skeleton for writing
 * replacing passes, which implements FEVisitor.  On its own it does
 * nothing, but it is a convenice class for deriving your own
 * replacers from.  All of the member functions of FEReplacer return
 * objects of appropriate types (Expression subclasses return
 * Expressions; Statement subclasses return Statements; other objects
 * return their own types); an attempt is made to not create new
 * objects if they would be identical to the original objects.
 *
 * For Statements, this class also keeps a list of statements in the
 * current block.  Calling the addStatement() method will add a
 * statement to the end of the list; a statement visitor can return
 * a statement, or can call addStatement() itself and return null.
 * Derived classes should take care to only call addStatement() for
 * statements inside a block; practically, this means that any pass
 * that adds or removes statements should be called after the
 * MakeBodiesBlocks pass.
 *
 * Statements that visit Expressions call doExpression() to do
 * the actual visitation; by default, this accepts this on the
 * provided expression, but derived classes can override
 * doExpression() to perform some custom action.
 */
public class FEReplacer implements FEVisitor
{
    // No direct accessor.  If derived classes need access to this
    // more than addStatement() provides, change this to protected
    // visibility.
    private List newStatements;

    protected void addStatement(Statement stmt)
    {
        newStatements.add(stmt);
    }

    protected Expression doExpression(Expression expr)
    {
        return (Expression)expr.accept(this);
    }

    public Object visitExprArray(ExprArray exp)
    {
        Expression base = (Expression)exp.getBase().accept(this);
        Expression offset = (Expression)exp.getOffset().accept(this);
        if (base == exp.getBase() && offset == exp.getOffset())
            return exp;
        else
            return new ExprArray(exp.getContext(), base, offset);
    }
    
    public Object visitExprBinary(ExprBinary exp)
    {
        Expression left = (Expression)exp.getLeft().accept(this);
        Expression right = (Expression)exp.getRight().accept(this);
        if (left == exp.getLeft() && right == exp.getRight())
            return exp;
        else
            return new ExprBinary(exp.getContext(), exp.getOp(), left, right);
    }
    
    public Object visitExprComplex(ExprComplex exp)
    {
        Expression real = exp.getReal();
        if (real != null) real = (Expression)real.accept(this);
        Expression imag = exp.getImag();
        if (imag != null) imag = (Expression)imag.accept(this);
        if (real == exp.getReal() && imag == exp.getImag())
            return exp;
        else
            return new ExprComplex(exp.getContext(), real, imag);
    }
    
    public Object visitExprConstChar(ExprConstChar exp) { return exp; }
    public Object visitExprConstFloat(ExprConstFloat exp) { return exp; }
    public Object visitExprConstInt(ExprConstInt exp) { return exp; }
    public Object visitExprConstStr(ExprConstStr exp) { return exp; }

    public Object visitExprField(ExprField exp)
    {
        Expression left = (Expression)exp.getLeft().accept(this);
        if (left == exp.getLeft())
            return exp;
        else
            return new ExprField(exp.getContext(), left, exp.getName());
    }

    public Object visitExprFunCall(ExprFunCall exp)
    {
        boolean hasChanged = false;
        List newParams = new ArrayList();
        for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
        {
            Expression param = (Expression)iter.next();
            Expression newParam = (Expression)param.accept(this);
            newParams.add(newParam);
            if (param != newParam) hasChanged = true;
        }
        if (!hasChanged) return exp;
        return new ExprFunCall(exp.getContext(), exp.getName(), newParams);
    }

    public Object visitExprPeek(ExprPeek exp)
    {
        Expression expr = (Expression)exp.getExpr().accept(this);
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprPeek(exp.getContext(), expr);
    }

    public Object visitExprPop(ExprPop exp) { return exp; }
    
    public Object visitExprTernary(ExprTernary exp)
    {
        Expression a = (Expression)exp.getA().accept(this);
        Expression b = (Expression)exp.getB().accept(this);
        Expression c = (Expression)exp.getC().accept(this);
        if (a == exp.getA() && b == exp.getB() && c == exp.getC())
            return exp;
        else
            return new ExprTernary(exp.getContext(), exp.getOp(), a, b, c);
    }
    
    public Object visitExprUnary(ExprUnary exp)
    {
        Expression expr = (Expression)exp.getExpr().accept(this);
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprUnary(exp.getContext(), exp.getOp(), expr);
    }
    
    public Object visitExprVar(ExprVar exp) { return exp; }

    public Object visitFunction(Function func)
    {
        Statement newBody = (Statement)func.getBody().accept(this);
        if (newBody == func.getBody()) return func;
        return new Function(func.getContext(), func.getCls(),
                            func.getName(), func.getReturnType(),
                            func.getParams(), newBody);
    }
    
    public Object visitFuncWork(FuncWork func)
    {
        Statement newBody = (Statement)func.getBody().accept(this);
        Expression newPeek = (Expression)func.getPeekRate().accept(this);
        Expression newPop = (Expression)func.getPopRate().accept(this);
        Expression newPush = (Expression)func.getPushRate().accept(this);
        if (newBody == func.getBody() && newPeek == func.getPeekRate() &&
            newPop == func.getPopRate() && newPush == func.getPushRate())
            return func;
        return new FuncWork(func.getContext(), func.getName(),
                            newBody, newPeek, newPop, newPush);
    }
    
    public Object visitProgram(Program prog)
    {
        // Don't need to visit types, only streams.  Assume *something*
        // will change.
        List newStreams = new ArrayList();
        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); )
            newStreams.add(((FENode)(iter.next())).accept(this));
        return new Program(prog.getContext(), newStreams, prog.getStructs());
    }
    
    public Object visitSCAnon(SCAnon creator)
    {
        StreamSpec newSpec = (StreamSpec)creator.getSpec().accept(this);
        if (newSpec == creator.getSpec()) return creator;
        return new SCAnon(creator.getContext(), newSpec);
    }
    
    public Object visitSCSimple(SCSimple creator) { return creator; }
    public Object visitSJDuplicate(SJDuplicate sj) { return sj; }

    public Object visitSJRoundRobin(SJRoundRobin sj)
    {
        Expression newWeight = (Expression)sj.getWeight().accept(this);
        if (newWeight == sj.getWeight()) return sj;
        return new SJRoundRobin(sj.getContext(), newWeight);
    }
    
    public Object visitSJWeightedRR(SJWeightedRR sj)
    {
        boolean changed = false;
        List newWeights = new ArrayList();
        for (Iterator iter = sj.getWeights().iterator(); iter.hasNext(); )
        {
            Expression oldWeight = (Expression)iter.next();
            Expression newWeight = (Expression)oldWeight.accept(this);
            if (newWeight != oldWeight) changed = true;
            newWeights.add(newWeight);
        }
        if (!changed) return sj;
        return new SJWeightedRR(sj.getContext(), newWeights);
    }
        
    public Object visitStmtAdd(StmtAdd stmt)
    {
        StreamCreator newCreator =
            (StreamCreator)stmt.getCreator().accept(this);
        if (newCreator == stmt.getCreator()) return stmt;
        return new StmtAdd(stmt.getContext(), newCreator);
    }
    
    public Object visitStmtAssign(StmtAssign stmt)
    {
        Expression newLHS = doExpression(stmt.getLHS());
        Expression newRHS = doExpression(stmt.getRHS());
        if (newLHS == stmt.getLHS() && newRHS == stmt.getRHS())
            return stmt;
        return new StmtAssign(stmt.getContext(), newLHS, newRHS,
                              stmt.getOp());
    }
    
    public Object visitStmtBlock(StmtBlock stmt)
    {
        boolean changed = false;
        List oldStatements = newStatements;
        newStatements = new ArrayList();
        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
        {
            Statement oldStmt = (Statement)iter.next();
            Statement newStmt = (Statement)oldStmt.accept(this);
            if (newStmt != oldStmt) changed = true;
            if (newStmt != null) addStatement(newStmt);
        }
        Statement result;
        if (!changed)
            result = stmt;
        else
            result = new StmtBlock(stmt.getContext(), newStatements);
        newStatements = oldStatements;
        return result;
    }
    
    public Object visitStmtBody(StmtBody stmt)
    {
        StreamCreator newCreator =
            (StreamCreator)stmt.getCreator().accept(this);
        if (newCreator == stmt.getCreator()) return stmt;
        return new StmtBody(stmt.getContext(), newCreator);
    }

    public Object visitStmtBreak(StmtBreak stmt) { return stmt; }
    public Object visitStmtContinue(StmtContinue stmt) { return stmt; }

    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
        Statement newBody = (Statement)stmt.getBody().accept(this);
        Expression newCond = doExpression(stmt.getCond());
        if (newBody == stmt.getBody() && newCond == stmt.getCond())
            return stmt;
        return new StmtDoWhile(stmt.getContext(), newBody, newCond);
    }
    
    public Object visitStmtEnqueue(StmtEnqueue stmt)
    {
        Expression newValue = doExpression(stmt.getValue());
        if (newValue == stmt.getValue()) return stmt;
        return new StmtEnqueue(stmt.getContext(), newValue);
    }
    
    public Object visitStmtExpr(StmtExpr stmt)
    {
        Expression newExpr = doExpression(stmt.getExpression());
        if (newExpr == stmt.getExpression()) return stmt;
        return new StmtEnqueue(stmt.getContext(), newExpr);
    }

    public Object visitStmtFor(StmtFor stmt)
    {
        Statement newInit = (Statement)stmt.getInit().accept(this);
        Expression newCond = doExpression(stmt.getCond());
        Statement newIncr = (Statement)stmt.getIncr().accept(this);
        Statement newBody = (Statement)stmt.getBody().accept(this);
        if (newInit == stmt.getInit() && newCond == stmt.getCond() &&
            newIncr == stmt.getIncr() && newBody == stmt.getBody())
            return stmt;
        return new StmtFor(stmt.getContext(), newInit, newCond, newIncr,
                           newBody);
    }
    
    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        Expression newCond = doExpression(stmt.getCond());
        Statement newCons = stmt.getCons() == null ? null :
            (Statement)stmt.getCons().accept(this);
        Statement newAlt = stmt.getAlt() == null ? null :
            (Statement)stmt.getAlt().accept(this);
        if (newCond == stmt.getCond() && newCons == stmt.getCons() &&
            newAlt == stmt.getAlt())
            return stmt;
        return new StmtIfThen(stmt.getContext(), newCond, newCons, newAlt);
    }
    
    public Object visitStmtJoin(StmtJoin stmt)
    {
        SplitterJoiner newJoiner =
            (SplitterJoiner)stmt.getJoiner().accept(this);
        if (newJoiner == stmt.getJoiner()) return stmt;
        return new StmtJoin(stmt.getContext(), newJoiner);
    }
        
    public Object visitStmtLoop(StmtLoop stmt)
    {
        StreamCreator newCreator =
            (StreamCreator)stmt.getCreator().accept(this);
        if (newCreator == stmt.getCreator()) return stmt;
        return new StmtLoop(stmt.getContext(), newCreator);
    }

    public Object visitStmtPush(StmtPush stmt)
    {
        Expression newValue = doExpression(stmt.getValue());
        if (newValue == stmt.getValue()) return stmt;
        return new StmtPush(stmt.getContext(), newValue);
    }
    
    public Object visitStmtReturn(StmtReturn stmt)
    {
        Expression newValue = stmt.getValue() == null ? null :
            doExpression(stmt.getValue());
        if (newValue == stmt.getValue()) return stmt;
        return new StmtReturn(stmt.getContext(), newValue);
    }
    
    public Object visitStmtSplit(StmtSplit stmt)
    {
        SplitterJoiner newSplitter =
            (SplitterJoiner)stmt.getSplitter().accept(this);
        if (newSplitter == stmt.getSplitter()) return stmt;
        return new StmtSplit(stmt.getContext(), newSplitter);
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        Expression newInit = stmt.getInit() == null ? null :
            doExpression(stmt.getInit());
        if (newInit == stmt.getInit()) return stmt;
        return new StmtVarDecl(stmt.getContext(), stmt.getType(),
                               stmt.getName(), newInit);
    }
    
    public Object visitStmtWhile(StmtWhile stmt)
    {
        Expression newCond = doExpression(stmt.getCond());
        Statement newBody = (Statement)stmt.getBody().accept(this);
        if (newCond == stmt.getCond() && newBody == stmt.getBody())
            return stmt;
        return new StmtWhile(stmt.getContext(), newCond, newBody);
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        // Oof, there's a lot here.  At least half of it doesn't get
        // visited...
        StreamType newST = null;
        if (spec.getStreamType() != null)
            newST = (StreamType)spec.getStreamType().accept(this);
        List newVars = new ArrayList();
        List newFuncs = new ArrayList();
        boolean changed = false;
        
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            Statement oldVar = (Statement)iter.next();
            Statement newVar = (Statement)oldVar.accept(this);
            if (oldVar != newVar) changed = true;
            newVars.add(newVar);
        }
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
            Function oldFunc = (Function)iter.next();
            Function newFunc = (Function)oldFunc.accept(this);
            if (oldFunc != newFunc) changed = true;
            newFuncs.add(newFunc);
        }

        if (!changed && newST == spec.getStreamType()) return spec;
        return new StreamSpec(spec.getContext(), spec.getType(),
                              newST, spec.getName(), spec.getParams(),
                              newVars, newFuncs);
        
    }
    
    public Object visitStreamType(StreamType type) { return type; }
    public Object visitOther(FENode node) { return node; }
}
