package streamit.frontend.nodes;

/**
 * A complex-valued expression.  This has two child expressions, which
 * are the real and imaginary parts.  Either child may be null, which
 * corresponds to a value of zero for that part.  (So a pure-real value
 * represented as an ExprComplex would have a null imaginary part.)  This
 * is intended to be used to construct simple expressions from the parse
 * tree, and then combine them into more complicated expressions with
 * fully-expanded real and imaginary parts.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprComplex.java,v 1.3 2003-06-30 20:23:12 dmaze Exp $
 */
public class ExprComplex extends Expression
{
    private Expression real, imag;
    
    /** Create a new ExprComplex with the specified real and imaginary
     * parts.  Either of real or imag may be null; see the class
     * description for details. */
    public ExprComplex(FEContext context, Expression real, Expression imag)
    {
        super(context);
        this.real = real;
        this.imag = imag;
    }
    
    /** Returns the real part of this. */
    public Expression getReal() { return real; }  

    /** Returns the imaginary part of this. */
    public Expression getImag() { return imag; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprComplex(this);
    }

    public String toString()
    {
        return "((" + real + ")+(" + imag + ")i)";
    }
    
    public boolean equals(Object other)
    {
        if (!(other instanceof ExprComplex))
            return false;
        ExprComplex that = (ExprComplex)other;
        if (!(this.real.equals(that.real)))
            return false;
        if (!(this.imag.equals(that.imag)))
            return false;
        return true;
    }
    
    public int hashCode()
    {
        return real.hashCode() ^ imag.hashCode();
    }
}
