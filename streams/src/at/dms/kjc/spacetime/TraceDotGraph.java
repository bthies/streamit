package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.flatgraph2.*;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;


public class TraceDotGraph 
{
    public static void dumpGraph(List steadyTrav, String fileName, boolean DRAM) 
    {
	try {
	    boolean first = true;
	    int order = 1;
	    FileWriter fw = new FileWriter(fileName);
	    fw.write("digraph TraceDotGraph {\n");
	    fw.write("size = \"8, 10.5\";\n");
	    Iterator traces = steadyTrav.iterator();
	    while (traces.hasNext()) {
		Trace trace = (Trace)traces.next();
		TraceNode node = trace.getHead();
		fw.write("subgraph cluster" + trace.hashCode() + " {\n");
		fw.write("  color=blue;\n");
		fw.write("  label = \"" + order++ + "\";\n");
		while (node != null) {
		    if (node.isFilterTrace() && !node.getNext().isOutputTrace())
			fw.write("  " + node.hashCode() + " -> " + node.getNext().hashCode() + ";\n");
		    if (node.isInputTrace()) 
			bufferArc(IntraTraceBuffer.getBuffer((InputTraceNode)node, (FilterTraceNode)node.getNext()), fw, DRAM);
		    if (node.isOutputTrace())
			bufferArc(IntraTraceBuffer.getBuffer((FilterTraceNode)node.getPrevious(), (OutputTraceNode)node), fw, DRAM);
		    
		    fw.write("  " + node.hashCode() + "[ label=\"" + node.toString());
		    if (node.isFilterTrace()) {
			FilterInfo filter = FilterInfo.getFilterInfo((FilterTraceNode)node);
			fw.write("\\nMult: (" + filter.initMult + ", " + filter.primePump + ", " + filter.steadyMult + ")");
			fw.write("\\nPre-peek, pop, push: (" + filter.prePeek + ", " + filter.prePop + ", " + filter.prePush + ")");
			fw.write("\\npeek, pop, push: (" + filter.peek + ", " + filter.pop + ", " + filter.push + ")");
		    }
		    fw.write("\"");
		    if (first) {
			fw.write(", shape=triangle");
			first = false;
		    }
		    fw.write("];\n");
		    node = node.getNext();
		}
		fw.write("}\n");
	    }
	    
	    Iterator buffers = OffChipBuffer.getBuffers().iterator();
	    while (buffers.hasNext()) {
		OffChipBuffer buffer = (OffChipBuffer)buffers.next();
		if (buffer.isIntraTrace())
		    continue;
		bufferArc(buffer, fw, DRAM);
	    }
	    fw.write("}\n");
	    fw.close();
	}
	catch (Exception e) {
	    
	}
	
    }
    
    private static void bufferArc(OffChipBuffer buffer, FileWriter fw, boolean DRAM) throws Exception 
    {
	fw.write(buffer.getSource().hashCode() + " -> " + buffer.getDest().hashCode() + 
		 "[label=\"" + (DRAM ? buffer.getDRAM().toString(): "not assigned\""));
	if (DRAM) {
	    if (buffer.redundant())
		fw.write("\", style=dashed");
	    else
		fw.write(buffer.getSize(true) + ", " + buffer.getSize(false) + 
			 "(" + buffer.getIdentPrefix() + ")\", style=bold");
	}
	fw.write("];\n");	
    }
    
}
