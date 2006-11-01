package at.dms.kjc.slicegraph;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;

/**
 * Predefined FilterContent for file input.
 * @author jasperln
 */
public class FileInputContent extends InputContent {
    private String filename; //The filename
    
    /**
     * Copy constructor for FileInputContent.
     * @param content The FileInputContent to copy.
     */
    public FileInputContent(FileInputContent content) {
        super(content);
        filename=content.filename;
    }

    /**
     * Construct FileInputContent from SIRFileReader.
     * @param filter The SIRFileReader used to contruct the FileInputContent.
     */
    public FileInputContent(SIRFileReader filter) {
        super(filter);
        filename=filter.getFileName();
    }

    /**
     * Construct FileInputContent from UnflatFilter.
     * @param unflat The UnflatFilter used to contruct the FileInputContent.
     */
    public FileInputContent(UnflatFilter unflat) {
        super(unflat);
        filename=((SIRFileReader)unflat.filter).getFileName();
    }

    /**
     * Returns filename of FileInputContent.
     */
    public String getFileName() {
        return filename;
    }
    
    /**
     * Returns if output format of file is floating point.
     */
    public boolean isFP() 
    {
        return getOutputType().isFloatingPoint();
    }
    
    /**
     * Return the type of the file reader
     *  
     * @return The type.
     */
    public CType getType() {
        return getOutputType();
    }
}
