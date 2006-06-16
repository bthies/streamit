/**
 * 
 */
package at.dms.kjc.spacedynamic;

import java.io.FileWriter;
import java.util.HashSet;
import java.util.Iterator;

import at.dms.kjc.CStdType;
import at.dms.kjc.CType;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.common.RawSimulatorPrint;
import at.dms.kjc.spacetime.Util;
import at.dms.util.Utils;

/**
 * This class creates the bC machine file for the raw simulator that will
 * describe any file I/O and generate automatic performance gathering code.
 * 
 * @author mgordon
 *
 */
public class BCFile {
    /** the entire streamgraph of the app */
    private static SpdStreamGraph streamGraph;
    /** the layout for the app */
    private static Layout layout;
    /** the raw chip that we are compiling to */
    private static RawChip rawChip;
    
    /**
     * Create the bC machine file for the raw simulator that will
     * describe any file I/O and generate automatic performance gathering code.
     * 
     * @param streamGraph The stream graph of the application.
     * @param computeTiles The tiles that are assigned to filters 
     * (performing useful work).
     */
    public static void generate(SpdStreamGraph streamGraph, HashSet computeTiles) {
        BCFile.streamGraph = streamGraph;
        layout = streamGraph.getLayout();
        rawChip = streamGraph.getRawChip();
        
        try {
            if (streamGraph.getFileState().foundReader
                    || streamGraph.getFileState().foundWriter)
                createBCFile(true, computeTiles);
            else
                createBCFile(false, computeTiles);
        }
        catch (Exception e) {
            System.err.println("Error encountered while generating bC File!");
        }
    }
        
    
    /**
     * Create the bc file for application describing file i/o and number 
     * gathering stuff.
     * 
     * @param hasIO True if we read or write a file.
     * @param tiles The tiles that are assigned for computation.
     * 
     * @throws Exception
     */
    private static void createBCFile(boolean hasIO, HashSet tiles) throws Exception {
        FileWriter fw = new FileWriter("fileio.bc");
        
        if (KjcOptions.malloczeros)
            fw.write("global gMEM_INIT_VAL = 0x0;\n");
        
        if (KjcOptions.magic_net)
            fw.write("gTurnOffNativeCompilation = 1;\n");
        
        fw.write("include(\"<dev/basic.bc>\");\n");
        
        // workaround for magic instruction support...
        fw.write("include(\"<dev/magic_instruction.bc>\");\n");
        
        // let the simulation know how many tiles are mapped to
        // filters or joiners
        
        fw.write("global gStreamItTilesUsed = " + layout.getTilesAssigned()
                + ";\n");
        fw.write("global gStreamItTiles = " + rawChip.getTotalTiles() + ";\n");
        fw.write("global gMHz_streamit = 450;\n");
        fw.write("global gStreamItUnrollFactor = " + KjcOptions.unroll + ";\n");
        fw.write("global streamit_home = getenv(\"STREAMIT_HOME\");\n");
        
        fw.write("global gStreamItOutputs = " + KjcOptions.outputs + ";\n");
        
        // create the function to tell the simulator what tiles are mapped
        fw.write("fn mapped_tile(tileNumber) {\n");
        fw.write("if (0 ");
        Iterator tilesIterator = tiles.iterator();
        // generate the if statement with all the tile numbers of mapped tiles
        while (tilesIterator.hasNext()) {
            RawTile tile = (RawTile) tilesIterator.next();
            if (layout.isAssigned(tile)) {
                fw.write("|| tileNumber == " + tile.getTileNumber() + "\n");
            }
        }
        fw.write(") {return 1; }\n");
        fw.write("return 0;\n");
        fw.write("}\n");
        
        if (KjcOptions.decoupled || RawWorkEstimator.SIMULATING_WORK) {
            fw.write("global gStreamItFilterTiles = " + tiles.size() + ";\n");
            fw.write("global gFilterNames;\n");
            
            fw.write("{\n");
            fw
            .write("  local workestpath = malloc(strlen(streamit_home) + 30);\n");
            fw.write("  gFilterNames = listi_new();\n");
            Iterator it = tiles.iterator();
            for (int i = 0; i < rawChip.getTotalTiles(); i++) {
                if (tiles.contains(rawChip.getTile(i))) {
                    fw.write("  listi_add(gFilterNames, \""
                            + layout.getNode(rawChip.getTile(i)).getName()
                            + "\");\n");
                }
            }
            //use a magic instruction handler to handle the stuff...
            fw.write("sprintf(workestpath, \"%s%s\", streamit_home, \"/include/work_est.bc\");\n");
            fw.write("include(workestpath);\n");
            fw.write("work_est_init();\n");
            fw.write("}\n");
        }
        
        // number gathering code
        if (KjcOptions.numbers > 0 && !IMEMEstimation.TESTING_IMEM) {
            fw.write("global printsPerCycle = " + KjcOptions.numbers + ";\n");
            fw.write("global quitAfter = " + 10 + ";\n");
            fw.write("{\n");
            fw
            .write("  local numberpath = malloc(strlen(streamit_home) + 30);\n");
            fw
            .write("  sprintf(numberpath, \"%s%s\", streamit_home, \"/include/sd_numbers.bc\");\n");
            // include the number gathering code and install the device file
            fw.write("  include(numberpath);\n");
            // call the number gathering initialization function
            fw.write("  gather_numbers_init();\n");
            fw.write("}\n");
        }
        
        //generate the bc code for the magic print handler...
        fw.write(RawSimulatorPrint.bCMagicHandler());
        
        fw
        .write("global gAUTOFLOPS = 0;\n"
                + "fn __event_fpu_count(hms)\n"
                + "{"
                + "\tlocal instrDynamic = hms.instr_dynamic;\n"
                + "\tlocal instrWord = InstrDynamic_GetInstrWord(instrDynamic);\n"
                + "\tif (imem_instr_is_fpu(instrWord))\n"
                + "\t{\n"
                + "\t\tAtomicIncrement(&gAUTOFLOPS);\n"
                + "\t}\n"
                + "}\n\n"
                + "EventManager_RegisterHandler(\"issued_instruction\", \"__event_fpu_count\");\n"
                +
                
                "fn count_FLOPS(steps)\n"
                + "{\n"
                + "  gAUTOFLOPS = 0;\n"
                + "  step(steps);\n"
                + "  printf(\"// **** count_FLOPS: %4d FLOPS, %4d mFLOPS\\n\",\n"
                + "         gAUTOFLOPS, (450*gAUTOFLOPS)/steps);\n"
                + "}\n" + "\n");
        
        fw.write("\n{\n");
        
        if (KjcOptions.magic_net)
            fw.write("  addMagicNetwork();\n");
        
        if (hasIO) {
            // generate the code for the fileReaders
            Iterator frs = streamGraph.getFileState().getFileReaderDevs()
            .iterator();
            // include the file reader devices
            fw.write("\tlocal f_readerpath = malloc(strlen(streamit_home) + 30);\n");                
            fw.write("\tlocal f_writerpath = malloc(strlen(streamit_home) + 30);\n");                
            fw.write("\tsprintf(f_readerpath, \"%s%s\", streamit_home, \"/include/from_file.bc\");\n");
            fw.write("\tsprintf(f_writerpath, \"%s%s\", streamit_home, \"/include/to_file.bc\");\n");
            fw.write("\tinclude(f_readerpath);\n");
            fw.write("\tinclude(f_writerpath);\n");

            while (frs.hasNext()) {
                FileReaderDevice dev = (FileReaderDevice) frs.next();
                //if this file reader is its own device or we don't have a communication simulator
                //then use the dynamic network
                int static_network = 1, destX = 0, destY = 0; 
                if (dev.isDynamic() || 
                        ((SpdStaticStreamGraph)streamGraph.getParentSSG(dev.getFlatNode())).simulator instanceof NoSimulator) {
                    dev.setDynamic();
                    static_network = 0;
                    //set the dest, only used for data over gdn
                    destX = layout.getTile(dev.getDest()).getY();  
                    destY = layout.getTile(dev.getDest()).getX(); 
                }
               
                fw.write("\tdev_from_file(\"" + dev.getFileName() + "\", " +
                        dev.getPort().getPortNumber() + ", " +
                        static_network + ", " +
                        (KjcOptions.asciifileio ? "0" : "1" ) + ", " +
                        dev.getTypeCode() + ", " +
                        "1, " + //wait for trigger
                        dev.getPort().getY() + ", " + 
                        dev.getPort().getX()  + ", " + 
                        destY + ", " +  
                        destX + 
                        ");\n");
            }
            // generate the code for the file writers
            Iterator fws = streamGraph.getFileState().getFileWriterDevs()
            .iterator();
            while (fws.hasNext()) {
                FileWriterDevice dev = (FileWriterDevice) fws.next();
                int size = getTypeSize(dev.getType());
//              if this file write is its own device or we don't have a communication simulator
                //then use the dynamic network
                boolean dynamic = dev.isDynamic() || 
                ((SpdStaticStreamGraph)streamGraph.getParentSSG(dev.getFlatNode())).simulator instanceof NoSimulator;
                if (dynamic)
                    dev.setDynamic();
                //now create the function call the creates the bc device, create the 
                //appropriate device based on what network is used
                fw.write("  dev_to_file(\"" + 
                        dev.getFileName()
                        + "\", " + dev.getPort().getPortNumber() + ", " +
                        (dynamic ? "0, " : "1, ") + //network
                        "0, " + //don't wait for trigger
                        (KjcOptions.asciifileio ? "0, " : "1, ") + 
                        dev.getTypeCode() + ", " +
                        size + 
                ");\n");
            }
        }
        
        fw.write("\n}\n");
        fw.close();
    }
    
    
    /**
     * Given a type return the number of bytes it occupies.
     * 
     * @param type The type.
     * @return The number of bytes for type.
     */
    public static int getTypeSize(CType type) {
        if (type.equals(CStdType.Boolean))
            return 1;
        else if (type.equals(CStdType.Byte))
            return 1;
        else if (type.equals(CStdType.Integer))
            return 4;
        else if (type.equals(CStdType.Short))
            return 4;
        else if (type.equals(CStdType.Char))
            return 1;
        else if (type.equals(CStdType.Float))
            return 4;
        else if (type.equals(CStdType.Long))
            return 4;
        else {
            Utils.fail("Cannot write type to file: " + type);
        }
        return 0;
    }
}
