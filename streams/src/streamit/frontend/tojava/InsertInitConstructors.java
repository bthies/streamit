package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Inserts statements in init functions to call member object constructors.
 * 
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: InsertInitConstructors.java,v 1.9 2003-06-25 15:33:23 dmaze Exp $
 */
public class InsertInitConstructors extends InitMunger
{
    /**
     * Returns true if this type needs a constructor generated.
     * This happens if the type is complex, or if it is not a
     * primitive type.  (Complex primitive types use the Java
     * 'Complex' class.)
     */
    private static boolean needsConstructor(Type type)
    {
        return type.isComplex() || !(type instanceof TypePrimitive);
    }

    /**
     * Return an ordered list of all of the constructors that need to
     * be generated to initialize a particular variable.
     */
    static private List stmtsForConstructor(FEContext ctx,
                                            Expression name, Type type)
    {
        List result = new java.util.ArrayList();

        // If the type doesn't involve a constructor, there are no
        // generated statements.
        if (!needsConstructor(type))
            return result;

        // No; generate the constructor.
        result.add(new StmtJavaConstructor(ctx, name, type));

        // Now, if this is a structure type, we might need to
        // recursively generate constructors for the structure
        // members.
        if (type instanceof TypeStruct)
        {
            TypeStruct ts = (TypeStruct)type;
            for (int i = 0; i < ts.getNumFields(); i++)
            {
                String fname = ts.getField(i);
                Type ftype = ts.getType(fname);
                if (needsConstructor(ftype))
                {
                    // Construct the new left-hand side:
                    Expression lhs = new ExprField(ctx, name, fname);
                    // Get child constructors and add them:
                    result.addAll(stmtsForConstructor(ctx, lhs, ftype));
                }
            }
        }
        
        return result;
    }
    
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
                if (needsConstructor(type))
                {
                    FEContext ctx = field.getContext();
                    Expression lhs = new ExprVar(ctx, field.getName(i));
                    newStmts.addAll(stmtsForConstructor(ctx, lhs, type));
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

    public Object visitStmtVarDecl(StmtVarDecl decl)
    {
        // We're not actually going to modify this declaration, but
        // we may generate some additional statements (for constructors)
        // that go after it.
        addStatement(decl);
        
        // So, now go through the list of all the variables and add
        // constructors as needed:
        for (int i = 0; i < decl.getNumVars(); i++)
        {
            Type type = decl.getType(i);
            if (needsConstructor(type))
            {
                FEContext ctx = decl.getContext();
                Expression lhs = new ExprVar(ctx, decl.getName(i));
                addStatements(stmtsForConstructor(ctx, lhs, type));
            }
        }

        // We already added the statement, return null so there aren't
        // duplicate declarations.
        return null;
    }
}
