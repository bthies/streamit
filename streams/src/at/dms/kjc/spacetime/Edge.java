package at.dms.kjc.spacetime;

public class Edge {
    private OutputTraceNode src;
    private InputTraceNode dest;

    public Edge(OutputTraceNode src,InputTraceNode dest) {
	assert src!=null:"Source Null!";
	assert dest!=null:"Dest Null!";
	this.src=src;
	this.dest=dest;
    }

    public Edge(OutputTraceNode src) {
	this.src=src;
    }

    public OutputTraceNode getSrc() {
	return src;
    }

    public InputTraceNode getDest() {
	return dest;
    }

    public void setDest(InputTraceNode dest) {
	this.dest=dest;
    }

    public String toString() 
    {
	return src.toString() + "->" + dest.toString();
    }
}

