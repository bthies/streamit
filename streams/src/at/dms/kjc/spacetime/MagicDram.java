package at.dms.kjc.spacetime;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.io.FileWriter;

public class MagicDram extends IODevice
{
    //the list of instructions
    public LinkedList steadyInsList;
    public LinkedList initInsList;

    //the names of the buffer indices
    private HashSet indices;
    //the names and sizes of the buffers
    private HashMap buffers;

    public MagicDram(RawChip chip, int port, RawTile tile) 
    {
	super(chip, port, tile);
	tile.setIODevice(this);
	steadyInsList = new LinkedList();
	initInsList = new LinkedList();
	indices = new HashSet();
	buffers = new HashMap();
    }
    
    public static void GenerateCode(RawChip chip)
    {
	for (int i = 0; i < chip.getDevices().length; i++) {
	    if (chip.getDevices()[i] == null) 
		continue;
	    ((MagicDram)chip.getDevices()[i]).bcCode();
	}
    }
    
    
    private void bcCode() 
    {
	try {
	    FileWriter fw = new FileWriter("magicdram" + port + ".bc");
	    
	    fw.write("fn dev_magic_dram" + port + "_init(ioPort)\n");
	    fw.write("{\n");
	    fw.write("local result;\n");
	    fw.write("local port = ioPort;\n");
	    fw.write("result = SimAddDevice(\"Magic DRAM" + port + "\",\n");
	    fw.write("			\"dev_magic_dram" + port + "_reset\",\n");
	    fw.write("			\"dev_magic_dram" + port + "_calc\",\n");
	    fw.write("			port);\n");
	    fw.write("if (result == 0) {\n");
	    fw.write("\tprintf(\"// **** magic_dram: error adding device to port %d\\n\", port);\n");
	    fw.write("\treturn 0;\n");
	    fw.write("}\n");
	    fw.write("\treturn 1;\n");
	    fw.write("}\n\n");
	    fw.write("fn dev_magic_dram" + port +"_reset(port) \n");
	    fw.write("{\n");
	    fw.write("}\n");

	    fw.write("fn dev_magic_dram" + port + "_calc(port)\n");
	    fw.write("{\n");
	    fw.write("\tlocal temp = 0;\n");
	    
	    Iterator inds = indices.iterator();
	    while (inds.hasNext()) {
		String current = (String)inds.next();
		fw.write("\tlocal " + current + "_ld = 0;\n");
		fw.write("\tlocal " + current + "_st = 0;\n");
	    }
	    
	    Iterator bufs = buffers.keySet().iterator();
	    while (bufs.hasNext()) {
		String current = (String)bufs.next();
		fw.write("\tlocal " + current + " = malloc(" + buffers.get(current) + ");\n");
	    }
	    
	    //write the init magic dram instructions
	    Iterator it = initInsList.iterator();
	    while (it.hasNext())
		fw.write("\t" + ((MagicDramInstruction)it.next()).toC());
	    //write the steady magic dram instructions with a while loop around them
	    fw.write("\twhile(1) {\n");
	    //reset the indices..
	    inds = indices.iterator();
	    while (inds.hasNext()) {
		String current = (String)inds.next();
		fw.write("\t\t" + current + "_ld = 0;\n");
		fw.write("\t\t" + current + "_st = 0;\n");
	    }
	    it = steadyInsList.iterator();
	    if (!it.hasNext())
		fw.write("\t\tyield;\n");
	    while (it.hasNext()) 
		fw.write("\t\t" + ((MagicDramInstruction)it.next()).toC());
	    fw.write("\t}\n");
	    fw.write("}\n");
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Error generating bc code for magic dram");
	    e.printStackTrace();
	}
    }
    
    public static void testMagicDramCode(RawChip chip)
    {
	for (int i = 0; i < chip.getDevices().length; i++) {
	    if (chip.getDevices()[i] == null) 
		continue;
	    System.out.println("  ----- " + chip.getDevices()[i].getPort() + " --------- ");
	    Iterator bufs = ((MagicDram)chip.getDevices()[i]).buffers.keySet().iterator();
	    while(bufs.hasNext()) {
		String buf = (String)bufs.next();
		int size = ((Integer)((MagicDram)chip.getDevices()[i]).buffers.get(buf)).intValue();
		System.out.println(buf + " " + size);
	    }
	    System.out.println("init:");
	    Iterator it = ((MagicDram)chip.getDevices()[i]).initInsList.iterator();
	    while (it.hasNext()) 
		System.out.println("\t" + ((MagicDramInstruction)it.next()).toC());
	    System.out.println("steady:");
	    it = ((MagicDram)chip.getDevices()[i]).steadyInsList.iterator();
	    while (it.hasNext()) 
		System.out.println("\t" + ((MagicDramInstruction)it.next()).toC());
	}
    }

    
    public void addBuffer(OutputTraceNode out, InputTraceNode in, int size) 
    {
	//add the index
	if (!indices.contains(getBufferIdent(out, in)))
	    indices.add(new String(getBufferIdent(out, in)));
	
	//add the buffer, 
	if (!buffers.containsKey(getBufferIdent(out, in) + "_buffer"))
	    buffers.put(new String(getBufferIdent(out, in) + "_buffer"), new Integer(size));
	else {
	    //if already present see if the new size for this stage is larger than the previous
	    //stage size
	    int oldVal = ((Integer)buffers.get(getBufferIdent(out, in) + "_buffer")).intValue();
	    if (size > oldVal)
		buffers.put(new String(getBufferIdent(out, in) + "_buffer"), new Integer(size));
	}
		
    }

    public static String getBufferIdent(OutputTraceNode out, InputTraceNode in) 
    {
	return out.getIdent() + "_" + in.getIdent();
    }
    
}
