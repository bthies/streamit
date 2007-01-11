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

package streamit.frontend.passes;

import streamit.frontend.tojava.*;
import streamit.frontend.nodes.*;

import java.util.*;

/**
 * Generate code to copy structures and arrays elementwise.  In StreamIt,
 * assigning one composite object to another copies all of its members
 * (there are no references); this pass makes that copying explicit.
 * It also generates temporary variables for push, pop, and peek
 * statements to ensure that languages with references do not see
 * false copies.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: GenerateCopies.java,v 1.18 2007-01-11 05:05:54 thies Exp $
 */
public class GenerateCopies extends SymbolTableVisitor
{
    private TempVarGen varGen;
    private boolean libraryFormat;
    private DetectImmutable immutableDetector;

    private String activeStreamName = null;

    /**
     * Create a new copy generator.
     *
     * @param varGen  global temporary variable generator
     * @param libraryFormat  whether or not we are generating code for Java library
     * @param immutableDetector the output of another pass recording which variables
     *                          are immutable.
     */
    public GenerateCopies(TempVarGen varGen,
                          boolean libraryFormat,
                          DetectImmutable immutableDetector)
    {
        super(null);
        this.varGen = varGen;
        this.libraryFormat = libraryFormat;
        this.immutableDetector = immutableDetector;
    }

    /**
     * Checks if variables of type type can be implemented as
     * a reference in Java or elsewhere.  This is true for arrays,
     * structures, and complex numbers.
     */
    private boolean needsCopy(Type type)
    {
        if (type instanceof TypeArray)
            return true;
        if (type instanceof TypeStruct)
            return structNeedsCopy((TypeStruct)type);
        if (type instanceof TypeStructRef)
            return true;
        if (type.isComplex()) 
            return true;
        if (type.isComposite()) 
            return false;
        return false;
    }

    /**
     * Returns whether or not a given structure type needs to be
     * copied.  A struct only needs to be copied if any of its
     * sub-types need to be copied (otherwise it should be passed by
     * value as a struct).  For example, if a member has an array
     * type, it needs to be copied so it won't share that pointer.
     */
    private boolean structNeedsCopy(TypeStruct type) {
        for (int i=0; i<type.getNumFields(); i++) {
            if (needsCopy(type.getType(type.getField(i)))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if the result of the given expression can be implemented
     * as a reference in Java or elsewhere.
     */
    private boolean needsCopy(Expression expr)
    {
        // don't generate copies for array initializers, since we
        // currently assume that they specify every literal in the
        // array (they don't contain variable references).
        if (expr instanceof ExprComposite) {
            return true;
        }

        if (expr instanceof ExprArrayInit) {
            return false;
        } else {
            if (getType(expr) == null) return false;
            return needsCopy(getType(expr));
        }
    }

    /**
     * Use <code>addStatement</code> to add a statement assigning
     * <code>expr</code> to a new temporary, and return the expression
     * for the temporary.
     *
     * @param expr  expression to copy
     * @param deep  if true, generate a deep copy, as in {@link makeCopy}.
     * @return      variable expression for the temporary
     */
    private Expression assignToTemp(Expression expr, boolean deep)
    {
        String tempName = varGen.nextVar();
        Expression tempVar = new ExprVar(expr.getContext(), tempName);
        Type type = getType(expr);
        addVarDecl(expr.getContext(), type, tempName);
        if (deep)
            makeCopy(expr, tempVar);
        else
            addStatement(new StmtAssign(expr.getContext(), tempVar, expr));
        return tempVar;
    }

    /**
     * Use <code>addStatement</code> to generate a deep copy of the
     * (idempotent) expression in <code>from</code> into the (lvalue)
     * expression in <code>to</code>.
     */
    private void makeCopy(Expression from, Expression to)
    {
        // Assume that from and to have the same type.  What are we copying?
        Type type = getType(from);
        if (type instanceof TypeArray)
            makeCopyArray(from, to, (TypeArray)type);
        else if (type instanceof TypeStruct)
            makeCopyStruct(from, to, (TypeStruct)type);
        else if (type.isComplex())
            makeCopyComplex(from, to);
        else if (type.isComposite())
            makeCopyComposite(from, to, ((TypePrimitive)type).getType());
        else
            addStatement(new StmtAssign(to.getContext(), to, from));
    }

    private void makeCopyArray(Expression from, Expression to, TypeArray type)
    {
        // We need to generate a for loop, since from our point of
        // view, the array bounds may not be constant.
        String indexName = varGen.nextVar();
        ExprVar index = new ExprVar(null, indexName);
        Type intType = new TypePrimitive(TypePrimitive.TYPE_INT);        
        Statement init =
            new StmtVarDecl(null, intType, indexName,
                            new ExprConstInt(null, 0));
        symtab.registerVar(indexName, intType, null, SymbolTable.KIND_LOCAL);
        Expression cond =
            new ExprBinary(null, ExprBinary.BINOP_LT, index, type.getLength());
        Statement incr =
            new StmtAssign(null, index,
                           new ExprBinary(null, ExprBinary.BINOP_ADD,
                                          index, new ExprConstInt(null, 1)));
        // Need to make a deep copy.  Existing machinery uses
        // addStatement(); visiting a StmtBlock will save this.
        // So, create a block containing a shallow copy, then
        // visit:
        Expression fel = new ExprArray(null, from, index);
        Expression tel = new ExprArray(null, to, index);
        Statement body =
            new StmtBlock(null,
                          Collections.singletonList((Statement)new StmtAssign(null,
                                                                   tel,
                                                                   fel)));
        body = (Statement)body.accept(this);

        // Now generate the loop, we have all the parts.

        addStatement(new StmtFor(null, init, cond, incr, body));
    }

    private void makeCopyStruct(Expression from, Expression to,
                                TypeStruct type)
    {
        for (int i = 0; i < type.getNumFields(); i++)
            {
                String fname = type.getField(i);
                makeCopy(new ExprField(from.getContext(), from, fname),
                         new ExprField(to.getContext(), to, fname));
            }
    }

    private void makeCopyComplex(Expression from, Expression to)
    {
        addStatement
            (new StmtAssign(to.getContext(),
                            new ExprField(to.getContext(), to, "real"),
                            new ExprField(from.getContext(), from, "real")));
        addStatement
            (new StmtAssign(to.getContext(),
                            new ExprField(to.getContext(), to, "imag"),
                            new ExprField(from.getContext(), from, "imag")));
    }

    
    private void makeCopyComposite(Expression from, Expression to, int type)
    {
        addStatement
            (new StmtAssign(to.getContext(),
                            new ExprField(to.getContext(), to, "x"),
                            from instanceof ExprComposite ? 
                            ((ExprComposite)from).getX() : new ExprField(from.getContext(), from, "x")));
        addStatement
            (new StmtAssign(to.getContext(),
                            new ExprField(to.getContext(), to, "y"),
                            from instanceof ExprComposite ? 
                            ((ExprComposite)from).getY() : new ExprField(from.getContext(), from, "y")));
        if (type == TypePrimitive.TYPE_FLOAT3 ||
            type == TypePrimitive.TYPE_FLOAT4)
            addStatement
                (new StmtAssign(to.getContext(),
                                new ExprField(to.getContext(), to, "z"),
                                from instanceof ExprComposite ? 
                                ((ExprComposite)from).getZ() : new ExprField(from.getContext(), from, "z")));
        if (type == TypePrimitive.TYPE_FLOAT4)
            addStatement
                (new StmtAssign(to.getContext(),
                                new ExprField(to.getContext(), to, "w"),
                                from instanceof ExprComposite ? 
                                ((ExprComposite)from).getW() : new ExprField(from.getContext(), from, "w")));
    }

    /**
     * Returns whether or not <exp> contains a function call.
     */
    private boolean containsFunCall(Expression expr) {
        final boolean[] result = { false };

        expr.accept(new FEReplacer() {
                public Object visitExprFunCall(ExprFunCall expr)
                {
                    result[0] = true;
                    // don't need to visit any further; we found what
                    // we were looking for
                    return expr;
                }
            });
    
        return result[0];
    }

    public Object visitExprPeek(ExprPeek expr)
    {
        Expression result = (Expression)super.visitExprPeek(expr);
        if (needsCopy(result))
            result = assignToTemp(result, false);
        return result;
    }
    
    public Object visitExprPop(ExprPop expr)
    {
        Expression result = (Expression)super.visitExprPop(expr);
        if (needsCopy(result))
            result = assignToTemp(result, false);
        return result;
    }

    public Object visitExprHelperCall(ExprHelperCall expr)
    {
        boolean hasChanged = false;
        Expression result = expr;
        List<Expression> newParams = new ArrayList<Expression>();
        for (Iterator iter = expr.getParams().iterator(); iter.hasNext(); )
            {
                Expression param = (Expression)iter.next();
                Expression newParam = param;
                /*
                  if (needsCopy(newParam))
                  newParam = assignToTemp(newParam, true, false);
                */
                newParam = doExpression(newParam);
                newParams.add(newParam);
                if (param != newParam) hasChanged = true;
            }
        if (hasChanged)
            result = new ExprHelperCall(expr.getContext(), 
                                        expr.getHelperPackage(), 
                                        expr.getName(), newParams);
        if (needsCopy(result))
            result = assignToTemp(result, false);
        return result;
    }

    public Object visitStmtAssign(StmtAssign stmt)
    {

        //System.out.println("GenCopies::visitStmtAssign"+
        //         " lhs: "+getType(stmt.getLHS())+
        //         " rhs: "+getType(stmt.getRHS())+" \n");

        // recurse:
        Statement result = (Statement)super.visitStmtAssign(stmt);
        if (result instanceof StmtAssign) // it probably is:
            {
                stmt = (StmtAssign)result;
        
                //System.out.println("GenCopies::visitStmtAssign take2 "+
                //             " lhs: "+getType(stmt.getLHS())+
                //             " rhs: "+getType(stmt.getRHS())+
                //             " Needs-copy: "+needsCopy(stmt.getRHS())+" \n");

                Expression rhs = stmt.getRHS();
                Expression lhs = stmt.getLHS();
                
                if (libraryFormat && 
                    lhs instanceof ExprVar &&
                    rhs instanceof ExprVar && 
                    immutableDetector.isImmutable(activeStreamName, 
                                                  ((ExprVar) lhs).getName()) &&
                    immutableDetector.isImmutable(activeStreamName,
                                                  ((ExprVar) rhs).getName())) {
                    return result;
                }
                if (needsCopy(rhs))
                    {

                        if (rhs instanceof ExprComposite) {

                            // if rhs is a float vector constant assigning to
                            // temporary variable will not work

                            makeCopy(rhs, stmt.getLHS());                

                        } else {

                            // if RHS is a function call, make a copy of it.
                            // We don't want to call the function multiple
                            // times for each element.
                            if (containsFunCall(rhs)) {
                                rhs = assignToTemp(stmt.getRHS(), 
                                                   // "true" as deep
                                                   // argument will cause
                                                   // bugs; need more
                                                   // sophisticated
                                                   // framework to do
                                                   // nested structures
                                                   // correctly
                                                   false);
                            }
            
                            // drops op!  If there are compound assignments
                            // like "a += b" here, we lose.  There shouldn't be,
                            // though, since those operators aren't well-defined
                            // for structures and arrays and this should be run
                            // after complex prop.
            
                            makeCopy(rhs, stmt.getLHS());                
        
                        }

                        return null;
                    }
            }
        return result;
    }

    public Object visitStmtPush(StmtPush expr)
    {
        Expression value = (Expression)expr.getValue().accept(this);
        if (needsCopy(value))
            value = assignToTemp(value, true);
        return new StmtPush(expr.getContext(), value);
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        // Set the active stream name so when we visit an array we know which
        // stream with which to associate it.
        activeStreamName = spec.getName();

        if (activeStreamName == null)
            throw new RuntimeException("Anonymous or improperly named stream. " +
                                       "This pass must run after all anonymous " +
                                       "streams have been given unique names.");

        return super.visitStreamSpec(spec);
    }
}
