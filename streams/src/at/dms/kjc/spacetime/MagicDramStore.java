package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import at.dms.kjc.flatgraph2.*;

public class MagicDramStore extends MagicDramInstruction 
{
    private OutputTraceNode source;
    private InputTraceNode[] dests;

    public MagicDramStore(OutputTraceNode source, InputTraceNode[] dests) 
    {
	this.source = source;
	this.dests = dests;
    }

    public String toC() 
    {
	StringBuffer sb = new StringBuffer();
	//store
	sb.append("temp = threaded_static_io_receive(machine, port);\n");
	//sb.append("while (switch_to_io_move(machine, port, &temp) == 0) yield;\n");
	for (int i = 0; i < dests.length; i++) {
	    if (dests[i].isFileOutput()) {
		FileOutputContent out = 
		    (FileOutputContent)((FilterTraceNode)dests[i].getNext()).getFilter();
		if (out.isFP()) {
		    sb.append("\tfprintf(" + Util.getFileHandle(out) + 
			      ", \"%f\\n\", double(temp));\n");
		    sb.append("\tprintf(\"[%d]: %f\\n\", port, double(temp));\n");
		}
		else {
		    sb.append("\tfprintf(" + Util.getFileHandle(out) + ", \"%d\\n\", temp);\n");
		    sb.append("\tprintf(\"[%d]: %d\\n\", port, temp);\n");
		}
		
		//comment out the output limitation if getoutputs == -1
		if (out.getOutputs() == -1)
		    sb.append("/*\n");
		sb.append("\t" + Util.getOutputsVar(out) + " = " + Util.getOutputsVar(out) +
			  " + 1;\n");
		sb.append("\tif (" + Util.getOutputsVar(out) + " >= " + 
			  out.getOutputs() + ") quit_sim();\n");
		if (out.getOutputs() == -1)
		    sb.append("*/\n");
	    }
	    else {
		sb.append("\t\t" + MagicDram.getBufferIdent(source, dests[i]) +
			  "_buffer[" + MagicDram.getBufferIdent(source, dests[i]) +
			  "_st] = temp;\n");
		sb.append("\t\t" +  MagicDram.getBufferIdent(source, dests[i]) + 
			  "_st = (" +  MagicDram.getBufferIdent(source, dests[i]) + 
			  "_st + 1) % " +  MagicDram.getBufferIdent(source, dests[i]) + 
			  "_size;\n");
	    }
	}
	
	sb.append("\t\tyield;\n");
	return sb.toString();
    }
}
