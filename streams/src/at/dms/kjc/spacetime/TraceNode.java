package at.dms.kjc.spacetime;

/** 
 *
 **/
abstract public class TraceNode  
{
    private TraceNode next;
    
    public TraceNode getNext() {
	return next;
    }
    
    public void setNext(TraceNode next) {
	this.next = next;
    }
}
