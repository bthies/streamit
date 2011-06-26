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

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;
import streamit.frontend.passes.*;

import java.util.*;

/**
 * Inserts statements in init functions to call member object constructors.
 * 
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: InsertInitConstructors.java,v 1.23 2006-04-13 02:32:55 madrake Exp $
 */
public class InsertInitConstructors extends InitMunger
{
    private TempVarGen varGen;
    private boolean libraryFormat;
    private DetectImmutable immutableDetector;

    private String activeStreamName = null;
    private boolean special_global = false;

    /**
     * Create a new pass to insert constructors.
     *
     * @param varGen         global object to generate variable names
     * @param libraryFormat  whether or not we are generating code for Java library
     * @param immutableDetector the output of another pass recording which variables are 
     *                          immutable
     */
    public InsertInitConstructors(TempVarGen varGen, 
                                  boolean libraryFormat, 
                                  DetectImmutable immutableDetector)
    {
        this.varGen = varGen;
        this.libraryFormat = libraryFormat;
        this.immutableDetector = immutableDetector;
    }
    public InsertInitConstructors(TempVarGen varGen,
                                  boolean libraryFormat) 
    {
        this(varGen, libraryFormat, new DetectImmutable());
    }
    
    /**
     * Returns true if this type needs a constructor generated.
     * This happens if the type is complex, or if it is not a
     * primitive type.  (Complex primitive types use the Java
     * 'Complex' class.) Arrays which are immutable should not
     * get constructors, although this is only checked in the 
     * library for now.
     *
     * Call the version with the initializer when possible; if no
     * initializer available, will assume to be null.
     */
    private boolean needsConstructor(Type type)
    {
        return needsConstructor(type, null);
    }
    private boolean needsConstructor(Type type, Expression initializer) 
    {
        return (type.isComplex() || 
                type.isComposite() || 
                (!(type instanceof TypePrimitive) && 
                 // compiler doesn't need constructors for any arrays,
                 // because we now declare them with static bounds
                 // (e.g., int x[10][10]).
                 ((!libraryFormat && !(type instanceof TypeArray)) ||
                  // library still needs constructors for all arrays
                  // except for those that have initializers
                  (libraryFormat && !(type instanceof TypeArray && initializer!=null)))));
    }

    /**
     * Return an ordered list of all of the constructors that need to
     * be generated to initialize a particular variable.
     *
     * @param ctx   file name and line number for created statements
     * @param name  base name expression
     * @param type  type of object to create constructors for
     * @param arrayConstructor if false, only recurse, don't actually
     *              generate a constructor for name if it is an array
     *              type; useful for members of multidimensional
     *              arrays
     */
    private List stmtsForConstructor(FEContext ctx, Expression name,
                                     Type type, Expression init, 
                                     boolean arrayConstructor)
    {
        List result = new java.util.ArrayList();

        // If the type doesn't involve a constructor, there are no
        // generated statements.
        
        String field_name = null;
        if (name instanceof ExprVar)
            field_name = ((ExprVar) name).getName();

        if (!needsConstructor(type, init))
            return result;

        // No; generate the constructor.
        if (!libraryFormat || (arrayConstructor || !(type instanceof TypeArray))) 
            {
                // Instead of constructing here, as in new int[][],
                // it should just assign the variable to reference
                // a global copy initialized to zero.
                if (libraryFormat && 
                    !special_global && 
                    type instanceof TypeArray && 
                    field_name != null &&  
                    activeStreamName != null && 
                    immutableDetector.isImmutable(activeStreamName, field_name))
                    {
                        String tempVar = varGen.nextVar();
                        Expression varExp = new ExprVar(ctx, tempVar);
                        List elements = new ArrayList();
                        Type base = type;
                        while (base instanceof TypeArray) 
                        {
                            elements.add(elements.size(),
                                         ((TypeArray) base).getLength()
                                );
                            base = ((TypeArray) base).getBase();
                        }
                        result.add(
                            new StmtVarDecl(
                                ctx,
                                new TypeArray(new TypePrimitive(TypePrimitive.TYPE_INT),
                                              new ExprConstInt(elements.size())),
                                tempVar,
                                new ExprArrayInit(ctx,
                                                  elements))
                            );
                        result.add(
                            new StmtAssign(
                                ctx,
                                name,
                                new ExprTypeCast(
                                    ctx,
                                    type,
                                    new ExprFunCall(
                                        ctx,
                                        "ArrayMemoizer.initArray",
                                        new ExprJavaConstructor(
                                            ctx,
                                            new TypeArray(((TypeArray) type).getComponent(), new ExprConstInt(0))
                                            ),
                                        varExp
                                        )
                                    )
                                )
                            );
                    }
                else
                    {
                        result.add(new StmtAssign(ctx, name,
                                                  new ExprJavaConstructor(ctx, type)));
                    }
            }

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
                                result.addAll(stmtsForConstructor(ctx, lhs, ftype, null, true));
                            }
                    }
            }
        // Or, if this is an array of structures, we might need to
        // recursively generate constructors.

        if (libraryFormat && (type instanceof TypeArray))
            {
                TypeArray ta = (TypeArray) type;
                Type base = ta.getBase();
                if (needsConstructor(base))
                    {
                        // The length might be non-constant.  This means that
                        // we need to do this by looping through the array.
                        String tempVar = varGen.nextVar();
                        Expression varExp = new ExprVar(ctx, tempVar);
                        Statement decl =
                            new StmtVarDecl(ctx,
                                            new TypePrimitive(TypePrimitive.TYPE_INT),
                                            tempVar,
                                            new ExprConstInt(ctx, 0));
                        Expression cond =
                            new ExprBinary(ctx,
                                           ExprBinary.BINOP_LT,
                                           varExp,
                                           ta.getLength());
                        Statement incr =
                            new StmtExpr(ctx,
                                         new ExprUnary(ctx,
                                                       ExprUnary.UNOP_POSTINC,
                                                       varExp));
                        Expression lhs = new ExprArray(ctx, name, varExp);
                        Statement body =
                            new StmtBlock(ctx,
                                          stmtsForConstructor(ctx, lhs, base, null, false));
                        Statement loop =
                            new StmtFor(ctx, decl, cond, incr, body);
                        result.add(loop);
                    }
            }
        
        return result;
    }
    
    public Object visitStreamSpec(StreamSpec spec)
    {
        // Set the active stream name so when we visit an array we know which
        // stream with which to associate it.
        activeStreamName = spec.getName();
        special_global = (spec.getType() == StreamSpec.STREAM_GLOBAL);

        if (activeStreamName == null)
            throw new RuntimeException("Anonymous or improperly named stream. " +
                                       "This pass must run after all anonymous " +
                                       "streams have been given unique names.");
       
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
                        Expression init = field.getInit(i);
                        String field_name = field.getName(i);
                        if (needsConstructor(type, init))
                            {
                                FEContext ctx = field.getContext();
                                Expression lhs = new ExprVar(ctx, field.getName(i));
                                newStmts.addAll(stmtsForConstructor(ctx, lhs, type, init, true));
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
                              newFuncs, spec.isStateful());
    }

    public Object visitStmtVarDecl(StmtVarDecl decl)
    {
        // Prepass: check all of the types in the declaration.
        // If none of them need constructors, don't actually
        // go through with this.  (So what?  This hack lets
        // the code work correctly even with a variable declaration
        // in the initializer part of a for loop, otherwise the
        // declaration gets pulled out of the loop and null replaces
        // the initializer.)
        boolean needed = false;
        for (int i = 0; i < decl.getNumVars(); i++)
            if (needsConstructor(decl.getType(i), 
                                 decl.getInit(i)))
                needed = true;
        if (!needed)
            return decl;
        
        // We're not actually going to modify this declaration, but
        // we may generate some additional statements (for constructors)
        // that go after it.
        addStatement(decl);
        
        // So, now go through the list of all the variables and add
        // constructors as needed:
        for (int i = 0; i < decl.getNumVars(); i++)
            {
                Type type = decl.getType(i);
                Expression init = decl.getInit(i);
                if (needsConstructor(type, init))
                    {
                        FEContext ctx = decl.getContext();
                        Expression lhs = new ExprVar(ctx, decl.getName(i));
                        addStatements(stmtsForConstructor(ctx, lhs, type, init, 
                                                          true));
                    }
            }

        // We already added the statement, return null so there aren't
        // duplicate declarations.
        return null;
    }
}
