package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import at.dms.kjc.flatgraph2.*;

public class MagicDramLoad extends MagicDramInstruction 
{
    private OutputTraceNode source;
    private InputTraceNode dest;
    
    public MagicDramLoad(InputTraceNode dest, OutputTraceNode source) 
    {
	this.dest = dest;
	this.source = source;
    }
    
    public String toC() 
    {
	StringBuffer sb = new StringBuffer();
	if (source.isFileInput()) {
	    FileInputContent in = (FileInputContent)((FilterTraceNode)source.getPrevious()).getFilter();
	    sb.append("\t\tfscanf(" + Util.getFileHandle(in) + ", \"%e\\n\", &temp);\n");
	    sb.append("while (io_to_switch_move(machine, port, temp) == 0) yield;\n");
	}
	else {
	    sb.append("while (io_to_switch_move(machine, port, " + MagicDram.getBufferIdent(source, dest) +
		      "_buffer[" + MagicDram.getBufferIdent(source, dest) + "_ld]) == 0) yield;\n");
	    sb.append("\t\t" + MagicDram.getBufferIdent(source, dest) + "_ld = (" + 
		      MagicDram.getBufferIdent(source, dest) + "_ld + 1) % " + 
		      MagicDram.getBufferIdent(source, dest) + "_size;\n");
	    sb.append("\t\tyield;\n");
	}
	return sb.toString();
    }
    
	
}
