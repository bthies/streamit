 package at.dms.kjc.flatgraph2;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;

public class FileOutputContent extends OutputContent {
    private String filename;
    private int outputs;

    public FileOutputContent(FileOutputContent content) {
	super(content);
	outputs = -1;
	filename=content.filename;
    }

    public FileOutputContent(SIRFileWriter filter) {
	super(filter);
	outputs = -1;
	filename=filter.getFileName();
    }

    public FileOutputContent(UnflatFilter unflat) {
	super(unflat);
	outputs = -1;
	filename=((SIRFileWriter)unflat.filter).getFileName();
    }

    public int getOutputs() 
    {
	return outputs;
    }
    
    public void setOutputs(int i) 
    {
	outputs = i;
    }

    public String getFileName() {
	return filename;
    }

    public boolean isFP() 
    {
	return getInputType().isFloatingPoint();
    }
}
