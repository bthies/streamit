/*
 * InsertInitConstructors.java: insert object field constructors
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: InsertInitConstructors.java,v 1.3 2002-09-23 16:06:56 dmaze Exp $
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
public class InsertInitConstructors extends InitMunger
{
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
        newFuncs = replaceInitWithPrepended(spec.getContext(), newFuncs,
                                            newStmts);
        
        return new StreamSpec(spec.getContext(), spec.getType(),
                              spec.getStreamType(), spec.getName(),
                              spec.getParams(), spec.getVars(),
                              newFuncs);
    }
}
