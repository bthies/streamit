package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;

/** 
 *
 **/
public class FilterTraceNode extends TraceNode
{
    private SIRFilter filter;
    private int multiplicity;
    private int x, y;

    public FilterTraceNode(SIRFilter filter,
			   int mult, int x, int y,
			   Trace parent) {
	super(parent);
	this.filter = filter;
	multiplicity = mult;
	this.x = x;
	this.y = y;
    }
    
    public void setX(int x) {
	this.x = x;
    }
    
    public void setY(int y) {
	this.y = y;
    }
    
    public int getX() {
	return x;
    }    
    
    public int getY() {
	return y;
    }

    public SIRFilter getFilter() {
	return filter;
    }

    public int getMultiplicity() {
	return multiplicity;
    }
}
