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

package streamit.frontend.nodes;

import java.util.List;

/**
 * Visitor that returns the type of an expression.  This needs to be
 * created with a symbol table to help resolve the types of variables.
 * All of the visitor methods return <code>Type</code>s.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: GetExprType.java,v 1.11 2003-10-09 19:50:59 dmaze Exp $
 */
public class GetExprType extends FENullVisitor
{
    private SymbolTable symTab;
    private StreamType streamType;
    
    public GetExprType(SymbolTable symTab, StreamType streamType)
    {
        this.symTab = symTab;
        this.streamType = streamType;
    }
    
    public Object visitExprArray(ExprArray exp)
    {
        Type base = (Type)exp.getBase().accept(this);
        // ASSERT: base is a TypeArray.
        return ((TypeArray)base).getBase();
    }

    public Object visitExprBinary(ExprBinary exp)
    {
        // Comparison operators always return a boolean value.
        switch(exp.getOp())
        {
        case ExprBinary.BINOP_EQ:
        case ExprBinary.BINOP_NEQ:
        case ExprBinary.BINOP_LT:
        case ExprBinary.BINOP_LE:
        case ExprBinary.BINOP_GT:
        case ExprBinary.BINOP_GE:
            return new TypePrimitive(TypePrimitive.TYPE_BOOLEAN);
        }
        
        // Otherwise, this requires type unification.  Punt for the
        // moment (though it wouldn't actually be hard).
        return exp.getLeft().accept(this);
    }

    public Object visitExprComplex(ExprComplex exp)
    {
        return new TypePrimitive(TypePrimitive.TYPE_COMPLEX);
    }
    
    public Object visitExprConstBoolean(ExprConstBoolean exp)
    {
        return new TypePrimitive(TypePrimitive.TYPE_BOOLEAN);
    }

    public Object visitExprConstChar(ExprConstChar exp)
    {
        // return new TypePrimitive(TypePrimitive.TYPE_CHAR);
        return null;
    }
    
    public Object visitExprConstFloat(ExprConstFloat exp)
    {
        return new TypePrimitive(TypePrimitive.TYPE_FLOAT);
    }

    public Object visitExprConstInt(ExprConstInt exp)
    {
        return new TypePrimitive(TypePrimitive.TYPE_INT);
    }
    
    public Object visitExprConstStr(ExprConstStr exp)
    {
        // return new TypePrimitive(TypePrimitive.TYPE_STRING);
        return null;
    }
    
    public Object visitExprField(ExprField exp)
    {
        Type base = (Type)exp.getLeft().accept(this);
        // If the base is a complex type, a field of it is double.
        if (base.isComplex())
            return new TypePrimitive(TypePrimitive.TYPE_DOUBLE);
        // ASSERT: base is a TypeStruct.
        return ((TypeStruct)base).getType(exp.getName());
    }

    public Object visitExprFunCall(ExprFunCall exp)
    {
        // Has SymbolTable given us a function declaration?
        try
        {
            Function fn = symTab.lookupFn(exp.getName());
            return fn.getReturnType();
        } catch (UnrecognizedVariableException e) {
            // ignore
        }
        
        // Otherwise, we can assume that the only function calls are
        // calls to built-in functions in the absence of helper
        // function support in the parser.  These by and large have a
        // signature like
        //
        //   template<T> T foo(T);
        //
        // So, if there's any arguments, return the type of the first
        // argument; otherwise, return float as a default.
        List params = exp.getParams();
        if (params.isEmpty())
            return new TypePrimitive(TypePrimitive.TYPE_FLOAT);
        return ((Expression)params.get(0)).accept(this);
    }
    
    public Object visitExprPeek(ExprPeek exp)
    {
        return streamType.getIn();
    }
    
    public Object visitExprPop(ExprPop exp)
    {
        return streamType.getIn();
    }
    
    public Object visitExprTernary(ExprTernary exp)
    {
        // Again, should do type unification on the two sides.
        // And might not want to blindly assert ?:.
        return exp.getB().accept(this);
    }

    public Object visitExprTypeCast(ExprTypeCast exp)
    {
        return exp.getType();
    }

    public Object visitExprUnary(ExprUnary exp)
    {
        // A little more solid ground here: the type of -foo and !foo
        // will probably always be the same as the type of foo.
        return exp.getExpr().accept(this);
    }
    
    public Object visitExprVar(ExprVar exp)
    {
        // Look this up in the symbol table.
        return symTab.lookupVar(exp.getName());
    }
}
