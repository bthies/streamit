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
	sb.append("while (switch_to_io_move(machine, port, &temp) == 0) yield;\n");
	for (int i = 0; i < dests.length; i++) {
	    if (dests[i].isFileOutput()) {
		FileOutputContent out = (FileOutputContent)((FilterTraceNode)dests[i].getNext()).getFilter();
		sb.append("\tfprintf(" + Util.getFileHandle(out) + ", \"%e\\n\", temp);\n");
		sb.append("\tprintf(\"[%d]: %e\\n\", port, temp);\n");
		sb.append("if (" + Util.getOutputsVar(out) + 
			 " != -1 && ++" + Util.getOutputsVar(out) + " >= " + 
			 out.getOutputs() + ") quit_sim();\n");
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
