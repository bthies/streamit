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
    public static void dumpGraph(List steadyTrav, String fileName) 
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
			bufferArc(OffChipBuffer.getBuffer(node, node.getNext()), fw);
		    if (node.isOutputTrace())
			bufferArc(OffChipBuffer.getBuffer(node.getPrevious(), node), fw);
		    
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
		if (buffer.getSource().isInputTrace() || 
		    buffer.getDest().isOutputTrace())
		    continue;
		bufferArc(buffer, fw);
	    }
	    fw.write("}\n");
	    fw.close();
	}
	catch (Exception e) {
	    
	}
	
    }
    
    private static void bufferArc(OffChipBuffer buffer, FileWriter fw) throws Exception 
    {
	fw.write(buffer.getSource().hashCode() + " -> " + buffer.getDest().hashCode() + 
		 "[label=\"" + buffer.getDRAM());
	if (buffer.redundant())
	    fw.write("\", style=dashed");
	else
	    fw.write(buffer.getSize() + "(" + buffer.getIdent() + ")\", style=bold");
	fw.write("];\n");	
    }
    
}
