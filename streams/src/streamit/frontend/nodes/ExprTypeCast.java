package streamit.frontend.nodes;

/**
 * An expression directing one expression to be interpreted as a different
 * (primitive) type.  This has a child instruction and the type that is
 * being cast to.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprTypeCast.java,v 1.1 2003-05-13 22:42:57 dmaze Exp $
 */
public class ExprTypeCast extends Expression
{
    private Type type;
    private Expression expr;
    
    /**
     * Create a new ExprTypeCast with a specified type and child
     * expression.
     *
     * @param context  Context indicating file and line number
     *                 this expression was created in
     * @param type     Type the expression is being cast to
     * @param expr     Expression being cast
     */
    public ExprTypeCast(FEContext context, Type type, Expression expr)
    {
        super(context);
        this.type = type;
        this.expr = expr;
    }
    
    /**
     * Get the type the expression is being cast to.
     *
     * @return  Type the expression is cast to
     */
    public Type getType()
    {
        return type;
    }
    
    /**
     * Get the expression being cast.
     *
     * @return  The expression being cast
     */
    public Expression getExpr()
    {
        return expr;
    }
    
    /**
     * Accept a front-end visitor.
     */
    public Object accept(FEVisitor v)
    {
        return v.visitExprTypeCast(this);
    }
}

