package at.dms.kjc.spacetime;

/** 
 *
 **/
abstract public class TraceNode  
{
    private TraceNode next;
    private TraceNode previous;

    public TraceNode getNext() {
	return next;
    }
    
    public TraceNode getPrevious() {
	return previous;
    }

    public void setPrevious(TraceNode prev) {
	previous = prev;
    }

    public void setNext(TraceNode next) {
	this.next = next;
    }
    
    public boolean isInputTrace() 
    {
	return this instanceof InputTraceNode;
    }
    
    public boolean isFilterTrace() 
    {
	return this instanceof FilterTraceNode;
    }
    
    public boolean isOutputTrace() 
    {
	return this instanceof OutputTraceNode;
    }
    
}
