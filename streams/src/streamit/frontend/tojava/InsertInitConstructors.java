/*
 * InsertInitConstructors.java: insert object field constructors
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: InsertInitConstructors.java,v 1.1 2002-09-20 17:09:46 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Visitor class to insert statements in init functions that call
 * constructors for field declarations that map to Java objects.
 */
public class InsertInitConstructors extends FEReplacer
{
    // Hmm, should probably put this in a shared helper class;
    // this code copied verbatim from MoveStreamParamters.
    private Function findInit(FEContext context, List fns)
    {
        for (Iterator iter = fns.iterator(); iter.hasNext(); )
        {
            Function fn = (Function)iter.next();
            if (fn.getCls() == Function.FUNC_INIT)
                return fn;
        }
        
        // No init function; create an empty one.
        return Function.newInit(context,
                                new StmtBlock(context,
                                              Collections.EMPTY_LIST));
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        // Stop if there are no fields.
        if (spec.getVars().isEmpty())
            return spec;
        
        List newStmts = new ArrayList();
            
        // Walk through the variables.  If any of them are for
        // complex or non-primitive types, generate a constructor.
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            StmtVarDecl field = (StmtVarDecl)iter.next();
            Type type = field.getType();
            if (type.isComplex() || !(type instanceof TypePrimitive))
            {
                Statement constructor =
                    new StmtJavaConstructor(field.getContext(),
                                            field.getName(),
                                            field.getType());
                newStmts.add(constructor);
            }
        }

        // Stop if there are no constructors to generate.
        if (newStmts.isEmpty())
            return spec;
        
        // Okay.  Prepend the new statements to the init function.
        List newFuncs = new ArrayList(spec.getFuncs());
        Function init = findInit(spec.getContext(), spec.getFuncs());
        newFuncs.remove(init);
        StmtBlock body = (StmtBlock)init.getBody();
        newStmts.addAll(body.getStmts());
        Statement newBody = new StmtBlock(body.getContext(), newStmts);
        init = new Function(init.getContext(), init.getCls(),
                            init.getName(), init.getReturnType(),
                            init.getParams(), newBody);
        newFuncs.add(init);
        
        return new StreamSpec(spec.getContext(), spec.getType(),
                              spec.getStreamType(), spec.getName(),
                              spec.getParams(), spec.getVars(),
                              newFuncs);
    }
}
