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
    
    public String getTypeCode() 
    {
	assert getType().isFloatingPoint() ||
	    getType().isOrdinal() : "Invalid type for file reader: " + getType();
	Integer i = 
	    new Integer(getType().isFloatingPoint() ? 1 : 0);
	return i.toString();
    }
}

