/*
 * Program.java: an entire StreamIt program
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: Program.java,v 1.2 2002-09-06 16:28:43 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.List;

/**
 * A Program is a StreamIt program, including all of its declared streams
 * and structure types.  It consequently has Lists of streams (as StreamSpec
 * objects) and of structures (as TypeStruct objects).
 */
public class Program extends FENode
{
    private List streams, structs;
    
    /** Creates a new StreamIt program, given lists of streams and
     * structures. */
    public Program(FEContext context, List streams, List structs)
    {
        super(context);
        this.streams = streams;
        this.structs = structs;
    }
    
    /** Returns the list of streams declared in this. */
    public List getStreams()
    {
        return streams;
    }
    
    /** Returns the list of structures declared in this. */
    public List getStructs()
    {
        return structs;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitProgram(this);
    }
}

