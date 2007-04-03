package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.io.*;
import java.util.*;

/** 
 * Set up in the backend the various file readers or writers by creating the necessary 
 * devices and connecting them to the chip.
 * 
 * @author mgordon
 */
public class FileState implements StreamGraphVisitor, FlatVisitor {
    /** set to true if you want to query the user for the port assignment
     * for each file reader and writer.
     */
    private static final boolean MANUAL = false;
    
    // true if the graph contains a fileReader
    public boolean foundReader;

    // a hashmap FlatNode -> FileReaderDevice
    private HashMap<Object, FileReaderDevice> fileReaders;

    // true if the graph contains a fileWriter
    public boolean foundWriter;

    // a hashmap FlatNode -> FileWriterDevice
    private HashMap<Object, FileWriterDevice> fileWriters;

    // a hashset containing the flatnodes of all the file manipulators
    // (both readers and writers
    public HashSet<Object> fileNodes;

    private RawChip rawChip;

    private SpdStreamGraph streamGraph;

    // the buffered reader from where we get the assignment
    private BufferedReader inputBuffer;

    /** As we are assigning ports to readers, this is the next port num to assign */
    private int readerPort;
    /** As we are assigning ports to writers, this is the next port num to assign */
    private int writerPort;
    
    public void visitStaticStreamGraph(SpdStaticStreamGraph ssg) {
        ssg.getTopLevel().accept(this, new HashSet<FlatNode>(), false);
    }

    public FileState(SpdStreamGraph streamGraph) {
        this.streamGraph = streamGraph;
        this.rawChip = streamGraph.getRawChip();
        foundReader = false;
        foundWriter = false;
        fileReaders = new HashMap<Object, FileReaderDevice>();
        fileWriters = new HashMap<Object, FileWriterDevice>();
        fileNodes = new HashSet<Object>();
        
        switch (rawChip.getTotalSimulatedTiles()) {
        case 1: readerPort = 0; writerPort = 0; break;
        case 2: readerPort = 0; writerPort = 0; break;
        case 4: readerPort = 1; writerPort = 14; break;
        case 9: readerPort = 1; writerPort = 14; break;
        case 16: readerPort = 1; writerPort = 9; break;
        case 25: readerPort = 2; writerPort = 29; break;
        case 36: readerPort = 3; writerPort = 28; break;
        case 49: readerPort = 3; writerPort = 27; break;
        case 64: readerPort = 3; writerPort = 18; break;
        default: readerPort = 0;
            writerPort = (rawChip.getNumPorts() / 2) + (rawChip.getXSize());
        }
       
        if (KjcOptions.devassignfile != null) {
            try { // read from the file specified...
                inputBuffer = new BufferedReader(new FileReader(
                                                                KjcOptions.devassignfile));
            } catch (Exception e) {
                System.err
                    .println("Error opening device-to-port assignment file "
                             + KjcOptions.devassignfile);
                System.exit(1);
            }
        } else
            // otherwise read for standard input
            inputBuffer = new BufferedReader(new InputStreamReader(System.in));

        ((SpdStaticStreamGraph)streamGraph.getTopLevel()).accept(this, null, true);

        try { // close the file
            if (KjcOptions.devassignfile != null)
                inputBuffer.close();
        } catch (Exception e) {
            System.err
                .println("Error closing device-to-port assignment stream");
            System.exit(1);
        }

        if (rawChip.getTotalTiles() == 1) {
            assert fileReaders.keySet().size() < 2 && 
                fileWriters.keySet().size() < 2 &&
                fileReaders.keySet().size() + fileWriters.keySet().size() <= 2 :
                    "Too many file devices for chip!";
        }
        
        // add everything to the fileNodes hashset
        SpaceDynamicBackend.addAll(fileNodes, fileReaders.keySet());
        SpaceDynamicBackend.addAll(fileNodes, fileWriters.keySet());
    }

    /** If we have a file reader or writer, create the device and connect
        it to the raw chip **/
    public void visitNode(FlatNode node) {
        //lots of duplication here, but oh well
        if (node.contents instanceof SIRFileReader) {
            FileReaderDevice dev = new FileReaderDevice(streamGraph, node);
            SpdStaticStreamGraph parent = (SpdStaticStreamGraph)streamGraph.getParentSSG(node);
            
            if (parent.getIOFilters().contains(node.contents)) {
                //we have a dynamic file reader
                dev.setDynamic();
            }

            IOPort port = getPort(dev);
            rawChip.connectDevice(dev, port);
            System.out.println("Assigning " + dev + " to " + port + ".");
            fileReaders.put(node, dev);
            foundReader = true;
        } else if (node.contents instanceof SIRFileWriter) {
            FileWriterDevice dev = new FileWriterDevice(streamGraph, node);
            SpdStaticStreamGraph parent = (SpdStaticStreamGraph)streamGraph.getParentSSG(node);
            
            if (parent.getIOFilters().contains(node.contents)) {
                //we have a dynamic file reader
                dev.setDynamic();
            }


            IOPort port = getPort(dev);
            rawChip.connectDevice(dev, port);
            System.out.println("Assigning " + dev + " to " + port + ".");
            foundWriter = true;
            fileWriters.put(node, dev);
        }
    }

    private IOPort getPort(IODevice dev) {
        //if we want to query the user, then do it
        if (MANUAL)
            return getPortFromUser(dev);
        //assign automatically, file readers start from port 0
        //and increase to max port -1
        if (dev.isFileReader()) {
            assert readerPort >=0 && readerPort < rawChip.getNumPorts() :
                "Error assigning ports to file reader: probably too many file readers.";
            return rawChip.getIOPort(readerPort++);
            
        } else {
            //file writers start at maxport / 2 and go to maxPort / 2 - 1
            assert writerPort >=0 && writerPort < rawChip.getNumPorts() :
                 "Error assigning ports to file writer: probably too many file writers.";
            IOPort port = rawChip.getIOPort(writerPort); 
            writerPort = (writerPort + 1) & rawChip.getNumPorts();
            return port;
        }
    }
    
    private IOPort getPortFromUser(IODevice dev) {
        int num;

        while (true) {
            String str = null;

            System.out.print("Enter port number for " + dev.toString() + ": ");
            try {
                str = inputBuffer.readLine();
                num = Integer.valueOf(str).intValue();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Bad number! (" + str + ")");
                continue;
            }

            if (num < 0 || num >= streamGraph.getRawChip().getNumPorts()) {
                System.out.println("Enter valid port number  [0, "
                                   + streamGraph.getRawChip().getNumPorts() + ")\n");
                continue;
            }

            if (streamGraph.getRawChip().getIOPort(num).hasDevice()) {
                System.out.println("Port " + num
                                   + " already assigned a device!\n");
                continue;
            }
            break;
        }

        System.out.println(dev.toString() + " assigned to port " + num);
        return rawChip.getIOPort(num);
    }

    /** get the FileWriterDevice that implements this SIRFileWriter. 
     * 
     * @param fw
     * @return The FileWriterDevice
     */
    public FileWriterDevice getFileWriterDevice(FlatNode fw) {
        assert fileWriters.containsKey(fw);
        return fileWriters.get(fw);
    }
    
    public Collection<FileWriterDevice> getFileWriterDevs() {
        return fileWriters.values();
    }

    public Collection<FileReaderDevice> getFileReaderDevs() {
        return fileReaders.values();
    }

    public boolean isConnectedToFileReader(RawTile tile) {
        for (int i = 0; i < tile.getIOPorts().length; i++) {
            for (int d = 0; d < tile.getIOPorts()[i].getDevices().length; d++) {
                if (tile.getIOPorts()[i].getDevices()[d] != null &&
                        tile.getIOPorts()[i].getDevices()[d] instanceof FileReaderDevice)
                    return true;
            }
        }
        return false;
    }

}
