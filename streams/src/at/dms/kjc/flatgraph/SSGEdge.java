package at.dms.kjc.flatgraph;

import at.dms.kjc.flatgraph.FlatNode;

/**
 * This class represents inter-SSG edges.
 *
 * Dynamic-rate edges between static-rate subgraphs (StaticStreamGraph).
 * 
 * Note that a SSGEdge may be shared between the SSGs at its upstream and downstream ends,
 * or they may have separate SSGEdge's with identical data.
 * 
 * @author Mike Gordon
 */

public class SSGEdge<fromType extends StaticStreamGraph, toType extends StaticStreamGraph> 
{
    //the source and dest SSG
    //fromSSG->toSSG
    private fromType fromSSG;
    private toType toSSG;
    //the exact nodes of the SSGs,
    // outputNode -> inputNode
    FlatNode upstreamNode, downstreamNode;
    /** the connection numbers, so we can rebuild the
        SSGEdges if the flatgraph changes **/
    private int from, to;

    public SSGEdge(fromType fromSSG, 
                   toType toSSG,
                   int from, int to) 
    {
        this.fromSSG = fromSSG;
        this.toSSG = toSSG;
        this.from = from;
        this.to = to;
    }
    
    /** get the index into the input array for the downstream SSG **/
    public int getDownstreamNum() 
    {
        return to;
    }
    
    /** get the index into the output array of the upstream SSG **/
    public int getUpstreamNum() 
    {
        return from;
    }

    /** the downstream SSG of edge **/
    public toType getDownstream() 
    {
        return toSSG;
    }
    /** the upstream SSG of edge **/
    public fromType getUpstream()
    {
        return fromSSG;
    }
}
