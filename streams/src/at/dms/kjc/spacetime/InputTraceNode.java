package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.HashSet;
import at.dms.kjc.flatgraph2.*;

/** 
 *
 **/
public class InputTraceNode extends TraceNode
{
    private int[] weights;
    private Edge[] sources;
    private static int unique = 0;
    private String ident;
    private static int[] EMPTY_WEIGHTS=new int[0];
    private static Edge[] EMPTY_SRCS=new Edge[0];

    public InputTraceNode(int[] weights,
			  Edge[] sources) {
	//this.parent = parent;
	if (weights.length != sources.length)
	    Utils.fail("Add comment later");
	this.sources = sources;
	this.weights=weights;
	ident = "input" + unique;
	unique++;
    }

    public InputTraceNode(int[] weights,
			  OutputTraceNode[] sources) {
	//this.parent = parent;
	if (weights.length != sources.length)
	    Utils.fail("Add comment later");
	//this.sources = sources;
	this.sources=new Edge[sources.length];
	for(int i=0;i<sources.length;i++)
	    this.sources[i]=new Edge(sources[i],this);
	this.weights=weights;
	ident = "input" + unique;
	unique++;
    }

    public InputTraceNode(int[] weights) {
	//this.parent = parent;
	sources=EMPTY_SRCS;
	this.weights=weights;
	ident = "input" + unique;
	unique++;
    }

    public InputTraceNode() {
	//this.parent = parent;
	sources=EMPTY_SRCS;
	weights=EMPTY_WEIGHTS;
	ident = "input" + unique;
	unique++;
    }

    public boolean isFileOutput() 
    {
	return ((FilterTraceNode)getNext()).isFileOutput();
    }

    public String getIdent() 
    {
	return ident;
    }
    
    public int[] getWeights() {
	return weights;
    }

    public Edge[] getSources() {
	return sources;
    }

    public void setSources(Edge[] sources) {
	this.sources=sources;
    }

    public int totalWeights() 
    {
	int sum = 0;
	for (int i = 0; i < weights.length; i++) 
	    sum += weights[i];
	return sum;
    }

    public int getWeight(Edge out) 
    {
	int sum = 0;

	for (int i = 0; i < sources.length; i++)
	    if (sources[i] == out)
		sum += weights[i];
	
	return sum;
    }
    
    public boolean oneInput() 
    {
	return (sources.length == 1);
    }

    public Edge getSingleEdge() 
    {
	assert oneInput() : 
	    "Calling getSingeEdge() on InputTrace with less/more than one input";
	return sources[0];
    }
    
    public FilterTraceNode getNextFilter() 
    {
	return (FilterTraceNode)getNext();
    }
    
    public boolean noInputs() 
    {
	return sources.length == 0;
    }
    
    public double ratio(Edge edge) 
    {
	if (totalWeights() == 0)
	    return 0.0;
	return ((double)getWeight(edge)/(double)totalWeights());
    }
    
    public HashSet getSourceSet() 
    {
	HashSet set = new HashSet();
	for (int i = 0; i < sources.length; i++)
	    set.add(sources[i]);
	return set;
    }

    public String debugString() 
    {
	StringBuffer buf = new StringBuffer();
	buf.append("***** " + this.toString() + " *****\n");
	for (int i = 0; i < sources.length; i++) {
	    buf.append("  weight " + weights[i] + ": " + sources[i].toString() + "\n");
	}
	buf.append("**********\n");
	return buf.toString();
    }
    
    public boolean isFileWriter() 
    {
	return getNextFilter().getFilter() instanceof FileOutputContent;
    }

    public boolean hasFileInput() 
    {
	for (int i = 0; i < sources.length; i++) {
	    if (sources[i].getSrc().isFileReader())
		return true;
	}
	return false;
    }
}
