package at.dms.kjc.spacetime;

import at.dms.kjc.flatgraph2.FilterContent;

public class PredefinedFilterTraceNode extends FilterTraceNode {
    public PredefinedFilterTraceNode(FilterContent filter,int x,int y) {
	super(filter,x,y);
    }

    public PredefinedFilterTraceNode(FilterContent filter) {
	super(filter);
    }
}
