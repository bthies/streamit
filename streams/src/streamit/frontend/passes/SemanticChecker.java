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

import streamit.frontend.controlflow.*;
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
 * @version $Id: SemanticChecker.java,v 1.21 2004-01-27 23:20:59 dmaze Exp $
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
        checker.checkStreamTypes(prog);
        checker.checkFunctionValidity(prog);
        checker.checkStatementPlacement(prog);
        checker.checkVariableUsage(prog);
        checker.checkBasicTyping(prog);
        checker.checkStreamConnectionTyping(prog);
        checker.checkStatementCounts(prog);
        checker.checkIORates(prog);
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
     * Check that stream type declarations are valid.  In particular,
     * check that there is exactly one void->void declaration
     * and that it corresponds to a named stream.
     *
     * @param prog  parsed program object to check
     */
    public void checkStreamTypes(Program prog)
    {
        theTopLevel = null;

        // Check for duplicate top-level streams.
        prog.accept(new FEReplacer() {
                public Object visitStreamSpec(StreamSpec ss)
                {
                    StreamType st = ss.getStreamType();
                    if (st != null &&
                        st.getIn() instanceof TypePrimitive &&
                        st.getOut() instanceof TypePrimitive &&
                        ((TypePrimitive)st.getIn()).getType() ==
                        TypePrimitive.TYPE_VOID &&
                        ((TypePrimitive)st.getOut()).getType() ==
                        TypePrimitive.TYPE_VOID)
                    {
                        // Okay, we have a void->void object.
                        if (ss.getName() == null)
                            report(ss, "anonymous stream cannot be top-level");
                        else if (theTopLevel == null)
                            theTopLevel = ss;
                        else
                        {
                            report(theTopLevel,
                                   "first declared top-level stream" +
                                   theTopLevel.getName());
                            report(ss, "duplicate top-level stream " +
                                   ss.getName());
                        }
                    }
                    return super.visitStreamSpec(ss);
                }
            });
        if (theTopLevel == null)
            report((FEContext)null, "no top-level stream declared");
    }

    private StreamSpec theTopLevel;

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
     * Checks that basic operations are performed on appropriate types.
     * For example, the type of the unary ! operator can't be float
     * or complex; there needs to be a common type for equality
     * checking, but an arithmetic type for arithmetic operations.
     * This also tests that the right-hand side of an assignment is
     * assignable to the left-hand side.
     *<p>
     * (Does this test that peek, pop, push, and enqueue are used
     * properly?  Initial plans were to have this as a separate
     * function, but it does fit nicely here.)
     *
     * @param prog  parsed program object to check
     */
    public void checkBasicTyping(Program prog)
    {
        /* We mostly just need to walk through and check things.
         * enqueue statements can be hard, though: if there's a
         * feedback loop with void type, we need to find the
         * loopback type, which is the output type of the loop
         * stream object.  If it's kosher to have enqueue before
         * loop, we need an extra pass over statements to find
         * the loop type.  The AssignLoopTypes pass could be
         * helpful here, but we want to give an error message if
         * things fail. */
        prog.accept(new SymbolTableVisitor(null) {
                // Need to visit everything.  Handily, STV does things
                // for us like save streamType when we run across
                // streamspecs; the only potentially hard thing is
                // the loop type of feedback loops.
                //
                // Otherwise, assume that the GetExprType pass can
                // find the types of things.  This should report
                // an error exactly when GET returns null,
                // so we can ignore nulls (assume that they type
                // check).
                public Object visitExprUnary(ExprUnary expr)
                {
                    Type ot = getType(expr.getExpr());
                    if (ot != null)
                    {
                        Type inttype =
                            new TypePrimitive(TypePrimitive.TYPE_INT);
                        Type bittype =
                            new TypePrimitive(TypePrimitive.TYPE_BIT);
                    
                        switch(expr.getOp())
                        {
                        case ExprUnary.UNOP_NEG:
                            if (!inttype.promotesTo(ot))
                                report(expr, "cannot negate " + ot);
                            break;
                            
                        case ExprUnary.UNOP_NOT:
                            if (!ot.promotesTo(inttype))
                                report(expr, "cannot take boolean not of " +
                                       ot);
                            break;
                            
                        case ExprUnary.UNOP_PREDEC:
                        case ExprUnary.UNOP_PREINC:
                        case ExprUnary.UNOP_POSTDEC:
                        case ExprUnary.UNOP_POSTINC:
                            if (!inttype.promotesTo(ot))
                                report(expr, "cannot perform ++/-- on " + ot);
                            break;
                        }
                    }
                    
                    return super.visitExprUnary(expr);
                }

                public Object visitExprBinary(ExprBinary expr)
                {
                    Type lt = getType(expr.getLeft());
                    Type rt = getType(expr.getRight());

                    if (lt != null && rt != null)
                    {                        
                        Type ct = lt.leastCommonPromotion(rt);
                        Type inttype =
                            new TypePrimitive(TypePrimitive.TYPE_INT);
                        Type bittype =
                            new TypePrimitive(TypePrimitive.TYPE_BIT);
                        Type cplxtype =
                            new TypePrimitive(TypePrimitive.TYPE_COMPLEX);
                        Type floattype =
                            new TypePrimitive(TypePrimitive.TYPE_FLOAT);
                        if (ct == null)
                        {
                            report (expr,
                                    "incompatible types in binary expression");
                            return expr;
                        }
                        // Check whether ct is an appropriate type.
                        switch (expr.getOp())
                        {
                        // Arithmetic operations:
                        case ExprBinary.BINOP_ADD:
                        case ExprBinary.BINOP_DIV:
                        case ExprBinary.BINOP_MUL:
                        case ExprBinary.BINOP_SUB:
                            if (!ct.promotesTo(cplxtype))
                                report(expr,
                                       "cannot perform arithmetic on " + ct);
                            break;

                        // Bitwise and integer operations:
                        case ExprBinary.BINOP_BAND:
                        case ExprBinary.BINOP_BOR:
                        case ExprBinary.BINOP_BXOR:
                            if (!ct.promotesTo(inttype))
                                report(expr,
                                       "cannot perform bitwise operations on "
                                       + ct);
                            break;

                        case ExprBinary.BINOP_MOD:
                            if (!ct.promotesTo(inttype))
                                report(expr, "cannot perform % on " + ct);
                            break;
                            
                        // Boolean operations:
                        case ExprBinary.BINOP_AND:
                        case ExprBinary.BINOP_OR:
                            if (!ct.promotesTo(bittype))
                                report(expr,
                                       "cannot perform boolean operations on "
                                       + ct);
                            break;

                        // Comparison operations:
                        case ExprBinary.BINOP_GE:
                        case ExprBinary.BINOP_GT:
                        case ExprBinary.BINOP_LE:
                        case ExprBinary.BINOP_LT:
                            if (!ct.promotesTo(floattype))
                                report(expr,
                                       "cannot compare non-real type " + ct);
                            break;
                        
                        // Equality, can compare anything:
                        case ExprBinary.BINOP_EQ:
                        case ExprBinary.BINOP_NEQ:
                            break;
                        
                        // And now we should have covered everything.
                        default:
                            report(expr,
                                   "semantic checker missed a binop type");
                            break;
                        }
                        return expr;
                    }

                    return super.visitExprBinary(expr);
                }

                public Object visitExprTernary(ExprTernary expr)
                {
                    Type at = getType(expr.getA());
                    Type bt = getType(expr.getB());
                    Type ct = getType(expr.getC());
                    
                    if (at != null)
                    {
                        if (!at.promotesTo
                            (new TypePrimitive(TypePrimitive.TYPE_INT)))
                            report(expr,
                                   "first part of ternary expression "+
                                   "must be int");
                    }

                    if (bt != null && ct != null)
                    {                        
                        Type xt = bt.leastCommonPromotion(ct);
                        if (xt == null)
                            report(expr,
                                   "incompatible second and third types "+
                                   "in ternary expression");
                    }
                    
                    return super.visitExprTernary(expr);
                }

                public Object visitExprField(ExprField expr)
                {
                    Type lt = getType(expr.getLeft());

                    // Either lt is complex, or it's a structure
                    // type, or it's null, or it's an error.
                    if (lt == null)
                    {
                        // pass
                    }
                    else if (lt.isComplex())
                    {
                        String rn = expr.getName();
                        if (!rn.equals("real") &&
                            !rn.equals("imag"))
                            report(expr,
                                   "complex variables have only "+
                                   "'real' and 'imag' fields");
                    }
                    else if (lt instanceof TypeStruct)
                    {
                        TypeStruct ts = (TypeStruct)lt;
                        String rn = expr.getName();
                        boolean found = false;
                        for (int i = 0; i < ts.getNumFields(); i++)
                            if (ts.getField(i).equals(rn))
                            {
                                found = true;
                                break;
                            }
                        
                        if (!found)
                            report(expr,
                                   "structure does not have a field named "+
                                   "'" + rn + "'");
                    }
                    else
                    {
                        report(expr,
                               "field reference of a non-structure type");
                    }

                    return super.visitExprField(expr);
                }

                public Object visitExprArray(ExprArray expr)
                {
                    Type bt = getType(expr.getBase());
                    Type ot = getType(expr.getOffset());
                    
                    if (bt != null)
                    {
                        if (!(bt instanceof TypeArray))
                            report(expr, "array access with a non-array base");
                    }

                    if (ot != null)
                    {
                        if (!ot.promotesTo
                            (new TypePrimitive(TypePrimitive.TYPE_INT)))
                            report(expr, "array index must be an int");
                    }
                    
                    return super.visitExprArray(expr);
                }

                public Object visitExprPeek(ExprPeek expr)
                {
                    Type it = getType(expr.getExpr());
                    
                    if (it != null)
                    {
                        if (!it.promotesTo
                            (new TypePrimitive(TypePrimitive.TYPE_INT)))
                            report(expr, "peek index must be an int");
                    }
                    
                    return super.visitExprPeek(expr);                    
                }

                public Object visitStmtPush(StmtPush stmt)
                {
                    Type xt = getType(stmt.getValue());
                    
                    if (xt != null && streamType != null &&
                        streamType.getOut() != null &&
                        !(xt.promotesTo(streamType.getOut())))
                        report(stmt, "push expression must be of "+
                               "output type of filter");
                    
                    return super.visitStmtPush(stmt);
                }

                public Object visitStmtEnqueue(StmtEnqueue stmt)
                {
                    Type xt = getType(stmt.getValue());
                    
                    // Punt if, in addition to the normal cases,
                    // the input type is void.  (We'd have to
                    // determine the loopback type now.)
                    if (xt != null && streamType != null)
                    {
                        Type in = streamType.getIn();
                        if (!((in instanceof TypePrimitive) &&
                              ((TypePrimitive)in).getType() ==
                              TypePrimitive.TYPE_VOID) &&
                            !(xt.promotesTo(in)))
                        report(stmt, "enqueue expression must be of "+
                               "input type of feedback loop");
                    }
                    
                    return super.visitStmtEnqueue(stmt);
                }

                public Object visitStmtAssign(StmtAssign stmt)
                {
                    Type lt = getType(stmt.getLHS());
                    Type rt = getType(stmt.getRHS());
                    
                    if (lt != null && rt != null &&
                        !(rt.promotesTo(lt)))
                        report(stmt,
                               "right-hand side of assignment must "+
                               "be promotable to left-hand side's type");
                    
                    return super.visitStmtAssign(stmt);
                }
            });
    }
    
    /**
     * Checks that streams are connected with consistent types.
     * In a split-join, all of the children need to have the same
     * type, which is the same type as the parent stream; in a pipeline,
     * the type of the output of the first child must be the same as
     * the input of the second, and so on; feedback loops must be
     * properly connected too.
     *
     * @param prog  parsed program object to check
     */
    public void checkStreamConnectionTyping(Program prog)
    {
        // Generic map of stream names:
        final Map streams = new HashMap();
        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext(); )
        {
            StreamSpec ss = (StreamSpec)iter.next();
            streams.put(ss.getName(), ss);
        }
        
        // Look for init functions in composite streams:
        prog.accept(new FEReplacer() {
                public Object visitStreamSpec(StreamSpec ss)
                {
                    if (ss.getType() == StreamSpec.STREAM_SPLITJOIN)
                        checkSplitJoinConnectionTyping(ss, streams);
                    else if (ss.getType() == StreamSpec.STREAM_PIPELINE)
                        checkPipelineConnectionTyping(ss, streams);
                    else if (ss.getType() == StreamSpec.STREAM_FEEDBACKLOOP)
                        checkFeedbackLoopConnectionTyping(ss, streams);
                    return super.visitStreamSpec(ss);
                }
            });
    }

    private void checkPipelineConnectionTyping(StreamSpec ss,
                                               final Map streams)
    {
        Function init = ss.getInitFunc();
        final CFG cfg = CFGBuilder.buildCFG(init);
        final StreamType st = ss.getStreamType();
        final Set reported = new HashSet();

        // Use data flow to check the stream types.
        Map inTypes = new DataFlow() {
                public Lattice getInit()
                {
                    if (st == null)
                        return new StrictTypeLattice(true); // top
                    else
                        return new StrictTypeLattice(st.getIn());
                }
                
                public Lattice flowFunction(CFGNode node, Lattice in)
                {
                    if (!node.isStmt())
                        return in;
                    Statement stmt = node.getStmt();
                    if (!(stmt instanceof StmtAdd))
                        return in;
                    StrictTypeLattice stl = (StrictTypeLattice)in;
                    StmtAdd add = (StmtAdd)stmt;
                    StreamCreator sc = ((StmtAdd)stmt).getCreator();
                    StreamType st2;
                    if (sc instanceof SCAnon)
                        st2 = ((SCAnon)sc).getSpec().getStreamType();
                    else
                    {
                        String name = ((SCSimple)sc).getName();
                        StreamSpec spec = (StreamSpec)streams.get(name);
                        if (spec == null)
                            // Technically an error; keep going.
                            return stl.getTop();
                        st2 = spec.getStreamType();
                    }
                    // Report on conflicts at this node, but only
                    // if we haven't yet.
                    if (!reported.contains(stmt))
                    {
                        // Is the input bottom?
                        if (stl.isBottom())
                        {
                            reported.add(stmt);
                            report(stmt, "ambiguous prevailing stream type");
                        }
                        // If we have a stream type, does it disagree?
                        else if (!stl.isTop() && st2 != null &&
                                 !(stl.getValue().equals(st2.getIn())))
                        {
                            reported.add(stmt);
                            report(stmt, "prevailing stream type " +
                                   stl.getValue() + " disagrees with child");
                        }
                    }
                    // In any case, the output type is top if our stream
                    // type is null, or a value.
                    if (st2 == null)
                        return new StrictTypeLattice(true);
                    else
                        return new StrictTypeLattice(st2.getOut());
                }
        }.run(cfg);

        // At this point we've reported if stream types don't match
        // within the stream.  Check the output:
        StrictTypeLattice stl = (StrictTypeLattice)inTypes.get(cfg.getExit());
        if (stl.isBottom())
            report(init, "ambiguous type at pipeline exit");
        else if (!stl.isTop() && st != null &&
                 !(st.getOut().equals(stl.getValue())))
            report(init, "type at pipeline exit " + stl.getValue() +
                   " disagrees with declared type");
    }
    
    private void checkSplitJoinConnectionTyping(StreamSpec ss, Map streams)
    {
        checkSJFLConnections(ss, streams, true, true);
        checkSJFLConnections(ss, streams, true, false);
    }

    private void checkFeedbackLoopConnectionTyping(StreamSpec ss, Map streams)
    {
        checkSJFLConnections(ss, streams, false, true);
        checkSJFLConnections(ss, streams, false, false);
    }

    // Only do this once, it's the same basic algorithm for split-joins
    // and feedback loops:
    private void checkSJFLConnections(final StreamSpec ss,
                                      final Map streams,
                                      final boolean forSJ,
                                      final boolean forInput)
    {
        // What we want to do is check that the input and/or output
        // types of a non-pipeline composite stream match up.  That is,
        // there should be a single type at the splitter or joiner.
        // Using the data-flow infrastructure for this seems gratuitous,
        // except that the StreamType type may be indeterminate
        // (could have a null StreamType, or for a feedback loop
        // could have a void input or output type).

        Function init = ss.getInitFunc();
        assert init != null;
        CFG cfg = CFGBuilder.buildCFG(init);
        Map typeMap = new DataFlow() {
                public Lattice getInit()
                {
                    StreamType st = ss.getStreamType();
                    if (st == null)
                        return new StrictTypeLattice(true); // top
                    Type theType = forInput ? st.getIn() : st.getOut();
                    // For feedback loops, void input/output is
                    // special, and equivalent to top here:
                    if (!forSJ &&
                        theType instanceof TypePrimitive &&
                        ((TypePrimitive)theType).getType() ==
                        TypePrimitive.TYPE_VOID)
                        return new StrictTypeLattice(true);
                    return new StrictTypeLattice(theType);
                }

                public Lattice flowFunction(CFGNode node, Lattice in)
                {
                    if (!node.isStmt())
                        return in;
                    
                    // This is generic code for either split-joins or
                    // feedback loops.  Don't worry about getting the
                    // wrong statement.  sc gets set to the child
                    // stream creator object, we'll resolve it
                    // momentarily.  takeInput gets set if we're
                    // interested in the input type of the stream,
                    // false if we want the output.
                    Statement stmt = node.getStmt();
                    StreamCreator sc;
                    boolean takeInput;
                    if (stmt instanceof StmtAdd)
                    {
                        sc = ((StmtAdd)stmt).getCreator();
                        takeInput = forInput;
                    }
                    else if (stmt instanceof StmtBody)
                    {
                        sc = ((StmtBody)stmt).getCreator();
                        // child input connected to loop input
                        takeInput = forInput;
                    }
                    else if (stmt instanceof StmtLoop)
                    {
                        sc = ((StmtLoop)stmt).getCreator();
                        // child input connected to loop output
                        takeInput = !forInput;
                    }
                    else
                        // uninteresting statement
                        return in;
                    
                    // Find the actual stream spec/type.
                    StreamSpec ss = null;
                    if (sc instanceof SCAnon)
                        ss = ((SCAnon)sc).getSpec();
                    else if (sc instanceof SCSimple)
                    {
                        String name = ((SCSimple)sc).getName();
                        ss = (StreamSpec)streams.get(name);
                    }

                    if (ss == null)
                        return in;
                    if (ss.getStreamType() == null)
                        return in;
                    Type theType;
                    if (takeInput)
                        theType = ss.getStreamType().getIn();
                    else
                        theType = ss.getStreamType().getOut();

                    // Split-joins get to randomly have children
                    // with void inputs/outputs.  In particular:
                    // float->float splitjoin Insert(float f) {
                    //   split roundrobin(1,0);
                    //   add Identity<float>();
                    //   add void->float filter { ... };
                    //   join roundrobin;
                    // }
                    if (forSJ &&
                        theType instanceof TypePrimitive &&
                        ((TypePrimitive)theType).getType() ==
                        TypePrimitive.TYPE_VOID)
                        return in;
                    
                    // What do we actually return?  Take the meet
                    // of the input and output values, so that when
                    // we see
                    //   add void->float filter { ... };
                    //   add void->int filter { ... };
                    // we notice something's wrong.
                    //
                    // In principle, we could report here, but we
                    // would get an extra error on linear code when
                    // reporting on an error at a possible join
                    // just before exit (make the above statements
                    // branches of an if/then).
                    Lattice newVal = new StrictTypeLattice(theType);
                    return in.meet(newVal);
                }
            }.run(cfg);

        // We again want something that can traverse a CFG and
        // give the first place where the lattice value is bottom.
        StrictTypeLattice exitVal =
            (StrictTypeLattice)typeMap.get(cfg.getExit());
        if (exitVal.isBottom())
        {
            if (forSJ && forInput)
                report(ss, "types at splitjoin entry disagree");
            else if (forSJ && !forInput)
                report(ss, "types at splitjoin exit disagree");
            else if (!forSJ && forInput)
                report(ss, "types at feedbackloop entry disagree");
            else if (!forSJ && !forInput)
                report(ss, "types at feedbackloop exit disagree");
        }
    }

    /**
     * Checks that statements that must be invoked some number
     * of times in fact are.  This includes checking that split-join
     * and feedback loop init functions have exactly one splitter
     * and exactly one joiner.
     *
     * @param prog  parsed program object to check
     */
    public void checkStatementCounts(Program prog)
    {
        // Look for init functions in split-joins and feedback loops:
        prog.accept(new FEReplacer() {
                public Object visitStreamSpec(StreamSpec ss)
                {
                    if (ss.getType() == StreamSpec.STREAM_SPLITJOIN ||
                        ss.getType() == StreamSpec.STREAM_FEEDBACKLOOP)
                    {
                        exactlyOneStatement
                            (ss, "split",
                             new StatementCounter() {
                                 public boolean
                                     statementQualifies(Statement stmt)
                                 { return stmt instanceof StmtSplit; }
                             });
                        exactlyOneStatement
                            (ss, "join",
                             new StatementCounter() {
                                 public boolean
                                     statementQualifies(Statement stmt)
                                 { return stmt instanceof StmtJoin; }
                             });
                    }

                    if (ss.getType() == StreamSpec.STREAM_FEEDBACKLOOP)
                    {
                        exactlyOneStatement
                            (ss, "body",
                             new StatementCounter() {
                                 public boolean
                                     statementQualifies(Statement stmt)
                                 { return stmt instanceof StmtBody; }
                             });
                        exactlyOneStatement
                            (ss, "loop",
                             new StatementCounter() {
                                 public boolean
                                     statementQualifies(Statement stmt)
                                 { return stmt instanceof StmtLoop; }
                             });
                    }
                    return super.visitStreamSpec(ss);
                }
            });
    }

    private void exactlyOneStatement(StreamSpec ss, String stype,
                                     StatementCounter sc)
    {
        Function init = ss.getInitFunc();
        assert init != null;
        CFG cfg = CFGBuilder.buildCFG(init);
        Map splitCounts = sc.run(cfg);
        // TODO: modularize this analysis; report the first place
        // where there's a second split/join, and/or the first place
        // where there's ambiguity (bottom).  This would be easier if
        // Java had lambdas.
        CountLattice exitVal = (CountLattice)splitCounts.get(cfg.getExit());
        if (exitVal.isTop())
            report(init, "weird failure: " + stype + " exit value is top");
        else if (exitVal.isBottom())
            report(init, "couldn't determine number of " + stype +
                   " statements");
        else if (exitVal.getValue() == 0)
            report(init, "no " + stype + " statements");
        else if (exitVal.getValue() > 1)
            report(init, "more than one " + stype + " statement");
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
