package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.*;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.SIRFileWriter;

public class FileWriterDevice extends IODevice
{
    private SIRFileWriter fileWriter;
    private FlatNode node;
    
    public FileWriterDevice(FlatNode node)
    {
	assert node.contents instanceof SIRFileWriter :
	    "Trying to create a FileWriterDevice with non-SIRFileWriter";
	this.node = node;
	fileWriter = (SIRFileWriter)node.contents;
    }
    
    public FlatNode getFlatNode() 
    {
	return node;
    }

    public String getFileName() 
    {
	return fileWriter.getFileName();
    }
    
    public SIRFileWriter getSIRFileWriter() 
    {
	return fileWriter;
    }
    

    public CType getType() 
    {
	return fileWriter.getInputType();
    }

    public String toString() 
    {
	return "File Writer (" + getFileName() + ")";
    }
}

