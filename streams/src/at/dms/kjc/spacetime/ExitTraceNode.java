package at.dms.kjc.spacetime;

import at.dms.util.Utils;

/** 
 * For file writers
 **/
public class ExitTraceNode extends InputTraceNode {
    private String filename;

    public ExitTraceNode(String name,int[] weights,OutputTraceNode[] sources) {
	super(weights);
	filename=name;
    }

    public ExitTraceNode(String name,int[] weights) {
	super(weights);
	filename=name;
    }

    public String getFileName() {
	return filename;
    }
}
