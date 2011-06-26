package streamit.frontend.nodes;

/**
 * A StreamIt iter expression.  This returns the iteration count of
 * the filter containing it.
 *
 */
public class ExprIter extends Expression
{
    /**
     * Creates a new iter expression.
     *
     * @param context  file and line number of the expression
     */
     public ExprIter(FEContext context) {
         super(context);
     }

     @Override
     public Object accept(FEVisitor v) {
         return v.visitExprIter(this);
     }

     public String toString()
     {
         return "iter()";
     }
}
