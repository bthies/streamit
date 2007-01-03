package at.dms.kjc.spacetime;

import java.lang.StringBuffer;
import at.dms.kjc.*;

import java.io.FileWriter;

import at.dms.kjc.common.RawSimulatorPrint;
import at.dms.kjc.slicegraph.*;

/**
 * This class create the btl machine file for the application.  
 * It defines some number gathering stuff and some file i/o stuff 
 * and some useful functions while debugging in the simulator.
 * 
 * @author mgordon
 *
 */
public class BCFile 
{
    /** the raw chip we are compiling to */
    private RawChip rawChip;
    /** the string that stores the current btl code for the machine file */
    private StringBuffer buf;
    /** The number gathering structure for this application */
    private NumberGathering ng;
    /** Should we generate numbers gathering code */
    private boolean numbers;
    /** The name of file we are generating */
    public static final String BCFILE_NAME = "setup.bc";
    /** The space time schedule of application */
    private SpaceTimeSchedule spaceTime;

    /**
     * Generate the btl machine file that describes the application 
     * configuration, the file readers and writers (the external devices) 
     * and the number gathering code.  
     *  
     * @param rc The RawChip.
     * @param gn The NumberGathering structure.
     */
    public static void generate(SpaceTimeSchedule sp, RawChip rc, 
            NumberGathering gn) 
    {
        (new BCFile(sp, rc, gn)).doit();
    }
    
    /**
     * Create a new BCFile object that will generate the btl machine file.
     * 
     * @param rc The rawchip,
     * @param g The number gathering structures.
     */
    private BCFile(SpaceTimeSchedule sp, RawChip rc, NumberGathering g) 
    {
        spaceTime = sp;
        rawChip = rc;
        ng = g;
        //set numbers to true if we are given a number gathering object
        numbers = (ng != null);
    }
    
    /**
     * Actually generate the bc code that describes the machine
     * configuration and any helper functions.
     *
     */
    private void doit() 
    {
        buf = new StringBuffer();
        if (KjcOptions.malloczeros)
            buf.append("global gMEM_INIT_VAL = 0x0;\n");
        
        if (KjcOptions.magic_net) 
            buf.append("gTurnOffNativeCompilation = 1;\n");

        //set the bandwidth of the streaming memories
        buf.append("global gStreamingDRAMCyclesPerWord = " + KjcOptions.st_cyc_per_wd + ";\n");
        
        buf.append("include(\"<dev/basic.bc>\");\n");

        //workaround for magic instruction support...
        buf.append("include(\"<dev/magic_instruction.bc>\");\n");
        
                
        //let the simulation know how many tiles are mapped to 
        //filters or joiners
        buf.append("global gMHz = 450;\n");
        buf.append("global gStreamItTilesUsed = " + rawChip.computingTiles() + ";\n");
        buf.append("global gStreamItTiles = " + rawChip.getXSize() * rawChip.getYSize() +
                   ";\n");
        buf.append("global streamit_home = getenv(\"STREAMIT_HOME\");\n\n");      

        buf.append("global to_file_path = malloc(strlen(streamit_home) + 30);\n"); 
        buf.append("global from_file_path = malloc(strlen(streamit_home) + 30);\n\n"); 
        buf.append("global gStreamItOutputs = " + KjcOptions.outputs + ";\n");
        
        //number gathering stuff
        if (numbers) {
            numberGathering();
        }
        

        //include the file reader/writer devices
        buf.append("sprintf(to_file_path, \"%s%s\", streamit_home, \"/include/to_file.bc\");\n"); 
        buf.append("sprintf(from_file_path, \"%s%s\", streamit_home, \"/include/from_file.bc\");\n"); 
        buf.append("include(to_file_path);\n"); 
        buf.append("include(from_file_path);\n"); 
        
        buf.append("fn quit_sim()\n{\n");
        buf.append("\tgInterrupted = 1;\n");
        buf.append("\texit_now(0);\n");
        buf.append("}\n");
        
        //generate the bc code for the magic print handler...
        buf.append(RawSimulatorPrint.bCMagicHandler());
        
        files();

        if (KjcOptions.decoupled)
            decoupled();
        mappedFunction();

//        if (KjcOptions.magicdram) 
//            magicDram();

        if (KjcOptions.magic_net)
            magicNet();
        flopsCount();
        setupFunction();

        writeFile();
    }
    
    /**
     * If we are number gathering, then generate bc code
     * to use the number gathering file output device and 
     * generate the state of the number gathering variables.
     */
    private void numberGathering() 
    {
        //include the device
        buf.append("global gTotalSS = " + KjcOptions.numbers + ";\n");
        buf.append("global to_file_numbers_path = malloc(strlen(streamit_home) + 40);\n"); 
        buf.append("sprintf(to_file_numbers_path, \"%s%s\", streamit_home, \"/include/to_file_numbers.bc\");\n"); 
        buf.append("include(to_file_numbers_path);\n"); 
        buf.append("global gStreamItTiles = " + rawChip.getYSize() * rawChip.getXSize() + ";\n");
        int mappedTiles = 0;
        for (int i = 0; i < rawChip.getXSize(); i++) 
            for (int j = 0; j < rawChip.getYSize(); j++) {
                RawTile tile = rawChip.getTile(i, j);
                if (tile.isMapped()) {
                    mappedTiles++;
                }
            }
        //append the number of filters and the number of slices
        Slice slices[] = spaceTime.getPartitioner().getSliceGraph();
        int numSlices = 0;
        int numFilters = 0;
        for (int i = 0; i < slices.length; i++) {
            //don't count file reader and writers
            if (slices[i].getHead().getNextFilter().isFileInput() || 
                    slices[i].getHead().getNextFilter().isFileOutput())
                continue;
            numSlices++;
            numFilters += slices[i].getNumFilters();
        }
        
        buf.append("global gNumSlices = " + numSlices + ";\n");
        buf.append("global gNumFilters = " + numFilters + ";\n");
        buf.append("global gMappedTiles = " + mappedTiles + ";\n");
        buf.append("global gOffChipBufferSizeBytes = " + 
                OffChipBuffer.totalBufferSizeInBytes().toString() + ";\n");
        //define the vars
        //define the number of items received so far and zero it
        buf.append("global gNGItems;\n");
        buf.append("global gNGskip;\n");
        buf.append("global gNGsteady;\n");
        buf.append("global gNGfws = " + ng.fileWriters.length + ";\n");
        buf.append("global gTotalSteadyItems = " + ng.totalSteadyItems + ";\n");
        //malloc and set the arrays
        buf.append("\n{ //Number Gathering \n");
        buf.append("  gNGItems = malloc(gNGfws * 4);\n");
        buf.append("  gNGskip = malloc(gNGfws * 4);\n");
        buf.append("  gNGsteady = malloc(gNGfws * 4);\n\n");
        for (int i = 0; i < ng.fileWriters.length; i++) {
            buf.append("  gNGskip[" + i + "] = " + ng.skip[i] + ";\n");
            buf.append("  gNGsteady[" + i + "] = " + ng.steady[i] + ";\n");
        }
        //install the event handlers
        buf.append("\n  install_event_handlers();\n");
        buf.append("}\n");
        
    }
    
    /**
     * If this application performs file i/o, attach the
     * file reading and file writing devices to the appropriate 
     * DRAMs.  Use the number gathering file writer if we gather numbers.
     */
    private void files()
    {
        buf.append("\n{\n");    
        //print the declartions of the slave port var
        for (int i = 0; i < rawChip.getXSize(); i++) {
            for (int j = 0; j < rawChip.getYSize(); j++) {
                RawTile tile = rawChip.getTile(i, j);
                for (int d = 0; d < tile.getIODevices().length; d++) {
                    if (tile.getIODevices()[d] instanceof StreamingDram) {
                        StreamingDram dram = (StreamingDram)tile.getIODevices()[d];
                        if (dram.isFileWriter() || dram.isFileReader())
                            buf.append("  local slavePort" + dram.getPort() + " = " + 
                                       "dev_streaming_dram_at_port(" + dram.getPort() + 
                                       ").get_slave_port_fn();\n");
                    }
                }
            }
        }
        
        for (int i = 0; i < rawChip.getXSize(); i++) {
            for (int j = 0; j < rawChip.getYSize(); j++) {
                RawTile tile = rawChip.getTile(i, j);
                for (int d = 0; d < tile.getIODevices().length; d++) {
                    if (tile.getIODevices()[d] instanceof StreamingDram) {
                        StreamingDram dram = (StreamingDram)tile.getIODevices()[d];
                        if (dram.isFileReader()) {
                            buf.append("  dev_from_file(\"" + 
                                    dram.getFileReader().getFileName() +
                                    "\", slavePort" + dram.getPort() + ", " + 
                                    "1, " + //always use static network
                                    (KjcOptions.asciifileio ? "0, " : "1, ") + 
                                    (dram.getFileReader().isFP() ? "1, " : "0, ") +
                                    "0, " + //don't wait for trigger
                                    "0, 0, 0, 0" + //just use zero, only used for gdn
                                    ");\n");
                        }
                        if (dram.isFileWriter()) {
                            //if numbers call the ng file writer
                            if (numbers) {
                                buf.append("  dev_NG_to_file(" + 
                                           ng.getID((FileOutputContent)dram.getFileWriter().getContent()) + 
                                           ", \"");
                                buf.append(dram.getFileWriter().getFileName() +
                                        "\", slavePort" + dram.getPort() + ", " +
                                        "1, " + //always use static net
                                        (KjcOptions.asciifileio ? "0, " : "1, ") + 
                                        (dram.getFileWriter().isFP() ? "1, " : "0, ") +
                                        (4 * Util.getTypeSize(dram.getFileWriter().getContent().getInputType())) + 
                                ");\n");
                            }
                            else { 
                                buf.append("  dev_to_file(\"" + 
                                        dram.getFileWriter().getFileName() +
                                        "\", slavePort" + dram.getPort() + ", " +
                                        "1, " + //static network
                                        "0, " + //don't wait for trigger
                                        (KjcOptions.asciifileio ? "0, " : "1, ") + 
                                        (dram.getFileWriter().isFP() ? "1, " : "0, ") +
                                        (4 * Util.getTypeSize(dram.getFileWriter().getContent().getInputType())) + 
                                ");\n");
                            }
                        }
                    }
                }
            }
        }
        buf.append("\n}\n");
    }
    
    
    /**
     * If we are using the magic net, then call the magic net
     * setup function.  Otherwise do nothing.
     *
     */
    private void setupFunction() 
    {
        buf.append("\n{\n");    
        
        if (KjcOptions.magic_net)
            buf.append("  addMagicNetwork();\n");

        buf.append("\n}\n");
    }

    /**
     * Create the code for the flops counter.
     *
     */
    private void flopsCount() 
    {
        buf.append
            ("global gAUTOFLOPS = 0;\n" +
             "fn __event_fpu_count(hms)\n" +
             "{" +
             "\tlocal instrDynamic = hms.instr_dynamic;\n" +
             "\tlocal instrWord = InstrDynamic_GetInstrWord(instrDynamic);\n" +
             "\tif (imem_instr_is_fpu(instrWord))\n" +
             "\t{\n" +
             "\t\tAtomicIncrement(&gAUTOFLOPS);\n" +
             "\t}\n" +
             "}\n\n" +
             "EventManager_RegisterHandler(\"issued_instruction\", \"__event_fpu_count\");\n" +

             "fn count_FLOPS(steps)\n" +
             "{\n" +
             "  gAUTOFLOPS = 0;\n" +
             "  step(steps);\n" +
             "  printf(\"// **** count_FLOPS: %4d FLOPS, %4d mFLOPS\\n\",\n" +
             "         gAUTOFLOPS, (450*gAUTOFLOPS)/steps);\n" +
             "}\n" +
             "\n");     
    }
    
    /**
     * Create the code that will initialize the magic net schedules.
     */
    private void magicNet() 
    {
        buf.append("include(\"magic_schedules.bc\");\n");
         
        buf.append("fn addMagicNetwork() {\n");
        buf.append("  local magicpath = malloc(strlen(streamit_home) + 30);\n");
        buf.append("  sprintf(magicpath, \"%s%s\", streamit_home, \"/include/magic_net.bc\");\n");
        //include the number gathering code and install the device file
        buf.append("  include(magicpath);\n");  
        //add the function to catch the magic instructions
        buf.append("  addMagicFIFOs();\n");
        buf.append("  create_schedules();\n");
        buf.append("}\n");
    }

    /**
     * Create the function to tell the simulator what tiles are mapped, 
     * so should be counted in the statistics gathering.
     */
    private void mappedFunction() 
    {
        buf.append("fn mapped_tile(tileNumber) {\n");
        buf.append("if (");
        //generate the if statement with all the tile numbers of mapped tiles
        for (int i = 0; i < rawChip.getXSize(); i++) 
            for (int j = 0; j < rawChip.getYSize(); j++) {
                RawTile tile = rawChip.getTile(i, j);
                if (tile.isMapped()) {
                    int num = tile.getTileNumber();
                    buf.append("tileNumber == " + 
                               num);
                    buf.append(" ||\n");
                }
            }
        //append the 0 to end the conjuction
        buf.append("0) {return 1; }\n");
        buf.append("return 0;\n");
        buf.append("}\n");
    }

    /**
     * Create code to load all the schedules for the magic drams.
     *
     */
    private void magicDram() 
    {
        buf.append("{\n");
        //generate includes
        for (int i = 0; i < rawChip.getDevices().length; i++) {
            MagicDram dram = (MagicDram)rawChip.getDevices()[i];
            if (dram == null) 
                continue;
            if (dram.hasCode())
                buf.append("\tinclude(\"magicdram" + dram.getPort()+ ".bc\");\n");
        }
        //install devices
        for (int i = 0; i < rawChip.getDevices().length; i++) {
            MagicDram dram = (MagicDram)rawChip.getDevices()[i];
            if (dram == null) 
                continue;
            if (dram.hasCode())
                buf.append("\tdev_magic_dram" + dram.getPort() + "_init(" + dram.getPort() + ");\n");
        }
        buf.append("}\n");
    }
    

    /**
     * If we are in decoupled mode, generate bc code to install the print service
     * and measure the work of the tile.
     */
    private void decoupled() 
    {
        buf.append("global gStreamItFilterTiles = " + rawChip.computingTiles() + ";\n");
        buf.append("global gFilterNames;\n");
         
        buf.append("{\n");
        buf.append("  local workestpath = malloc(strlen(streamit_home) + 30);\n");
        buf.append("  gFilterNames = listi_new();\n");
        for (int i = 0; i < rawChip.getXSize() * rawChip.getYSize(); i++) {
            //just add some name, because multiple filter are on one tile
            buf.append("  listi_add(gFilterNames, \"" + i + "\");\n");
        }
        buf.append("  sprintf(workestpath, \"%s%s\", streamit_home, \"/include/work_est.bc\");\n");
        //include the number gathering code and install the device file
        buf.append("  include(workestpath);\n");
        // add print service to the south of the SE tile
        buf.append("  {\n");
        buf.append("    local str = malloc(256);\n");
        buf.append("    local result;\n");
        buf.append("    sprintf(str, \"/tmp/%s.log\", *int_EA(gArgv,0));\n");
        buf.append("    result = dev_work_est_init(\"/dev/null\", gXSize+gYSize);\n");
        buf.append("    if (result == 0)\n");
        buf.append("      exit(-1);\n");
        buf.append("  }\n");
        buf.append("}\n");
    }

    /**
     * Dump the stringbuffer to a file.
     *
     */
    private void writeFile() 
    {
        try {
            FileWriter fw = new FileWriter(BCFILE_NAME);
            
            fw.write(buf.toString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("System Error writing " + 
                               BCFILE_NAME);
        }
    }
}
