package at.dms.kjc.spacetime;

/** 
 * 
 **/
public class Trace 
{
    private Trace[] edges;
    private TraceNode head;
    private Trace[] depends;

    public Trace (Trace[] edges, Trace[] depends, TraceNode head) 
    {
	this.edges = edges;
	this.head = head;
    }

    public Trace(TraceNode head) {
	this.head=head;
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

    public Trace[] getDepends()
    {
	return depends;
    }

    public void setEdges(Trace[] edges) {
	this.edges=edges;
    }

    public void setDepends(Trace[] depends) {
	this.depends=depends;
    }
}

