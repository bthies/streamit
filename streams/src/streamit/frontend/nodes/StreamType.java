/*
 * StreamType.java: record the I/O type of a stream
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StreamType.java,v 1.3 2002-09-06 16:28:43 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * Records the input and output types of a stream.  Stream objects in
 * StreamIt have a single input and a single output; this type records
 * the types of the input and output.  This may be a void primitive type
 * if the filter has no input or output at all.
 *
 * (Note that there is a corner case, which this type does not address,
 * involving feedback loops with void input or output types.  In this
 * case, the input type of the body matches the output type of the loop,
 * but this doesn't match a void input type.  This setup is valid if the
 * joiner is a round-robin joiner taking no input from the external
 * edge, and the input type of the feedback loop is void.  A similar
 * condition can exist around the splitter and feedback loop output.
 * In these cases it can be useful to know the loop type separately
 * from the input or output type.)
 */
public class StreamType extends FENode
{
    private Type in, out;
    
    /** Creates a new StreamType with the specified input and output
     * types. */
    public StreamType(FEContext context, Type in, Type out)
    {
        super(context);
        this.in = in;
        this.out = out;
    }

    /** Returns the input type of the stream. */
    public Type getIn()
    {
        return in;
    }
    
    /** Returns the output type of the stream. */
    public Type getOut()
    {
        return out;
    }

    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStreamType(this);
    }
}
