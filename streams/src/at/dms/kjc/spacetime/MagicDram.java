package at.dms.kjc.spacetime;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.io.FileWriter;
import java.util.ArrayList;
import at.dms.kjc.flatgraph2.*;

public class MagicDram extends IODevice
{
    //the list of instructions
    public LinkedList steadyInsList;
    public LinkedList initInsList;
    
    //the FilterInputTraces of input files in the dram
    public HashSet inputFiles;
    //the  FilterOutputTraces of files in this dram
    public HashSet outputFiles;
    
    //the names of the buffer indices
    private HashSet indices;
    //the names and sizes of the buffers
    private ArrayList buffers;

    public MagicDram(RawChip chip, int port, RawTile tile) 
    {
	super(chip, port, tile);
	tile.setIODevice(this);
	steadyInsList = new LinkedList();
	initInsList = new LinkedList();
	indices = new HashSet();
	buffers = new ArrayList();
	inputFiles = new HashSet();
	outputFiles = new HashSet();
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
	    
	    //write the global file handlers
	    Iterator outfiles = outputFiles.iterator();
	    while (outfiles.hasNext()) {
		FileOutputContent out = (FileOutputContent)outfiles.next();
		fw.write("global " + Util.getFileHandle(out) + ";\n");
		fw.write("global " + Util.getOutputsVar(out) + " = 0;\n");
	    }
	    
	    Iterator infiles = inputFiles.iterator();
	    while (infiles.hasNext()) {
		FileInputContent in = (FileInputContent)infiles.next();
		fw.write("global " + Util.getFileHandle(in) + ";\n");
	    }
	    
	    fw.write("fn dev_magic_dram" + port + "_init(ioPort)\n");
	    fw.write("{\n");
	    fw.write("local result;\n");
	    fw.write("local port = ioPort;\n");
	    //open the input and output file(s)
	    outfiles = outputFiles.iterator();
	    while (outfiles.hasNext()) {
		FileOutputContent out = (FileOutputContent)outfiles.next();
		fw.write(Util.getFileHandle(out) + " = fopen(\"" + out.getFileName() +
			 "\", \"w\");\n");
		fw.write("if (" + Util.getFileHandle(out) + " == 0) {\n");
		fw.write("\tprintf(\"Error opening file " + out.getFileName() + "\");\n");
		fw.write("\treturn 0;\n");
		fw.write("}\n");
	    }
	    
	    infiles = inputFiles.iterator();
	    while (infiles.hasNext()) {
		FileInputContent in = (FileInputContent)infiles.next();
		fw.write(Util.getFileHandle(in) + " = fopen(\"" + in.getFileName() +
			 "\", \"r\");\n");
		fw.write("if (" + Util.getFileHandle(in) + " == 0) {\n");
		fw.write("\tprintf(\"Error reading from file " + in.getFileName() + "\");\n");
		fw.write("\treturn 0;\n");
		fw.write("}\n");
	    }

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
	    
	    Iterator bufs = buffers.iterator();
	    while (bufs.hasNext()) {
		Buffer current = (Buffer)bufs.next();
		String bufferIdent = getBufferIdent(current.out, current.in);
		fw.write("\tlocal " + bufferIdent + "_size = " + 
			 Util.magicBufferSize(current.in, current.out) + ";\n");
		fw.write("\tlocal " + bufferIdent + "_buffer = malloc(" + bufferIdent + "_size);\n");
	    }
	    //Before we can start this device, receive one item from the tile
	    //so that we know that it is done booting...
	    fw.write("// wait for boot to finish before starting...\n");
	    fw.write("threaded_static_io_receive(machine, port);\n");
	    fw.write("yield;\n");
	    
	    //write the init magic dram instructions
	    fw.write("// Initialization Stage \n");
	    Iterator it = initInsList.iterator();
	    while (it.hasNext())
		fw.write("\t" + ((MagicDramInstruction)it.next()).toC());
	    //write the steady magic dram instructions with a while loop around them
	    fw.write("// Steady State \n");
	    fw.write("\twhile(1) {\n");
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
    
    
    public void addBuffer(OutputTraceNode out, InputTraceNode in) 
    {
	//add the index
	indices.add(new String(getBufferIdent(out, in)));
	
	if (!buffers.contains(new Buffer(in, out)))
	    buffers.add(new Buffer(in, out));
    }
    
    public static String getBufferIdent(OutputTraceNode out, InputTraceNode in) 
    {
	return out.getIdent() + "_" + in.getIdent();
    }
    
}
 
class Buffer 
{
    public InputTraceNode in;
    public OutputTraceNode out;
    
    public Buffer(InputTraceNode i, OutputTraceNode o) 
    {
	in = i;
	out = o;
    }

    public boolean equals(Object buf) 
    {
	if (!(buf instanceof Buffer))
	    return false;

	Buffer b = (Buffer)buf;
	    
	if (this.in == b.in && 
	    this.out == b.out)
	    return true;
	return false;
    }
}
