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
	if (edges == null)
	    this.edges = new Trace[0];
	else 
	    this.edges = edges;

	this.head = head;

	if (depends == null)
	    this.depends = new Trace[0];
	else 
	    this.depends = depends;
    }

    public Trace(TraceNode head) {
	this.head = head;
	depends = new Trace[0];
	edges = new Trace[0];
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
	if (edges != null)
	    this.edges = edges;
    }

    public void setDepends(Trace[] depends) {
	if (depends != null) 
	    this.depends = depends;
    }
}

