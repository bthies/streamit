package at.dms.kjc.spacetime;

import at.dms.util.Utils;

/** 
 * For file readers
 **/
public class EnterTraceNode extends OutputTraceNode {
    private String filename;

    public EnterTraceNode(String name,int[] weights,OutputTraceNode[] sources) {
	super(weights);
	filename=name;
    }

    public EnterTraceNode(String name,int[] weights) {
	super(weights);
	filename=name;
    }

    public String getFileName() {
	return filename;
    }
}
