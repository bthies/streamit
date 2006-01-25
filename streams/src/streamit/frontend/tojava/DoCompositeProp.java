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
import streamit.frontend.passes.SymbolTableVisitor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.ArrayList;

/*
 * This class replaces vector aritmetic with appropriate calls to
 * StreamItvectorLibrary
 *
 * @author Janis Sermulins
 */

public class DoCompositeProp extends SymbolTableVisitor
{

    private static Type floattype =
        new TypePrimitive(TypePrimitive.TYPE_FLOAT);
    private static Type float2type =
        new TypePrimitive(TypePrimitive.TYPE_FLOAT2);
    private static Type float3type =
        new TypePrimitive(TypePrimitive.TYPE_FLOAT3);
    private static Type float4type =
        new TypePrimitive(TypePrimitive.TYPE_FLOAT4);
    
    public DoCompositeProp(TempVarGen varGen)
    {
        super(null, null);
    }


    public Object visitStmtAssign(StmtAssign stmt)
    {
        Expression lhs = stmt.getLHS();
        Expression rhs = stmt.getRHS();

        Expression new_rhs = (Expression)stmt.getRHS().accept(this);

        if (!new_rhs.equals(rhs)) {
            return new StmtAssign(stmt.getContext(), lhs, new_rhs, stmt.getOp());
        } else {
            return stmt;
        }
    }


    public Object visitExprUnary(ExprUnary unary) {

        Expression expr = (Expression)unary.getExpr().accept(this);
        FEContext ctx = unary.getContext();
        Type t = getType(expr);


        if (t.equals(float2type) || 
            t.equals(float3type) ||
            t.equals(float4type)) {
        
            int dim = 0;
            if (t.equals(float2type)) dim = 2;
            if (t.equals(float3type)) dim = 3;
            if (t.equals(float4type)) dim = 4;

            if (unary.getOp() == ExprUnary.UNOP_NEG) {
                List params = new ArrayList();
                params.add(expr);
                return new ExprHelperCall(ctx, "StreamItVectorLib", "neg"+dim, params);
            }
        }

        return new ExprUnary(ctx, unary.getOp(), expr);
    }


    public Object visitExprBinary(ExprBinary exp) {

        Expression left = (Expression)exp.getLeft().accept(this);
        Expression right = (Expression)exp.getRight().accept(this);
        FEContext ctx = exp.getContext();

        Type lt = getType(left);
        Type rt = getType(right);

        if ((lt.equals(float2type) && rt.equals(float2type)) ||
            (lt.equals(float3type) && rt.equals(float3type)) ||
            (lt.equals(float4type) && rt.equals(float4type))) {
        
            int dim = 0;
            if (lt.equals(float2type)) dim = 2;
            if (lt.equals(float3type)) dim = 3;
            if (lt.equals(float4type)) dim = 4;

            if (exp.getOp() == ExprBinary.BINOP_ADD) {
                List params = new ArrayList();
                params.add(left); params.add(right);
                return new ExprHelperCall(ctx, "StreamItVectorLib", "add"+dim, params);
            }
            if (exp.getOp() == ExprBinary.BINOP_SUB) {
                List params = new ArrayList();
                params.add(left); params.add(right);
                return new ExprHelperCall(ctx, "StreamItVectorLib", "sub"+dim, params);
            }
            if (exp.getOp() == ExprBinary.BINOP_MUL) {
                List params = new ArrayList();
                params.add(left); params.add(right);
                return new ExprHelperCall(ctx, "StreamItVectorLib", "mul"+dim, params);
            }
            if (exp.getOp() == ExprBinary.BINOP_DIV) {
                List params = new ArrayList();
                params.add(left); params.add(right);
                return new ExprHelperCall(ctx, "StreamItVectorLib", "div"+dim, params);
            }

            assert false:
                ctx+": Unsupported vector arithmetic operation";

        }

        if ((lt.equals(float2type) || 
             lt.equals(float3type) || 
             lt.equals(float4type))
            && rt.equals(floattype)) {

            int dim = 0;
            if (lt.equals(float2type)) dim = 2;
            if (lt.equals(float3type)) dim = 3;
            if (lt.equals(float4type)) dim = 4;
        
            if (exp.getOp() == ExprBinary.BINOP_ADD) {
                List params = new ArrayList();
                params.add(left); params.add(right);
                return new ExprHelperCall(ctx, "StreamItVectorLib", "addScalar"+dim, params);
            }
            if (exp.getOp() == ExprBinary.BINOP_SUB) {
                List params = new ArrayList();
                params.add(left); params.add(right);
                return new ExprHelperCall(ctx, "StreamItVectorLib", "subScalar"+dim, params);
            }
            if (exp.getOp() == ExprBinary.BINOP_MUL) {
                List params = new ArrayList();
                params.add(left); params.add(right);
                return new ExprHelperCall(ctx, "StreamItVectorLib", "scale"+dim, params);
            }
            if (exp.getOp() == ExprBinary.BINOP_DIV) {
                List params = new ArrayList();
                params.add(left); params.add(right);
                return new ExprHelperCall(ctx, "StreamItVectorLib", "scaleInv"+dim, params);
            }

            assert false:
                ctx+": Unsupported vector arithmetic operation";
        
        }

        if (lt.equals(floattype) && (rt.equals(float2type) ||
                                     rt.equals(float3type) ||
                                     rt.equals(float4type))) {

            int dim = 0;
            if (rt.equals(float2type)) dim = 2;
            if (rt.equals(float3type)) dim = 3;
            if (rt.equals(float4type)) dim = 4;
        
            if (exp.getOp() == ExprBinary.BINOP_ADD) {
                List params = new ArrayList();
                params.add(right); params.add(left);
                return new ExprHelperCall(ctx, "StreamItVectorLib", "addScalar"+dim, params);
            }
            if (exp.getOp() == ExprBinary.BINOP_SUB) {
                List params = new ArrayList();
                params.add(right); params.add(left);
                return new ExprHelperCall(ctx, "StreamItVectorLib", "subScalar"+dim, params);
            }
            if (exp.getOp() == ExprBinary.BINOP_MUL) {
                List params = new ArrayList();
                params.add(right); params.add(left);
                return new ExprHelperCall(ctx, "StreamItVectorLib", "scale"+dim, params);
            }

            assert false:
                ctx+": Unsupported vector arithmetic operation";

        }

        return new ExprBinary(ctx, exp.getOp(), left, right); 
    }

    public Object visitExprFunCall(ExprFunCall exp)
    {
        FEContext ctx = exp.getContext();

        // Start by resolving all of the parameters.
        List newParams = new ArrayList();
        Iterator iter = exp.getParams().iterator();
        while (iter.hasNext()) {
            Expression param = (Expression)iter.next();
            newParams.add(param.accept(this));
        }

        Type pt = null;
        int dim = 0;

        if (newParams.size() > 0) {
            pt = getType((Expression)newParams.get(0));
            if (pt.equals(float2type)) dim = 2;
            if (pt.equals(float3type)) dim = 3;
            if (pt.equals(float4type)) dim = 4;
        }
        
        if (exp.getName().equals("floor") && newParams.size() == 1 & dim >= 2) {
            return new ExprHelperCall(ctx, "StreamItVectorLib", "floor" + dim, newParams);
        }
        if (exp.getName().equals("normalize") && newParams.size() == 1 & dim >= 2) {
            return new ExprHelperCall(ctx, "StreamItVectorLib", "normalize" + dim, newParams);
        }

        return new ExprFunCall(exp.getContext(), exp.getName(), newParams);
    }


}



