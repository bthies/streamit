package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph2.FilterContent;
/** 
 *
 **/
public class FilterTraceNode extends TraceNode
{
    private FilterContent filter;
    //private int initMult;
    //private int steadyMult;
    private int x, y;

    public FilterTraceNode(FilterContent filter,
			   /*int initMult, int steadyMult, */int x, int y) {
	this.filter = filter;
	//this.initMult = initMult;
	//this.steadyMult = steadyMult;
	this.x = x;
	this.y = y;
    }

    public FilterTraceNode(FilterContent filter) {
	this.filter=filter;
    }
    
    public void setX(int x) {
	this.x = x;
    }
    
    public void setY(int y) {
	this.y = y;
    }
    
    public void setXY(int x,int y) {
	this.x=x;
	this.y=y;
    }
    
    public int getX() {
	return x;
    }    
    
    public int getY() {
	return y;
    }

    public FilterContent getFilter() {
	return filter;
    }

    public int getInitMult() {
	return filter.getInitMult();
    }
    
    public int getSteadyMult() {
	return filter.getSteadyMult();
    }

    public String toString() {
	return filter.toString();
    }
}


