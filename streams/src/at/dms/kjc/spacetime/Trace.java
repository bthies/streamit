package at.dms.kjc.spacetime;

/** 
 * 
 **/
public class Trace 
{
    private Trace[] edges;
    private TraceNode head;

    public Trace (Trace[] edges, TraceNode head) 
    {
	this.edges = edges;
	this.head = head;
    }

    public void setHead(TraceNode node) 
    {
	head = node;
    }
    
    public TraceNode getHead() 
    {
	return head;
    }
    
    public Trace[] getEdges() 
    {
	return edges;
    }
}
