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
 * @version $Id: SemanticChecker.java,v 1.3 2003-07-09 14:28:53 dmaze Exp $
 */
public class SemanticChecker
{
    /**
     * Check a StreamIt program for semantic correctness.  This
     * returns <code>false</code> and prints diagnostics to
     * standard error if errors are detected.
     *
     * @param prog  parsed program object to check
     * @returns     <code>true</code> if no errors are detected
     */
    public static boolean check(Program prog)
    {
        SemanticChecker checker = new SemanticChecker();
        Map streamNames = checker.checkStreamNames(prog);
        checker.checkDupFieldNames(prog, streamNames);
        checker.checkStatementPlacement(prog);
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
     * Checks that the provided program does not have duplicate
     * names of structures or streams.
     *
     * @param prog  parsed program object to check
     * @returns a map from structure names to <code>FEContext</code>
     *          objects showing where they are declared
     */
    public Map checkStreamNames(Program prog)
    {
        Map names = new HashMap(); // maps names to FEContexts
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
            report(octx, "Multiple declarations of '" + name + "'");
            report(ctx, "as a stream or structure");
        }
        else
        {
            map.put(name, ctx);
        }
    }

    /**
     * Checks that no structures have duplicated field names.
     * In particular, a field in a structure or filter can't
     * have the same name as another field in the same
     * structure or filter, and can't have the same name
     * as a stream or structure.
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
     * Checks that statements exist in valid contexts for the
     * type of statement.  This checks that add, split, join,
     * loop, and body statements are only in appropriate
     * initialization code, and that push, pop, peek, and keep
     * statements are only in appropriate work function code.
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
}
