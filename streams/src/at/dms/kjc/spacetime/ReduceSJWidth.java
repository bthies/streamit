package at.dms.kjc.spacetime;

import java.util.List;
import java.util.Iterator;


/**
 * This class will insert identity filters to reduce the width of 
 * splitting and joining, so that it

 * It modifies the stream graph in place, adding edges and identities
 **/
public class ReduceSJWidth
{
    public static void run(List steadyList, RawChip chip, Trace[] files) 
    {
	Iterator traceNodeTrav = Util.traceNodeTraversal(steadyList);
	while (traceNodeTrav.hasNext()) {
	    TraceNode traceNode = (TraceNode)traceNodeTrav.next();
	    if (traceNode.isInputTrace() && 
		((InputTraceNode)traceNode).getSourceSet().size() > chip.getNumDev()) {
		//reduce the Joiner widths

	    }
	    else if (traceNode.isOutputTrace() &&
		     ((OutputTraceNode)traceNode).getDestSet().size() > chip.getNumDev()) {
		//now reduce the splitter width
	    }
	    
	}
    }
    
}
