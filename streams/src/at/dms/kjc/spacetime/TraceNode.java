package at.dms.kjc.spacetime;

/** 
 *
 **/
abstract public class TraceNode  
{
    private TraceNode next;
    private Trace parent;
    
    public TraceNode(Trace parent) {
	this.parent = parent;
    }

    public TraceNode getNext() {
	return next;
    }
    
    public void setNext(TraceNode next) {
	this.next = next;
    }
}
