package at.dms.kjc.flatgraph;

import at.dms.kjc.flatgraph.FlatNode;

/**
 * This class represents inter-SSG edges.
 *
 * Dynamic-rate edges between static-rate subgraphs (StaticStreamGraph)
 * 
 * @author Mike Gordon
 */

public class SSGEdge 
{
    //the source and dest SSG
    //fromSSG->toSSG
    private StaticStreamGraph fromSSG, toSSG;
    //the exact nodes of the SSGs,
    // outputNode -> inputNode
    public FlatNode outputNode, inputNode;
    /** the connection numbers, so we can rebuild the
        SSGEdges if the flatgraph changes **/
    private int from, to;

    public SSGEdge(StaticStreamGraph fromSSG, 
                   StaticStreamGraph toSSG,
                   int from, int to) 
    {
        this.fromSSG = fromSSG;
        this.toSSG = toSSG;
        this.from = from;
        this.to = to;
    }
    
    /** get the index into the input array for the downstream SSG **/
    public int getInputNum() 
    {
        return to;
    }
    
    /** get the index into the output array of the upstream SSG **/
    public int getOutputNum() 
    {
        return from;
    }

    /** the downstream SSG of edge **/
    public StaticStreamGraph getInput() 
    {
        return toSSG;
    }
    /** the upstream SSG of edge **/
    public StaticStreamGraph getOutput()
    {
        return fromSSG;
    }
}
