package streamit.frontend.nodes;

/**
 * A string literal.  The only place these currently appear in StreamIt
 * is as the filename argument to file reader and writer filters.
 * For convenience, these are stored in their original program-source
 * representation.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ExprConstStr.java,v 1.3 2003-08-12 13:47:15 dmaze Exp $
 */
public class ExprConstStr extends Expression
{
    private String val;
    
    /**
     * Create a new ExprConstStr.
     *
     * @param context  file and line number in the source file
     * @param val      source-format representation of the string
     */
    public ExprConstStr(FEContext context, String val)
    {
        super(context);
        this.val = val;
    }
    
    /**
     * Returns the value of this.  The returned string will be in
     * the format it appeared in in the original source file,
     * including leading and trailing double quotes.
     *
     * @return  source-format representation of the string
     */
    public String getVal() { return val; }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprConstStr(this);
    }
}

