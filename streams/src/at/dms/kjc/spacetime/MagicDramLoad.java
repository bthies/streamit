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
	    sb.append("\tif (");
	    if (in.isFP())
		sb.append("fscanf(" + Util.getFileHandle(in) + ", \"%e\\n\", &temp)");
	    else 
		sb.append("fscanf(" + Util.getFileHandle(in) + ", \"%d\\n\", &temp)");
	    sb.append(" == -1) {\n");
	    sb.append("\t\tprintf(\"*** ERROR: Reached end of input file ***\\n\");\n");
	    sb.append("\t\tquit_sim();\n");
	    sb.append("\t}\n");
	    sb.append("threaded_static_io_send(machine, port, temp);\n");
	    sb.append("yield;\n");
	    //	    sb.append("while (io_to_switch_move(machine, port, temp) == 0) yield;\n");
	}
	else {
	    
	    //	    sb.append("while (io_to_switch_move(machine, port, " + 
	    sb.append("threaded__static_io_send(machine, port, " + 
		      MagicDram.getBufferIdent(source, dest) +
		      "_buffer[" + MagicDram.getBufferIdent(source, dest) + "_ld]);\n");	    
	    sb.append("\t\t" + MagicDram.getBufferIdent(source, dest) + "_ld = (" + 
		      MagicDram.getBufferIdent(source, dest) + "_ld + 1) % " + 
		      MagicDram.getBufferIdent(source, dest) + "_size;\n");
	    sb.append("\t\tyield;\n");
	}
	return sb.toString();
    }
    
	
}
