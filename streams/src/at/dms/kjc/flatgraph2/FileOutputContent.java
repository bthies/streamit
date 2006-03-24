package at.dms.kjc.flatgraph2;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;

/**
 * Predefined FilterContent for file output.
 * @author jasperln
 */
public class FileOutputContent extends OutputContent {
    private String filename; //The filename
    private int outputs; //Expected number of outputs

    /**
     * Copy constructor for FileOutputContent.
     * @param content The FileOutputContent to copy.
     */
    public FileOutputContent(FileOutputContent content) {
        super(content);
        outputs = -1;
        filename=content.filename;
    }

    /**
     * Construct FileInputContent from SIRFileWriter.
     * @param filter The SIRFileWriter used to contruct the FileInputContent.
     */
    public FileOutputContent(SIRFileWriter filter) {
        super(filter);
        outputs = -1;
        filename=filter.getFileName();
    }

    /**
     * Construct FileInputContent from UnflatFilter.
     * @param unflat The UnflatFilter used to contruct the FileInputContent.
     */
    public FileOutputContent(UnflatFilter unflat) {
        super(unflat);
        outputs = -1;
        filename=((SIRFileWriter)unflat.filter).getFileName();
    }

    /**
     * Returns expected number of outputs.
     */
    public int getOutputs() 
    {
        return outputs;
    }
    
    /**
     * Sets expected number of outputs.
     */
    public void setOutputs(int i) 
    {
        outputs = i;
    }

    /**
     * Returns filename of FileOutputContent.
     */
    public String getFileName() {
        return filename;
    }

    /**
     * Returns if output format of file is floating point.
     */
    public boolean isFP() 
    {
        return getInputType().isFloatingPoint();
    }
}
