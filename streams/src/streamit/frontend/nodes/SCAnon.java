/*
 * SCAnon.java: stream creator for anonymous streams
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: SCAnon.java,v 1.2 2002-09-06 16:28:43 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * SCAnon is a stream creator for anonymous streams.  It has a StreamSpec
 * object which specifies which stream is being created.
 */
public class SCAnon extends StreamCreator
{
    private StreamSpec spec;
    
    /** Creates a new anonymous stream given its specification. */
    public SCAnon(FEContext context, StreamSpec spec)
    {
        super(context);
        this.spec = spec;
    }
    
    /** Creates a new anonymous stream given the type of stream
     * and its init function. */
    public SCAnon(FEContext context, int type, Statement init)
    {
        super(context);
        this.spec = new StreamSpec(context, type, null, null, init);
    }
    
    /** Returns the stream specification this creates. */
    public StreamSpec getSpec()
    {
        return spec;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitSCAnon(this);
    }
}
