/*
 * GetExprType.java: get the type of an expression
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: GetExprType.java,v 1.1 2002-07-15 19:52:48 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * Visitor that returns the type of an expression.  This needs to be created
 * with a symbol table to help resolve the types of variables.  All of the
 * visitor methods return Types.
 */
public class GetExprType implements FEVisitor
{
    private SymbolTable symTab;
    
    public GetExprType(SymbolTable st)
    {
        symTab = st;
    }
    
    public Object visitExprArray(ExprArray exp)
    {
        Type base = (Type)exp.getBase().accept(this);
        // ASSERT: base is a TypeArray.
        return ((TypeArray)base).getBase();
    }

    public Object visitExprBinary(ExprBinary exp)
    {
        // This requires type unification.  Punt for the moment (though
        // it wouldn't actually be hard).
        return exp.getLeft().accept(this);
    }

    public Object visitExprComplex(ExprComplex exp)
    {
        return new TypePrimitive(TypePrimitive.TYPE_COMPLEX);
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
        // ASSERT: base is a TypeStruct.
        return ((TypeStruct)base).getType(exp.getName());
    }

    public Object visitExprFunCall(ExprFunCall exp)
    {
        // Errk.  We need to keep track of function type signatures here,
        // huh.  Need this for complex propagation, actually.
        return null;
    }
    
    public Object visitExprPeek(ExprPeek exp)
    {
        // Umm, and need the stream type for this and pop.
        return null;
    }
    
    public Object visitExprPop(ExprPop exp)
    {
        return null;
    }
    
    public Object visitExprTernary(ExprTernary exp)
    {
        // Again, should do type unification on the two sides.
        // And might not want to blindly assert ?:.
        return exp.getB().accept(this);
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
        return symTab.lookup(exp.getName());
    }
}
