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
    private static StreamGraph streamGraph;
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
    public static void generate(StreamGraph streamGraph, HashSet computeTiles) {
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
        
        if (KjcOptions.decoupled) {
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
            fw
            .write("  sprintf(workestpath, \"%s%s\", streamit_home, \"/include/work_est.bc\");\n");
            // include the number gathering code and install the device file
            fw.write("  include(workestpath);\n");
            // add print service to the south of the SE tile
            fw.write("  {\n");
            fw.write("    local str = malloc(256);\n");
            fw.write("    local result;\n");
            fw.write("    sprintf(str, \"/tmp/%s.log\", *int_EA(gArgv,0));\n");
            fw
            .write("    result = dev_work_est_init(\"/dev/null\", gXSize+gYSize);\n");
            fw.write("    if (result == 0)\n");
            fw.write("      exit(-1);\n");
            fw.write("  }\n");
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
        
        if (hasIO) {
            // create preamble
            fw
            .write("if (FindFunctionInSymbolHash(gSymbolTable, \"dev_data_transmitter_init\",3) == NULL)\n");
            fw.write("include(\"<dev/data_transmitter.bc>\");\n\n");
            
            // create the instrumentation function
            fw.write("// instrumentation code\n");
            fw.write("fn streamit_instrument(val, port){\n");
            fw.write("  local a;\n");
            fw.write("  local b;\n");
            fw
            .write("  Proc_GetCycleCounter(Machine_GetProc(machine,0), &a, &b);\n");
            fw.write("  //printf(\"cycleHi %X, cycleLo %X\\n\", a, b);\n");
            // use the same format string that generating a printf causes so we
            // can use
            // the same results script;
            fw.write("  printf(\"[port %d: %08x%08x]: %x\\n\", port, a, b, val);\n");
            fw.write("}\n\n");
            
            // create the function to write the data from the static network to a file
            fw.write("fn dev_st_port_to_file_size(filename, size, port)\n{\n");
            fw.write("  local receive_device_descriptor = hms_new();\n");
            fw.write("  // open the file\n  ;");
            fw.write("  receive_device_descriptor.fileName = filename;\n  ");
            fw.write("  receive_device_descriptor.port = port;\n");
            fw.write("  receive_device_descriptor.theFile = fopen(receive_device_descriptor.fileName,\"w\");\n");
            fw.write("  verify(receive_device_descriptor.theFile != NULL, \"### Failed to open output file\");\n");
            fw.write("  receive_device_descriptor.calc =\n");
            fw.write("    & fn(this)\n  {\n");
            fw.write("    local theFile = this.theFile;\n");
            fw.write("    local thePort = this.port;;\n");
            fw.write("    while (1)\n {\n");
            fw.write("      local value = this.receive();\n");
            fw.write("      fwrite(&value, size, 1, theFile);\n");
            fw.write("      streamit_instrument(value, thePort);\n");
            fw.write("      fflush(theFile);\n");
            fw.write("    }\n");
            fw.write("  };\n");
            fw.write("  return dev_data_transmitter_init(\"st_port_to_file\", port, 0, "
                    + "  receive_device_descriptor, 0" + 
            ");\n");
            fw.write("}\n\n");
            
            //create the function to write data from the GDN to a file
            fw.write("fn dev_gdn_port_to_file_size(filename, size, port)\n");
            fw.write("{\n");
            fw.write("  local receive_device_descriptor = hms_new();\n");
            fw.write("  // open the file\n");
            fw.write("  receive_device_descriptor.fileName = filename;\n");
            fw.write("  receive_device_descriptor.port = port;\n");
            fw.write("  receive_device_descriptor.theFile = fopen(receive_device_descriptor.fileName,\"w\");\n");
            fw.write("  verify(receive_device_descriptor.theFile != NULL, \"### Failed to open output file\");\n");
            fw.write("  receive_device_descriptor.calc =\n");
            fw.write("    & fn(this)\n");
            fw.write("    {\n");
            fw.write("      local theFile = this.theFile;\n");
            fw.write("      local thePort = this.port;\n");
            fw.write("      while (1)\n");
            fw.write("      {\n");
            fw.write("        local value = this.receive();\n");
            fw.write("        local i, bogus, length, senderY, senderX, ourY, ourX, hdr;\n");
            fw.write("\n");
            fw.write("        DecodeDynHdr(value, &bogus, &length, &hdr,\n");
            fw.write("                     &senderY, &senderX, &ourY, &ourX);\n");
            fw.write("\n");
            fw.write("        for (i = 0; i < length; i++) {\n");
            fw.write("        value = this.receive();\n");
            fw.write("        fwrite(&value, size, 1, theFile);\n");
            fw.write("        streamit_instrument(value, thePort);\n");
            fw.write("        fflush(theFile);\n");
            fw.write("      }\n");
            fw.write("    }\n");
            fw.write("  };\n");
            fw.write("  return dev_data_transmitter_init(\"gdn_port_to_file\", port, 0, receive_device_descriptor, 2);\n");
            fw.write("}\n\n");
        }
        //
        fw.write("\n{\n");
        
        if (KjcOptions.magic_net)
            fw.write("  addMagicNetwork();\n");
        
        if (hasIO) {
            // generate the code for the fileReaders
            Iterator frs = streamGraph.getFileState().getFileReaderDevs()
            .iterator();
            if (frs.hasNext()) {
                // include the file reader devices
                fw.write("\tlocal f_readerpath = malloc(strlen(streamit_home) + 30);\n");
                fw.write("\tlocal f_readerpath_dyn = malloc(strlen(streamit_home) + 30);\n");
                
                fw.write("\tsprintf(f_readerpath, \"%s%s\", streamit_home, \"/include/from_file_raw.bc\");\n");
                fw.write("\tinclude(f_readerpath);\n");
                
                fw.write("\tsprintf(f_readerpath_dyn, \"%s%s\", streamit_home, \"/include/from_file_dyn_raw.bc\");\n");
                fw.write("\tinclude(f_readerpath_dyn);\n");
            }
            while (frs.hasNext()) {
                FileReaderDevice dev = (FileReaderDevice) frs.next();
                //if this file reader is its own device or we don't have a communication simulator
                //then use the dynamic network
                if (dev.isDynamic() || 
                        streamGraph.getParentSSG(dev.getFlatNode()).simulator instanceof NoSimulator) {
                    dev.setDynamic();
                    fw.write("\tdev_from_file_dyn_raw(\"" + dev.getFileName() + "\", " +
                            dev.getPort().getY() + ", " + 
                            dev.getPort().getX()  + ", " + 
                            layout.getTile(dev.getDest()).getY() + ", " +  
                            layout.getTile(dev.getDest()).getX() + ", " + 
                            dev.getPort().getPortNumber() + ");\n");
                }
                else {
                    fw.write("\tdev_from_file_raw(\"" + dev.getFileName() + "\", "
                            + dev.getPort().getPortNumber() + ");\n");
                }
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
                streamGraph.getParentSSG(dev.getFlatNode()).simulator instanceof NoSimulator;
                if (dynamic)
                    dev.setDynamic();
                //now create the function call the creates the bc device, create the 
                //appropriate device based on what network is used
                fw.write("\tdev_" + ( dynamic ? "gdn" : "st") + "_port_to_file_size(\"" + 
                        dev.getFileName()
                        + "\", " + size + ", " + 
                        dev.getPort().getPortNumber() + 
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
