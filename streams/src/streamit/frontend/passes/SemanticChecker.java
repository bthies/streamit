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
 * @version $Id: SemanticChecker.java,v 1.2 2003-07-08 21:40:26 dmaze Exp $
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
        return checker.good;
    }
    
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
                checkADupFieldName(localNames, streamNames,
                                   func.getName(), func.getContext());
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
}
