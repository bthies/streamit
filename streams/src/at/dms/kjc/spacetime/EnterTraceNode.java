package at.dms.kjc.spacetime;

import at.dms.util.Utils;

/** 
 * For file readers
 **/
public class EnterTraceNode extends OutputTraceNode {
    private String filename;
    private boolean fp;
    private int samples;
    
    public EnterTraceNode(String name, boolean fp, int sample, int[] weights, InputTraceNode[][] dests) {
	super(weights, dests);
	if (weights.length > 1) 
	    Utils.fail("Multiple destinations for a single file not supported");
	filename=name;
	this.samples = samples;
	this.fp = fp;
    }

    public EnterTraceNode(String name, boolean fp, int[] weights) {
	super(weights);
	this.fp = fp;
	filename=name;
    }

    public boolean isFloatingPoint() 
    {
	return fp;
    }
    
    public int getSamples() 
    {
	return samples;
    }
    
    public String getIdent() 
    {
	return filename;
    }

    public String getFileHandle() 
    {
	return "file_" + getFileName();
    }

    public String getFileName() {
	return filename;
    }
}
