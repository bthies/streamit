package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Inserts statements in init functions to call member object constructors.
 * 
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: InsertInitConstructors.java,v 1.7 2003-05-13 19:32:54 dmaze Exp $
 */
public class InsertInitConstructors extends InitMunger
{
    public Object visitStreamSpec(StreamSpec spec)
    {
        spec = (StreamSpec)super.visitStreamSpec(spec);

        // Stop if there are no fields.
        if (spec.getVars().isEmpty())
            return spec;
        
        List newStmts = new ArrayList();
            
        // Walk through the variables.  If any of them are for
        // complex or non-primitive types, generate a constructor.
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            FieldDecl field = (FieldDecl)iter.next();
            for (int i = 0; i < field.getNumFields(); i++)
            {
                Type type = field.getType(i);
                if (type.isComplex() || !(type instanceof TypePrimitive))
                {
                    Statement constructor =
                        new StmtJavaConstructor(field.getContext(),
                                                field.getName(i),
                                                field.getType(i));
                    newStmts.add(constructor);
                }
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
