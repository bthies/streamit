package at.dms.kjc.spacetime;

import at.dms.util.Utils;

/** 
 * For file writers
 **/
public class ExitTraceNode extends InputTraceNode {
    private String filename;
    private boolean fp;
    private int outputs;
    
    public ExitTraceNode(String name, boolean fp, int[] weights,OutputTraceNode[] sources) {
	super(weights, sources);
	this.fp = fp;
	filename=name;
	outputs = -1;
    }

    public ExitTraceNode(String name, boolean fp, int[] weights) {
	super(weights);
	this.fp = fp;
	filename=name;
    }

    public int getOutputs() 
    {
	return outputs;
    }
    
    public boolean isFloatingPoint() 
    {
	return fp;
    }

    public String getFileName() {
	return filename;
    }
}
