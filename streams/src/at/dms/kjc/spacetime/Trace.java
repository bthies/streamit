package at.dms.kjc.spacetime;

/** 
 * 
 **/
public class Trace 
{
    private Trace[] edges;
    private InputTraceNode head;
    private OutputTraceNode tail;
    private int len;
    private Trace[] depends;

    public Trace (Trace[] edges, Trace[] depends, InputTraceNode head) 
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
	len=-1;
    }

    public Trace(InputTraceNode head) {
	this.head = head;
	depends = new Trace[0];
	edges = new Trace[0];
	len=-1;
    }

    public Trace(TraceNode node) {
	head = new InputTraceNode();
	head.setNext(node);
	node.setPrevious(head);
	depends = new Trace[0];
	edges = new Trace[0];
	len=-1;
    }

    //Finishes creating Trace
    public int finish() {
	int size=0;
	TraceNode node=head;
	if(node instanceof InputTraceNode)
	    node=node.getNext();
	TraceNode end=node;
	while(node!=null&&node instanceof FilterTraceNode) {
	    size++;
	    end=node;
	    node=node.getNext();
	}
	if(node!=null)
	    end=node;
	len=size;
	tail=(OutputTraceNode)end;
	return size;
    }
    
    //finish() must have been called
    public int size() {
	return len;
    }

    public void setHead(InputTraceNode node) 
    {
	head = node;
    }
    
    public TraceNode getHead() 
    {
	return head;
    }

    //finish() must have been called
    public OutputTraceNode getTail() {
	return tail;
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

    public void connect(Trace target) {
	edges=new Trace[]{target};
	target.depends=new Trace[]{this};
    }
}




