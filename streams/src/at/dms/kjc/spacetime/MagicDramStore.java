package at.dms.kjc.spacetime;

import at.dms.util.Utils;

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
	    sb.append("\t\t" + MagicDram.getBufferIdent(source, dests[i]) +
		      "_buffer[" + MagicDram.getBufferIdent(source, dests[i]) +
		      "_st] = temp;\n");
	    sb.append("\t\t" +  MagicDram.getBufferIdent(source, dests[i]) + 
		      "_st = (" +  MagicDram.getBufferIdent(source, dests[i]) + 
		      "_st + 1) % " +  MagicDram.getBufferIdent(source, dests[i]) + 
		      "_size;\n");
	}
	
	sb.append("\t\tyield;\n");
	return sb.toString();
    }
}
