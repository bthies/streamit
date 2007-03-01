package at.dms.kjc.flatgraph;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.Map;
import java.util.WeakHashMap;
/**
 * This class represents inter-SSG edges.
 *
 * Dynamic-rate edges between static-rate subgraphs (StaticStreamGraph).
 * 
 * A there is only a single SSGEdge for a connection, shared by its upstream side and its downstream side.
 * So, if upstream (resp. downstream) side sets upstreamNode (resp. downStreamNode) value, 
 * the downstream (resp. upstream) SSG will see it.
 * 
 * @author Mike Gordon
 */

public class SSGEdge 
{
    static Map<SSGEdge,Object> allEdges = new WeakHashMap<SSGEdge,Object>();

    /**
     * Create a new SSGEdge, or return an existing one with the same parameters.
     * @param fromSSG  upstream SSG
     * @param toSSG    dowmstream SSG
     * @param from     number of outgoing connection in upstream SSG
     * @param to       number of incoming connection in downstream SSG
     * @return a SSGEdge.
     */
    public static SSGEdge createSSGEdge(StaticStreamGraph fromSSG, StaticStreamGraph toSSG, int from, int to) {
        for (SSGEdge e : allEdges.keySet()) {
            if (e.fromSSG == fromSSG && e.toSSG == toSSG && e.from == from && e.to == to) {
                return e;
            }
        }
        SSGEdge retval = new SSGEdge(fromSSG, toSSG, from, to);
        allEdges.put(retval, null);
        return retval;
    }

    //the source and dest SSG
    //fromSSG->toSSG
    private StaticStreamGraph fromSSG;
    private StaticStreamGraph toSSG;
    //the exact nodes of the SSGs,
    // outputNode -> inputNode
    private FlatNode downstreamNode;
    private FlatNode upstreamNode;
    /** the connection numbers, so we can rebuild the
        SSGEdges if the flatgraph changes **/
    private int from, to;

    private SSGEdge(StaticStreamGraph fromSSG, 
                   StaticStreamGraph toSSG,
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
    public StaticStreamGraph getDownstream() 
    {
        return toSSG;
    }
    
    /** the upstream SSG of edge **/
    public StaticStreamGraph getUpstream()
    {
        return fromSSG;
    }

    /**
     * @param upstreamNode the upstreamNode to set
     */
    void setUpstreamNode(FlatNode upstreamNode) {
        this.upstreamNode = upstreamNode;
    }

    /**
     * @return the upstreamNode
     */
    FlatNode getUpstreamNode() {
        return upstreamNode;
    }

    /**
     * @param downstreamNode the downstreamNode to set
     */
    void setDownstreamNode(FlatNode downstreamNode) {
        this.downstreamNode = downstreamNode;
    }

    /**
     * @return the downstreamNode
     */
    FlatNode getDownstreamNode() {
        return downstreamNode;
    }
}
