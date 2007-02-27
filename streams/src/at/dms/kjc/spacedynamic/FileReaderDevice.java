package at.dms.kjc.spacedynamic;

import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.SIRFileReader;
import at.dms.kjc.flatgraph.*;

/** This class represents a file reader in the backend.  It store various information
 * for the file reader in easy-to-access form.  It extends IODevice so it is connected 
 * to the virtual chip that we construct in this backend.
 * 
 * @author mgordon
 *
 */
public class FileReaderDevice extends IODevice {
    private SIRFileReader fileReader;
    private FlatNode node;
    /** if dynamic the downstream node of this file reader, note we only support reader that 
        connect to filter (not splitters!) when dynamic */ 
    private FlatNode dest = null;

    private StreamGraph streamGraph;
    
    public FileReaderDevice(StreamGraph sg, FlatNode node) {
        assert node.contents instanceof SIRFileReader : "Trying to create a FileReaderDevice with non-file reader";
        streamGraph = sg;
        this.node = node;
        fileReader = (SIRFileReader) node.contents;
        isDynamic = false;
    }

    public void setDynamic() {
        isDynamic = true;
        //set the destination of this filereader 
        StaticStreamGraph parent = ((StaticStreamGraph)streamGraph.getParentSSG(node));
        //if this file reader is its own SSG
        if (parent.isOutput(node)) {
            dest = parent.getNext(node);
            return;
        }
        else {
            //otherwise, just use the downstream filter as dest
            assert node.ways == 1;
            dest = node.getEdges()[0];
            return;
        }
    }
    
    
    public FlatNode getDest() {
        assert isDynamic;
        return dest;
    }
    
    public FlatNode getFlatNode() {
        return node;
    }

    public SIRFileReader getSIRFileReader() {
        return fileReader;
    }

    public String getFileName() {
        return fileReader.getFileName();
    }

    public CType getType() {
        return fileReader.getOutputType();
    }

    public String toString() {
        return "File Reader (" + getFileName() + ")";
    }

    /**
     * Return 1 for floating point or 0 for int.  Used for
     * the raw file reader device. 
     * 
     * @return 1 for floating point or 0 for int.
     */
    public String getTypeCode() {
        assert getType().isFloatingPoint() || getType().isOrdinal() : 
            "Invalid type for file reader: "
            + getType();
        Integer i = new Integer(getType().isFloatingPoint() ? 1 : 0);
        return i.toString();
    }
}
