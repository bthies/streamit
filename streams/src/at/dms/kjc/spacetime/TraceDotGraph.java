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
	    FileWriter fw = new FileWriter(fileName);
	    fw.write("digraph TraceDotGraph {\n");
	    fw.write("size = \"8, 10.5\";\n");
	    Iterator it = Util.traceNodeTraversal(steadyTrav);
	    while (it.hasNext()) {
		TraceNode node = (TraceNode)it.next();
		fw.write(node.hashCode() + "[ label=\"" + node.toString());
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
	    }
	    Iterator buffers = OffChipBuffer.getBuffers().iterator();
	    while (buffers.hasNext()) {
		OffChipBuffer buffer = (OffChipBuffer)buffers.next();
		fw.write(buffer.getSource().hashCode() + " -> " + buffer.getDest().hashCode() + 
			 "[label=\"" + buffer.getDRAM());
		if (buffer.redundant())
		    fw.write("\", style=dashed");
		else
		    fw.write(buffer.getSize() + "(" + buffer.getIdent() + ")\", style=bold");
		fw.write("];\n");
	    }
	    fw.write("}\n");
	    fw.close();
	}
	catch (Exception e) {
	    
	}
	
    }
    
}
