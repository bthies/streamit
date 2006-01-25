package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.*;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.SIRFileWriter;

public class FileWriterDevice extends IODevice {
    private SIRFileWriter fileWriter;

    private FlatNode node;

    private StreamGraph streamGraph;

    public FileWriterDevice(StreamGraph sg, FlatNode node) {
        assert node.contents instanceof SIRFileWriter : "Trying to create a FileWriterDevice with non-SIRFileWriter";
        this.node = node;
        this.streamGraph = sg;
        fileWriter = (SIRFileWriter) node.contents;
        isDynamic = false;
    }

    public FlatNode getFlatNode() {
        return node;
    }

    public void setDynamic() 
    {
        isDynamic = true;
    }
    

    public String getFileName() {
        return fileWriter.getFileName();
    }

    public SIRFileWriter getSIRFileWriter() {
        return fileWriter;
    }

    /** return the data type that this file writer writes */
    public CType getType() {
        //if we have a dynamic file writer we cannot query the 
        //type of the SIRFW because the type was set to void, so 
        //ask the parent SSG
        if (isDynamic) 
            return streamGraph.getParentSSG(node).getInputType(node);
        else 
            return fileWriter.getInputType();
    }

    public String toString() {
        return "File Writer (" + getFileName() + ")";
    }

    public String getTypeCode() {
        assert getType().isFloatingPoint() || getType().isOrdinal() : "Invalid type for file writer: "
            + getType();
        Integer i = new Integer(getType().isFloatingPoint() ? 1 : 0);
        return i.toString();
    }
}
