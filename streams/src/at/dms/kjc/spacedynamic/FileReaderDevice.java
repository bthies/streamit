package at.dms.kjc.spacedynamic;

import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.SIRFileReader;
import at.dms.kjc.flatgraph.*;

public class FileReaderDevice extends IODevice
{
    private SIRFileReader fileReader;
    private FlatNode node;
    
    public FileReaderDevice(FlatNode node) 
    {
	assert node.contents instanceof SIRFileReader :
	    "Trying to create a FileReaderDevice with non-file reader";
	    
	this.node = node;
	fileReader = (SIRFileReader)node.contents;
    }

    public FlatNode getFlatNode() 
    {
	return node;
    }			   

    public SIRFileReader getSIRFileReader() 
    {
	return fileReader;
    }
    
    
    public String getFileName() 
    {
	return fileReader.getFileName();
    }
    
    public CType getType() 
    {
	return fileReader.getOutputType();
    }

    public String toString() 
    {
	return "File Reader (" + getFileName() + ")";
    }
    
}

