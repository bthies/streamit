package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.flatgraph2.*;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;


public class TraceDotGraph 
{
    public static void dumpGraph(List steadyTrav, Trace[] io, String fileName, boolean DRAM, RawChip rawChip) 
    {
	SpaceTimeBackend.println("Creating Trace Dot Graph...");	
	try {
	    int order = 1;
	    FileWriter fw = new FileWriter(fileName);
	    fw.write("digraph TraceDotGraph {\n");
	    fw.write("size = \"8, 10.5\";\n");
	    LinkedList tracesList = new LinkedList(steadyTrav);
	    for (int i = 0; i < io.length; i++)
		tracesList.add(io[i]);
	    //HashSet traceSet = new HashSet();
	    //Util.addAll(traceSet, steadyTrav);
	    //add the file readers and writes if they exist
	    //for (int i = 0; i < io.length; i++)
	    //traceSet.add(io[i]);
	    
	    Iterator traces = tracesList.iterator();
	    while (traces.hasNext()) {
		Trace trace = (Trace)traces.next();
		TraceNode node = trace.getHead();
		fw.write("subgraph cluster" + trace.hashCode() + " {\n");
		fw.write("  color=blue;\n");
		fw.write("  label = \"" + order++ + "\";\n");
		while (node != null) {
		    if (node.isFilterTrace() && !node.getNext().isOutputTrace())
			fw.write("  " + node.hashCode() + " -> " + node.getNext().hashCode() + ";\n");
		    if (node.isInputTrace()) {
			bufferArc(IntraTraceBuffer.getBuffer((InputTraceNode)node, (FilterTraceNode)node.getNext()), fw, DRAM);
			fw.write("  " + node.hashCode() + "[ label=\"");
			if (((InputTraceNode)node).oneInput() ||
			    ((InputTraceNode)node).noInputs())
			    fw.write(node.toString());
			else {
			    fw.write(((InputTraceNode)node).debugString(true));
			}
			
		    }
		    
		    if (node.isOutputTrace()) {
			bufferArc(IntraTraceBuffer.getBuffer((FilterTraceNode)node.getPrevious(), (OutputTraceNode)node), fw, DRAM);
			fw.write("  " + node.hashCode() + "[ label=\"");
			if (((OutputTraceNode)node).oneOutput() ||
			    ((OutputTraceNode)node).noOutputs())
			    fw.write(node.toString());
			else
			    fw.write(((OutputTraceNode)node).debugString(true));
		    }
		    
		    
		    if (node.isFilterTrace()) {
			fw.write("  " + node.hashCode() + "[ label=\"" + ((FilterTraceNode)node).toString(rawChip));
			FilterInfo filter = FilterInfo.getFilterInfo((FilterTraceNode)node);
			fw.write("\\nMult: (" + filter.initMult + ", " + filter.primePump + ", " + filter.steadyMult + ")");
			fw.write("\\nPre-peek, pop, push: (" + filter.prePeek + ", " + filter.prePop + ", " + filter.prePush + ")");
			fw.write("\\npeek, pop, push: (" + filter.peek + ", " + filter.pop + ", " + filter.push + ")");
		    }
		    fw.write("\"");
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
	SpaceTimeBackend.println("Finished Creating Trace Dot Graph");	
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
