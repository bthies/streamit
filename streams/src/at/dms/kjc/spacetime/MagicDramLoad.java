package at.dms.kjc.spacetime;

import at.dms.util.Utils;

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
	sb.append("while (io_to_switch_move(machine, port, " + MagicDram.getBufferIdent(source, dest) +
		      "_buffer[" + MagicDram.getBufferIdent(source, dest) + "_ld]) == 0) yield;\n");
	sb.append("\t\t" + MagicDram.getBufferIdent(source, dest) + "_ld++;\n");
	sb.append("\t\tyield;\n");
	return sb.toString();
    }
    
	
}
