package streamit.frontend.nodes;

/**
 * Records the input and output types of a stream.  Stream objects in
 * StreamIt have a single input and a single output; this type records
 * the types of the input and output.  This may be a void primitive type
 * if the filter has no input or output at all.
 * <p>
 * For stream types of feedback loops, this also records a third type,
 * which is the type of the output of the loop stream.  This is
 * usually the same as the type of the input of the stream, but
 * if the input type is <code>void</code>, then the loop joiner only
 * takes input from the loop stream with some non-void type.  This
 * type matters to the front-end because it determines the type of
 * values pushed on to this channel using the <code>enqueue</code>
 * statement.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StreamType.java,v 1.4 2003-09-02 15:50:09 dmaze Exp $
 */
public class StreamType extends FENode
{
    private Type in, out, loop;
    
    /**
     * Creates a new StreamType with the specified input and output
     * types.  The loop type is the same as the input type.
     *
     * @param context  file and line number for this declaration
     * @param in       input and loop type
     * @param out      output type
     */
    public StreamType(FEContext context, Type in, Type out)
    {
        super(context);
        this.in = in;
        this.out = out;
        this.loop = in;
    }

    /**
     * Creates a new StreamType with the specified input, output, and
     * loop types.
     *
     * @param context  file and line number for this declaration
     * @param in       input type
     * @param out      output type
     * @param loop     loop type
     */
    public StreamType(FEContext context, Type in, Type out, Type loop)
    {
        super(context);
        this.in = in;
        this.out = out;
        this.loop = loop;
    }

    /**
     * Returns the input type of the stream.
     *
     * @return the input type of the stream
     */
    public Type getIn()
    {
        return in;
    }
    
    /**
     * Returns the output type of the stream.
     *
     * @return the output type of the stream
     */
    public Type getOut()
    {
        return out;
    }

    /**
     * Returns the loop type of the stream.  For a feedback loop, this is
     * the type of the output of the loop child stream.
     *
     * @return the loop type of the stream
     */
    public Type getLoop()
    {
        return loop;
    }

    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStreamType(this);
    }
}
