package at.dms.kjc.spacetime;

import at.dms.util.Utils;

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
	head.setParent(this);

	if (depends == null)
	    this.depends = new Trace[0];
	else 
	    this.depends = depends;
	len=-1;
    }

    public Trace(InputTraceNode head) {
	this.head = head;
	head.setParent(this);
	depends = new Trace[0];
	edges = new Trace[0];
	len=-1;
    }

    public Trace(TraceNode node) {
	if(!(node instanceof FilterTraceNode))
	    Utils.fail("FilterTraceNode expected: "+node);
	head = new InputTraceNode();
	head.setParent(this);
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
	if(end instanceof OutputTraceNode)
	    tail=(OutputTraceNode)end;
	else {
	    tail=new OutputTraceNode();
	    end.setNext(tail);
	    tail.setPrevious(end);
	}
	tail.setParent(this);
	return size;
    }
    
    //finish() must have been called
    public int size() {
	assert len>-1:"finish() was not called";
	return len;
    }

    public void setHead(InputTraceNode node) 
    {
	head = node;
	node.setParent(this);
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

    public String toString() {
	return "Trace:"+head.getNext();
    }
}




