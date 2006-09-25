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
import java.util.Map;

/**
 * Visitor that returns the type of an expression.  This needs to be
 * created with a symbol table to help resolve the types of variables.
 * All of the visitor methods return <code>Type</code>s.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: GetExprType.java,v 1.26 2006-09-25 13:54:54 dimock Exp $
 */
public class GetExprType extends FENullVisitor
{
    private SymbolTable symTab;
    private StreamType streamType;
    private Map<String, TypeStruct> structsByName;
    private Map<String, TypeHelper> helpersByName;
    
    public GetExprType(SymbolTable symTab, StreamType streamType,
                       Map<String, TypeStruct> structsByName, Map<String, TypeHelper> helpersByName)
    {
        this.symTab = symTab;
        this.streamType = streamType;
        this.structsByName = structsByName;
        this.helpersByName = helpersByName;
    }
    
    public Object visitExprArray(ExprArray exp)
    {
        Type base = (Type)exp.getBase().accept(this);
        // ASSERT: base is a TypeArray.
        return ((TypeArray)base).getBase();
    }

    public Object visitExprArrayInit(ExprArrayInit exp)
    {
        // want to determine these about the array
        Type base;
        int length;

        // get the elements
        List<Expression> elems = exp.getElements();

        // not sure what to do for base type if array is empty... try
        // keeping it null --BFT
        if (elems.size()==0) {
            base = null;
        } else {
            // otherwise, take promotion over all elements declared
            base = (Type)elems.get(0).accept(this);
        
            for (int i=1; i<elems.size(); i++) {
                Type t = (Type)elems.get(i).accept(this);
                base = t.leastCommonPromotion(t);
            }
        }
    
        return new TypeArray(base, new ExprConstInt(elems.size()));
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
        
        // The type of the expression is some type that both sides
        // promote to, otherwise.
        Type tl = (Type)exp.getLeft().accept(this);
        Type tr = (Type)exp.getRight().accept(this);

        return tl.leastCommonPromotion(tr);
    }

    public Object visitExprComplex(ExprComplex exp)
    {
        return new TypePrimitive(TypePrimitive.TYPE_COMPLEX);
    }

    public Object visitExprComposite(ExprComposite exp)
    {
        int dim = exp.getDim();
        assert (dim >= 2 && dim <= 4); 
        if (dim == 2) return new TypePrimitive(TypePrimitive.TYPE_FLOAT2);
        if (dim == 3) return new TypePrimitive(TypePrimitive.TYPE_FLOAT3);
        return new TypePrimitive(TypePrimitive.TYPE_FLOAT4);
    }
    
    public Object visitExprConstBoolean(ExprConstBoolean exp)
    {
        return new TypePrimitive(TypePrimitive.TYPE_BOOLEAN);
    }

    public Object visitExprConstChar(ExprConstChar exp)
    {
        return new TypePrimitive(TypePrimitive.TYPE_CHAR);
    }
    
    public Object visitExprConstFloat(ExprConstFloat exp)
    {
        return new TypePrimitive(TypePrimitive.TYPE_FLOAT);
    }

    public Object visitExprConstInt(ExprConstInt exp)
    {
        // return a bit type if the value is 0 or 1
        if (exp.getVal()==0 || exp.getVal()==1) {
            return new TypePrimitive(TypePrimitive.TYPE_BIT);
        } else {
            return new TypePrimitive(TypePrimitive.TYPE_INT);
        }
    }
    
    public Object visitExprConstStr(ExprConstStr exp)
    {
        return new TypePrimitive(TypePrimitive.TYPE_STRING);
    }
    
    public Object visitExprDynamicToken(ExprDynamicToken exp) 
    {
        return new TypePrimitive(TypePrimitive.TYPE_INT);
    }

    public Object visitExprField(ExprField exp)
    {
        if (exp.getLeft().toString().equals("TheGlobal") ||
            exp.getLeft().toString().equals("TheGlobal.__get_instance()")) {
            return symTab.lookupVar(exp.getName());
        }
        Type base = (Type)exp.getLeft().accept(this);
        // If the base is a complex type, a field of it is float.
        if (base.isComplex())
            return new TypePrimitive(TypePrimitive.TYPE_FLOAT);
        // If the base is a composite type, a field of it is float.
        if (base.isComposite())
            return new TypePrimitive(TypePrimitive.TYPE_FLOAT);
        else if (base instanceof TypeStruct)
            return ((TypeStruct)base).getType(exp.getName());
        else if (base instanceof TypeStructRef)
            {
                String name = ((TypeStructRef)base).getName();
                TypeStruct str = structsByName.get(name);
                assert str != null : base;
                return str.getType(exp.getName());
            }
        else
            {
                assert false : base;
                return null;
            }
    }

    public Object visitExprHelperCall(ExprHelperCall helper)
    {

        TypeHelper th = helpersByName.get(helper.getHelperPackage());
        for (int i = 0; th != null && i < th.getNumFuncs(); i++) {
            Function func = th.getFunction(i);
            if (func.getName().equals(helper.getName()))
                return func.getReturnType();
        }
    
        if (helper.getHelperPackage().equals("StreamItVectorLib")) {
            String name = helper.getName();
            if (name.startsWith("dot") || 
                name.startsWith("sqrtDist") )
                return new TypePrimitive(TypePrimitive.TYPE_FLOAT);
            if (name.indexOf("Than") != -1 )
                return new TypePrimitive(TypePrimitive.TYPE_BOOLEAN);
            Object o;

            if (name.indexOf("2") != -1)
                return new TypePrimitive(TypePrimitive.TYPE_FLOAT2);
            if (name.indexOf("3") != -1)
                return new TypePrimitive(TypePrimitive.TYPE_FLOAT3);
            if (name.indexOf("4") != -1)
                return new TypePrimitive(TypePrimitive.TYPE_FLOAT4);

        }

        throw new RuntimeException("No such helper call exists:" + 
                                   helper.getHelperPackage() + '.' + 
                                   helper.getName());
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

        // "abs" returns a float.  We should probably insert other
        // special cases here for built-in functions, but I'm not
        // exactly sure which ones have a constant return type and
        // which ones are polymorphic.  --BFT
        if (exp.getName().equals("abs")) {
            return new TypePrimitive(TypePrimitive.TYPE_FLOAT);
        }

        // account for array initializers:
        //
        //   int[size] init_array_1D_int(String filename, int size);
        //   float[size] init_array_1D_float(String filename, int size);
        //
        // The length is the second argument.
        if (exp.getName().indexOf("1D_int")>0) {
            Type base = new TypePrimitive(TypePrimitive.TYPE_INT);
            Expression length = (Expression)exp.getParams().get(1);
            return new TypeArray(base, length);
        }
        if (exp.getName().indexOf("1D_float")>0) {
            Type base = new TypePrimitive(TypePrimitive.TYPE_FLOAT);
            Expression length = (Expression)exp.getParams().get(1);
            return new TypeArray(base, length);
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
        List<Object> params = exp.getParams();
        if (params.isEmpty()) {
            return new TypePrimitive(TypePrimitive.TYPE_FLOAT);
        }
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
    
    public Object visitExprRange(ExprRange exp)
    {
        return new TypePrimitive(TypePrimitive.TYPE_INT);
    }

    public Object visitExprTernary(ExprTernary exp)
    {
        // Do type unification on the two sides.
        // (Might not want to blindly assert ?:.)
        Type tb = (Type)exp.getB().accept(this);
        Type tc = (Type)exp.getC().accept(this);
        return tb.leastCommonPromotion(tc);
    }

    public Object visitExprTypeCast(ExprTypeCast exp)
    {
        return exp.getType();
    }

    public Object visitExprUnary(ExprUnary exp)
    {
        // A little more solid ground here: the type of -foo and !foo
        // will be the same as type of foo, except for bits...
        Type t = (Type)exp.getExpr().accept(this);
        // if <t> is a bit, then the unary expression could promote it
        // to an int.
        if (t.equals(TypePrimitive.bittype)) {
            switch (exp.getOp()) {
            case ExprUnary.UNOP_COMPLEMENT:
            case ExprUnary.UNOP_NOT: {
                // it is still a bit if it is negated, I think.
                return TypePrimitive.bittype;
            }
            case ExprUnary.UNOP_NEG:
            case ExprUnary.UNOP_PREINC:
            case ExprUnary.UNOP_POSTINC:
            case ExprUnary.UNOP_PREDEC: 
            case ExprUnary.UNOP_POSTDEC: {
                return TypePrimitive.inttype;
            }
            }
        }
        return t;
    }

    
    public Object visitExprVar(ExprVar exp)
    {
        // Look this up in the symbol table.
        return symTab.lookupVar(exp.getName());
    }
}
