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
	    return new StmtAssign(stmt.getContext(), lhs, new_rhs);
	} else {
	    return stmt;
	}
    }


    public Object visitExprBinary(ExprBinary exp) {

        Expression left = (Expression)exp.getLeft().accept(this);
        Expression right = (Expression)exp.getRight().accept(this);
	FEContext ctx = exp.getContext();

	Type lt = getType(left);
	Type rt = getType(right);

	Type floattype =
	    new TypePrimitive(TypePrimitive.TYPE_FLOAT);
	Type float2type =
	    new TypePrimitive(TypePrimitive.TYPE_FLOAT2);
	Type float3type =
	    new TypePrimitive(TypePrimitive.TYPE_FLOAT3);
	Type float4type =
	    new TypePrimitive(TypePrimitive.TYPE_FLOAT4);

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

    	return exp; // doExprProp(exp);
    }
}



