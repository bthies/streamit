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

package streamit.frontend.passes;

import streamit.frontend.nodes.*;
import java.util.*;

/**
 * Perform checks on the semantic correctness of a StreamIt program.
 * The main entry point to this class is the static
 * <code>streamit.frontend.passes.SemanticChecker.check</code> method,
 * which returns <code>true</code> if a program has no detected
 * semantic errors.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: SemanticChecker.java,v 1.11 2003-10-09 19:51:01 dmaze Exp $
 */
public class SemanticChecker
{
    /**
     * Check a StreamIt program for semantic correctness.  This
     * returns <code>false</code> and prints diagnostics to standard
     * error if errors are detected.
     *
     * @param prog  parsed program object to check
     * @returns     <code>true</code> if no errors are detected
     */
    public static boolean check(Program prog)
    {
        SemanticChecker checker = new SemanticChecker();
        Map streamNames = checker.checkStreamNames(prog);
        checker.checkDupFieldNames(prog, streamNames);
        checker.checkStreamCreators(prog, streamNames);
        checker.checkFunctionValidity(prog);
        checker.checkStatementPlacement(prog);
        checker.checkIORates(prog);
        checker.checkVariableUsage(prog);
        return checker.good;
    }
    
    // true if we haven't found any errors
    private boolean good;

    private void report(FENode node, String message)
    {
        report(node.getContext(), message);
    }
    
    private void report(FEContext ctx, String message)
    {
        good = false;
        System.err.println(ctx + ": " + message);
    }
    
    public SemanticChecker()
    {
        good = true;
    }

    /**
     * Checks that the provided program does not have duplicate names
     * of structures or streams.
     *
     * @param prog  parsed program object to check
     * @returns a map from structure names to <code>FEContext</code>
     *          objects showing where they are declared
     */
    public Map checkStreamNames(Program prog)
    {
        Map names = new HashMap(); // maps names to FEContexts

        // Add built-in streams:
        FEContext ctx = new FEContext("<built-in>");
        names.put("Identity", ctx);
        names.put("FileReader", ctx);
        names.put("FileWriter", ctx);

        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); )
        {
            StreamSpec spec = (StreamSpec)iter.next();
            checkAStreamName(names, spec.getName(), spec.getContext());
        }

        for (Iterator iter = prog.getStructs().iterator(); iter.hasNext(); )
        {
            TypeStruct ts = (TypeStruct)iter.next();
            checkAStreamName(names, ts.getName(), ts.getContext());
        }
        return names;
    }
    
    private void checkAStreamName(Map map, String name, FEContext ctx)
    {
        if (map.containsKey(name))
        {
            FEContext octx = (FEContext)map.get(name);
            report(ctx, "Multiple declarations of '" + name + "'");
            report(octx, "as a stream or structure");
        }
        else
        {
            map.put(name, ctx);
        }
    }

    /**
     * Checks that no structures have duplicated field names.  In
     * particular, a field in a structure or filter can't have the
     * same name as another field in the same structure or filter, and
     * can't have the same name as a stream or structure.
     *
     * @param prog  parsed program object to check
     * @param streamNames  map from top-level stream and structure
     *              names to FEContexts in which they are defined
     */
    public void checkDupFieldNames(Program prog, Map streamNames)
    {
        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); )
        {
            StreamSpec spec = (StreamSpec)iter.next();
            Map localNames = new HashMap();
            Iterator i2;
            for (i2 = spec.getParams().iterator(); i2.hasNext(); )
            {
                Parameter param = (Parameter)i2.next();
                checkADupFieldName(localNames, streamNames,
                                   param.getName(), spec.getContext());
            }
            for (i2 = spec.getVars().iterator(); i2.hasNext(); )
            {
                FieldDecl field = (FieldDecl)i2.next();
                for (int i = 0; i < field.getNumFields(); i++)
                    checkADupFieldName(localNames, streamNames,
                                       field.getName(i), field.getContext());
            }
            for (i2 = spec.getFuncs().iterator(); i2.hasNext(); )
            {
                Function func = (Function)i2.next();
                // Some functions get alternate names if their real
                // name is null:
                String name = func.getName();
                if (name == null)
                {
                    switch(func.getCls())
                    {
                    case Function.FUNC_INIT: name = "init"; break;
                    case Function.FUNC_WORK: name = "work"; break;
                    case Function.FUNC_PREWORK: name = "prework"; break;
                    case Function.FUNC_HANDLER:
                        report(func, "message handlers must have names");
                        break;
                    case Function.FUNC_HELPER:
                        report(func, "helper functions must have names");
                        break;
                    case Function.FUNC_PHASE:
                        report(func, "phase functions must have names");
                        break;
                    default:
                        // is BUILTIN_HELPER and CONST_HELPER.  Ignore
                    }
                }
                if (name != null)
                    checkADupFieldName(localNames, streamNames,
                                       name, func.getContext());
            }
        }
        for (Iterator iter = prog.getStructs().iterator(); iter.hasNext(); )
        {
            TypeStruct ts = (TypeStruct)iter.next();
            Map localNames = new HashMap();
            for (int i = 0; i < ts.getNumFields(); i++)
                checkADupFieldName(localNames, streamNames,
                                   ts.getField(i), ts.getContext());
        }
    }

    private void checkADupFieldName(Map localNames, Map streamNames,
                                    String name, FEContext ctx)
    {
        if (localNames.containsKey(name))
        {
            FEContext octx = (FEContext)localNames.get(name);
            report(ctx, "Duplicate declaration of '" + name + "'");
            report(octx, "(also declared here)");
        }
        else
        {
            localNames.put(name, ctx);
            if (streamNames.containsKey(name))
            {
                FEContext octx = (FEContext)streamNames.get(name);
                report(ctx, "'" + name + "' has the same name as");
                report(octx, "a stream or structure");
            }
        }
    }

    /**
     * Check that, everywhere a named stream is created, the name
     * corresponds to an actual stream (or a reserved name).
     *
     * @param prog  parsed program object to check
     * @param streamNames  map from top-level stream and structure
     *              names to FEContexts in which they are defined
     */
    public void checkStreamCreators(Program prog, final Map streamNames)
    {
        prog.accept(new FEReplacer() {
                public Object visitSCSimple(SCSimple creator)
                {
                    String name = creator.getName();
                    if (!streamNames.containsKey(creator.getName()))
                        report(creator, "no such stream '" +
                               creator.getName() + "'");
                    return super.visitSCSimple(creator);
                }
            });
    }
    
    /**
     * Check that functions do not exist in context they are required
     * to, and that all required functions exist.  In particular,
     * work functions are required in filters and init functions
     * are required in not-filters; work, prework, and phase functions
     * are not allowed in not-filters.
     *
     * @param prog  parsed program object to check
     */
    public void checkFunctionValidity(Program prog)
    {
        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); )
        {
            StreamSpec spec = (StreamSpec)iter.next();
            boolean hasInit = false;
            boolean hasWork = false;
            for (Iterator ifunc = spec.getFuncs().iterator();
                 ifunc.hasNext(); )
            {
                Function func = (Function)ifunc.next();
                if (func.getCls() == Function.FUNC_INIT)
                    hasInit = true;
                if (func.getCls() == Function.FUNC_WORK)
                {
                    hasWork = true;
                    if (spec.getType() != StreamSpec.STREAM_FILTER)
                        report(func, "work functions only allowed in filters");
                }
                if (func.getCls() == Function.FUNC_PREWORK)
                    if (spec.getType() != StreamSpec.STREAM_FILTER)
                        report(func, "prework functions only allowed " +
                               "in filters");
                if (func.getCls() == Function.FUNC_PHASE)
                    if (spec.getType() != StreamSpec.STREAM_FILTER)
                        report(func, "phase functions only allowed " +
                               "in filters");
            }
            if (spec.getType() == StreamSpec.STREAM_FILTER && !hasWork)
                report(spec, "missing work function");
            if (spec.getType() != StreamSpec.STREAM_FILTER && !hasInit)
                report(spec, "missing init function");
        }
    }

    /**
     * Checks that statements exist in valid contexts for the type of
     * statement.  This checks that add, split, join, loop, and body
     * statements are only in appropriate initialization code, and
     * that push, pop, peek, and keep statements are only in
     * appropriate work function code.
     *
     * @param prog  parsed program object to check
     */
    public void checkStatementPlacement(Program prog)
    {
        // This is easiest to implement as a visitor, since otherwise
        // we'll end up trying to recursively call ourselves if there's
        // an anonymous stream declaration.  Ick.
        prog.accept(new FEReplacer() {
                // Track the current streamspec and function:
                private StreamSpec spec = null;
                private Function func = null;
                
                public Object visitStreamSpec(StreamSpec ss)
                {
                    StreamSpec oldSpec = spec;
                    spec = ss;
                    Object result = super.visitStreamSpec(ss);
                    spec = oldSpec;
                    return result;
                }

                public Object visitFunction(Function func2)
                {
                    Function oldFunc = func;
                    func = func2;
                    Object result = super.visitFunction(func2);
                    func = oldFunc;
                    return result;
                }

                public Object visitFuncWork(FuncWork func2)
                {
                    Function oldFunc = func;
                    func = func2;
                    Object result = super.visitFuncWork(func2);
                    func = oldFunc;
                    return result;
                }

                // So the remainder of this just needs to check
                // spec.getType() and func.getCls() and that they're
                // correct vs. the type of statement.
                public Object visitStmtAdd(StmtAdd stmt)
                {
                    if ((func.getCls() != Function.FUNC_INIT) ||
                        (spec.getType() != StreamSpec.STREAM_PIPELINE &&
                         spec.getType() != StreamSpec.STREAM_SPLITJOIN))
                        report(stmt,
                               "add statement only allowed " +
                               "in pipeline/splitjoin");
                    return super.visitStmtAdd(stmt);
                }

                public Object visitStmtBody(StmtBody stmt)
                {
                    if (func.getCls() != Function.FUNC_INIT ||
                        spec.getType() != StreamSpec.STREAM_FEEDBACKLOOP)
                        report(stmt,
                               "body statement only allowed " +
                               "in feedbackloop");
                    return super.visitStmtBody(stmt);
                }

                public Object visitStmtEnqueue(StmtEnqueue stmt)
                {
                    if (func.getCls() != Function.FUNC_INIT ||
                        spec.getType() != StreamSpec.STREAM_FEEDBACKLOOP)
                        report(stmt,
                               "enqueue statement only allowed " +
                               "in feedbackloop");
                    return super.visitStmtEnqueue(stmt);
                }

                public Object visitStmtJoin(StmtJoin stmt)
                {
                    if ((func.getCls() != Function.FUNC_INIT) ||
                        (spec.getType() != StreamSpec.STREAM_FEEDBACKLOOP &&
                         spec.getType() != StreamSpec.STREAM_SPLITJOIN))
                        report(stmt,
                               "join statement only allowed " +
                               "in splitjoin/feedbackloop");
                    return super.visitStmtJoin(stmt);
                }

                public Object visitStmtLoop(StmtLoop stmt)
                {
                    if (func.getCls() != Function.FUNC_INIT ||
                        spec.getType() != StreamSpec.STREAM_FEEDBACKLOOP)
                        report(stmt,
                               "loop statement only allowed " +
                               "in feedbackloop");
                    return super.visitStmtLoop(stmt);
                }

                public Object visitExprPeek(ExprPeek expr)
                {
                    if ((func.getCls() != Function.FUNC_PHASE &&
                         func.getCls() != Function.FUNC_PREWORK &&
                         func.getCls() != Function.FUNC_WORK) ||
                        (spec.getType() != StreamSpec.STREAM_FILTER))
                        report(expr,
                               "peek expression only allowed " +
                               "in filter work functions");
                    return super.visitExprPeek(expr);
                }

                public Object visitExprPop(ExprPop expr)
                {
                    if ((func.getCls() != Function.FUNC_PHASE &&
                         func.getCls() != Function.FUNC_PREWORK &&
                         func.getCls() != Function.FUNC_WORK) ||
                        (spec.getType() != StreamSpec.STREAM_FILTER))
                        report(expr,
                               "pop expression only allowed " +
                               "in filter work functions");
                    return super.visitExprPop(expr);
                }

                public Object visitStmtPush(StmtPush stmt)
                {
                    if ((func.getCls() != Function.FUNC_PHASE &&
                         func.getCls() != Function.FUNC_PREWORK &&
                         func.getCls() != Function.FUNC_WORK) ||
                        (spec.getType() != StreamSpec.STREAM_FILTER))
                        report(stmt,
                               "push statement only allowed " +
                               "in filter work functions");
                    return super.visitStmtPush(stmt);
                }

                public Object visitStmtSplit(StmtSplit stmt)
                {
                    if ((func.getCls() != Function.FUNC_INIT) ||
                        (spec.getType() != StreamSpec.STREAM_FEEDBACKLOOP &&
                         spec.getType() != StreamSpec.STREAM_SPLITJOIN))
                        report(stmt,
                               "split statement only allowed " +
                               "in splitjoin/feedbackloop");
                    return super.visitStmtSplit(stmt);
                }
            });
    }

    /**
     * Checks that work and phase function I/O rates are valid for
     * their stream types, and that push, pop, and peek statements are
     * used correctly.  A statement can has a pop or peek rate of 0
     * (or null) iff its input type is void, and a push rate of 0 (or
     * null) iff its output type is void; in these cases, the
     * corresponding statement or expression may not appear in the
     * work function code.
     *
     * @param prog  parsed program object to check
     */
    public void checkIORates(Program prog)
    {
        // Similarly, implement as a visitor; there's still the
        // recursion issue.
        prog.accept(new FEReplacer() {
                private boolean canPop, canPush;
                private boolean hasPop, hasPush;
                private StreamSpec spec;
                
                public Object visitStreamSpec(StreamSpec ss)
                {
                    // We want to save the spec so we can look at its
                    // stream type.  But we only care within the
                    // context of work functions in filters, which
                    // can't have recursive stream definitions.  So
                    // unless something really broken is going on,
                    // we'll never visit a stream spec, visit
                    // something else, and then come back to visit a
                    // work function in the original spec; thus,
                    // there's no particular reason to save the old
                    // spec.
                    spec = ss;
                    return super.visitStreamSpec(ss);
                }

                public Object visitFuncWork(FuncWork func)
                {
                    // These can't be nested, which simplifies life.
                    // In fact, there are really two things we care
                    // about: if popping/peeking is allowed, and if
                    // pushing is allowed.  Decide these based on
                    // the declared I/O rates.
                    canPop = canPushOrPop(func.getPopRate());
                    canPush = canPushOrPop(func.getPushRate());
                    boolean canPeek = canPushOrPop(func.getPeekRate());
                    // (But note that we can still peek up to the pop
                    // rate even if no peek rate is declared; thus,
                    // this is used only to determine if a function
                    // is peeking without popping.)

                    // If this is a work or prework function,
                    // rather than a phase function, then
                    // it's possible to have neither push nor
                    // pop rates even with non-void types.
                    boolean isInit = (func.getCls() == Function.FUNC_PREWORK);
                    boolean isPhase = (func.getCls() == Function.FUNC_PHASE);
                    boolean phased = (!canPop) && (!canPeek) && (!isPhase);
                    
                    // Check for consistency with the stream type.
                    StreamType st = spec.getStreamType();

                    if (typeIsVoid(st.getIn()) && canPop)
                        report(func,
                               "filter declared void input type, but " +
                               "non-zero pop rate");
                    if (!typeIsVoid(st.getIn()) && !canPop && !phased &&
                        !isPhase && !isInit)
                        report(func,
                               "filter declared non-void input type, but "+
                               "has zero pop rate");
                    if (typeIsVoid(st.getOut()) && canPush)
                        report(func,
                               "filter declared void output type, but " +
                               "non-zero push rate");
                    if (!typeIsVoid(st.getOut()) && !canPush && !phased &&
                        !isPhase && !isInit)
                        report(func,
                               "filter declared non-void output type, but " +
                               "has zero push rate");
                    // If this isn't a phase function, and it has a
                    // peek rate, then it must have a pop rate.
                    if (!isPhase && !isInit && !canPop && canPeek)
                        report(func,
                               "filter declared a peek rate but not a " +
                               "pop rate");
                    
                    return super.visitFuncWork(func);
                    // To consider: check that, if the function has
                    // a push rate, then at least one push happens.
                    // Don't need this for popping since we have
                    // the implicit pop rule.
                }

                private boolean canPushOrPop(Expression expr)
                {
                    if (expr == null) return false;
                    if (!(expr instanceof ExprConstInt)) return true;
                    ExprConstInt eci = (ExprConstInt)expr;
                    if (eci.getVal() == 0) return false;
                    return true;
                }

                private boolean typeIsVoid(Type type)
                {
                    if (!(type instanceof TypePrimitive)) return false;
                    TypePrimitive tp = (TypePrimitive)type;
                    if (tp.getType() == tp.TYPE_VOID) return true;
                    return false;
                }

                public Object visitExprPeek(ExprPeek expr)
                {
                    if (!canPop)
                        report(expr,
                               "peeking not allowed in functions with " +
                               "zero pop rate");
                    return super.visitExprPeek(expr);
                }

                public Object visitExprPop(ExprPop expr)
                {
                    if (!canPop)
                        report(expr,
                               "popping not allowed in functions with " +
                               "zero pop rate");
                    return super.visitExprPop(expr);
                }

                public Object visitStmtPush(StmtPush stmt)
                {
                    if (!canPush)
                        report(stmt,
                               "pushing not allowed in functions with " +
                               "zero push rate");
                    return super.visitStmtPush(stmt);
                }
            });
    }

    /**
     * Check that variables are declared and used correctly.  In
     * particular, check that variables are declared before their
     * first use, that local variables and fields don't shadow stream
     * parameters, and that stream parameters don't appear on the
     * left-hand side of assignment statements or inside mutating
     * unary operations.
     *
     * @param prog  parsed program object to check
     */
    public void checkVariableUsage(Program prog)
    {
        prog.accept(new SymbolTableVisitor(null) {
                public Object visitExprVar(ExprVar var)
                {
                    // Check: the variable is declared somewhere.
                    try
                    {
                        symtab.lookupVar(var);
                    }
                    catch(UnrecognizedVariableException e)
                    {
                        report(var, "unrecognized variable");
                    }
                    return super.visitExprVar(var);
                }

                private boolean isStreamParam(String name)
                {
                    try
                    {
                        int kind = symtab.lookupKind(name);
                        if (kind == SymbolTable.KIND_STREAM_PARAM)
                            return true;
                    }
                    catch(UnrecognizedVariableException e)
                    {
                        // ignore; calling code should have recursive
                        // calls which will catch this
                    }
                    return false;
                }

                public Object visitStmtVarDecl(StmtVarDecl stmt)
                {
                    // Check: none of the locals shadow stream parameters.
                    for (int i = 0; i < stmt.getNumVars(); i++)
                    {
                        String name = stmt.getName(i);
                        if (isStreamParam(name))
                            report(stmt,
                                   "local variable shadows stream parameter");
                    }
                    return super.visitStmtVarDecl(stmt);
                }

                public Object visitStmtAssign(StmtAssign stmt)
                {
                    // Check: LHS isn't a stream parameter.
                    Expression lhs = stmt.getLHS();
                    if (lhs instanceof ExprVar)
                    {
                        ExprVar lhsv = (ExprVar)lhs;
                        String name = lhsv.getName();
                        if (isStreamParam(name))
                            report(stmt, "assignment to stream parameter");
                    }
                    return super.visitStmtAssign(stmt);
                }

                public Object visitExprUnary(ExprUnary expr)
                {
                    int op = expr.getOp();
                    Expression child = expr.getExpr();
                    if ((child instanceof ExprVar) &&
                        (op == ExprUnary.UNOP_PREINC ||
                         op == ExprUnary.UNOP_POSTINC ||
                         op == ExprUnary.UNOP_PREDEC ||
                         op == ExprUnary.UNOP_POSTDEC))
                    {
                        ExprVar var = (ExprVar)child;
                        String name = var.getName();
                        if (isStreamParam(name))
                            report(expr, "modification of stream parameter");
                    }
                    return super.visitExprUnary(expr);
                }
            });
    }
}
