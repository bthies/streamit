 /**
 * 
 */
package at.dms.kjc.spacetime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;

import at.dms.kjc.slicegraph.FileInputContent;
import at.dms.kjc.slicegraph.FileOutputContent;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.slicegraph.Util;

/**
 * This class asks the user to assign the input and output of each slice (trace) to a DRAM a
 * attached to the raw chip through an I/O port.  The input and output is modeled as
 * a OffChipBuffer abstract class with IntraSliceBuffer and InterSliceBuffer.  These are created
 * and assigned to ports and for each "buffer" it is decided if it will use the static network
 * or the general dynamic.  
 * 
 * @author mgordon
 *
 */
public class ManualDRAMPortAssignment {
    /** The raw chip */
    private static RawChip chip; 
    /**the traces that are file readers and writers */
    private static Slice[] files;
    private static BufferedReader inputBuffer;
    
    /**
     * Assign input and output of traces to off chip dram and decide
     * which network to use for each buffer.
     * 
     * @param spaceTime The Space/Time schedule and partition for the app. 
     */
    public static void run(SpaceTimeSchedule spaceTime) {
        chip = spaceTime.getRawChip();
        files = spaceTime.getPartitioner().io;
        inputBuffer = new BufferedReader(new InputStreamReader(
                                                               System.in));
      
        
        Iterator<SliceNode> traceNodeTrav = Util.sliceNodeTraversal(spaceTime.getPartitioner().getSliceGraph());
        while (traceNodeTrav.hasNext()) {
            SliceNode sliceNode = traceNodeTrav.next();
            // assign the buffer between inputtracenode and the filter
            // to a dram
            if (sliceNode.isInputSlice())
                manualIntraSliceAssignment((InputSliceNode) sliceNode, chip);
            // assign the buffer between the output trace node and the filter
            if (sliceNode.isOutputSlice()) {
                manualIntraSliceAssignment((OutputSliceNode) sliceNode, chip);
                manualInterSliceAssignment((OutputSliceNode)sliceNode, chip);
            }
            
            sliceNode = sliceNode.getNext();
            
            
        }
        // take care of the file readers and writes
        // assign the reader->output buffer and the input->writer buffer
        fileStuff(files, chip);
    }

    
    private static void manualInterSliceAssignment(OutputSliceNode output, RawChip chip) {
        // get the assignment for each input trace node
        Iterator edges = output.getDestSet().iterator();
        
        // commit the assignment
        while (edges.hasNext()) {
            Edge edge = (Edge) edges.next();
            String query = output + " -> " + edge.getDest();
            
            int port = getPortNumberFromUser("Assignment for: " + query, chip);
          
            InterSliceBuffer.getBuffer(edge).setDRAM((StreamingDram)chip.getDevices()[port]);
        }  
    }
    
    private static void manualIntraSliceAssignment(OutputSliceNode output, RawChip chip) {
        FilterSliceNode filter = output.getPrevFilter();
        
        //if the output trace node does nothing assign to zero
        if (output.noOutputs()) {
            IntraSliceBuffer.getBuffer(filter, output).setDRAM((StreamingDram)chip.getDevices()[0]);
            IntraSliceBuffer.getBuffer(filter, output).setStaticNet(true);
            return;
        }
        
        //it does something...
        String query =  filter + "->" + output; 
        
        int port = getPortNumberFromUser("Assignment for: " + query, chip);
        boolean staticNet = getNetworkFromUser("Static net? " + query);
        
        IntraSliceBuffer.getBuffer(filter, output).setDRAM((StreamingDram)chip.getDevices()[port]);
        IntraSliceBuffer.getBuffer(filter, output).setStaticNet(staticNet);
    }
    
    private static void manualIntraSliceAssignment(InputSliceNode input, RawChip chip) {
        FilterSliceNode filter = input.getNextFilter();
        
        if (input.noInputs()) { //if we don't do anything assign to zero
            IntraSliceBuffer.getBuffer(input, filter).setDRAM((StreamingDram)chip.getDevices()[0]);
            IntraSliceBuffer.getBuffer(input, filter).setStaticNet(true);
            return;
        }
        
        String query = input + "->" + filter; 
        
        int port = getPortNumberFromUser("Assignment for: " + query, chip);
        boolean staticNet = getNetworkFromUser("Static net? " + query);
        
        IntraSliceBuffer.getBuffer(input, filter).setDRAM((StreamingDram)chip.getDevices()[port]);
        IntraSliceBuffer.getBuffer(input, filter).setStaticNet(staticNet);
    }
        
    
    /** 
     * Ask the user for the port assignment.
     * 
     * @return the port 
     */
    private static int getPortNumberFromUser(String query, RawChip chip) {
        int portNumber = -1;
        String str = "";
        
        while (true) {
            System.out.print(query + ": ");

            try {
                str = inputBuffer.readLine();
                portNumber = Integer.valueOf(str).intValue();
            } catch (Exception e) {
                System.out.println("Bad number " + str);
                continue;
            }

            if (portNumber < 0 || portNumber >= chip.getNumDev()) {
                System.out.println("Bad port number!");
                continue;
            }
            break;
        }
        
        return portNumber; 
    }
   
    /**
     * Ask the user which network to use.
     * 
     * @return true if static network.
     */
    private static boolean getNetworkFromUser(String query) {
        boolean staticNet;
        String str = "";
        
        int num;
        while (true) {
            System.out.print(query + ": ");

            try {
                str = inputBuffer.readLine();
                num = Integer.valueOf(str).intValue();
            } catch (Exception e) {
                System.out.println("Bad number " + str);
                continue;
            }

            if (num != 0 && num != 1) {
                System.out.println("Enter 0 or 1!");
                continue;
            }
            break;
        }  
            
        return (num == 1);
    }
    
    /**
     * Assign the intra-trace buffer of file readers/writers to ports.  These are
     * the ports between inputtracenode->filewrite and filereader->outputtracenode. 
     * 
     * @param files The traces that are file readers and writers
     * @param chip The raw chip we are targeting
     */
    private static void fileStuff(Slice[] files, RawChip chip) {
        // first go thru the file, reader and writers and assign their
        // input->file and file->output buffers
        for (int i = 0; i < files.length; i++) {
            // these traces should have only one filter, make sure
            assert files[i].getHead().getNext().getNext() == files[i].getTail() : "File Slice incorrectly generated";
            FilterSliceNode filter = (FilterSliceNode) files[i].getHead()
                .getNext();

            if (files[i].getHead().isFileOutput()) {
                assert files[i].getHead().oneInput() : "buffer assignment of a joined file writer not implemented ";
                FileOutputContent fileOC = (FileOutputContent) filter
                    .getFilter();
                
                IntraSliceBuffer buf = IntraSliceBuffer.getBuffer(files[i]
                                                                  .getHead(), filter);
                // the dram of the tile where we want to add the file writer
                StreamingDram dram = buf.getDRAM();          
                // assign the other buffer to the same port
                // this should not affect anything
                IntraSliceBuffer.getBuffer(filter, files[i].getTail()).setDRAM(dram);
                // attach the file writer to the port
                dram.setFileWriter(fileOC);
            } else if (files[i].getTail().isFileInput()) {
                assert files[i].getTail().oneOutput() : "buffer assignment of a split file reader not implemented ";
                FileInputContent fileIC = (FileInputContent) filter.getFilter();
                IntraSliceBuffer buf = IntraSliceBuffer.getBuffer(filter,
                                                                  files[i].getTail());
                StreamingDram dram = buf.getDRAM();
                IntraSliceBuffer.getBuffer(files[i].getHead(), filter).setDRAM(dram);
                dram.setFileReader(fileIC);
            } else
                assert false : "File trace is neither reader or writer";
        }
    }
}