package at.dms.kjc.spacetime;

import java.lang.StringBuffer;
import at.dms.kjc.*;
import java.io.FileWriter;
import at.dms.kjc.flatgraph2.*;

public class BCFile 
{
    private RawChip rawChip;
    private StringBuffer buf;
    private NumberGathering ng;
    private boolean numbers;

    public static final String BCFILE_NAME = "setup.bc";

    public static void generate(RawChip rc, NumberGathering gn) 
    {
	(new BCFile(rc, gn)).doit();
    }
    
    public BCFile(RawChip rc, NumberGathering g) 
    {
	rawChip = rc;
	ng = g;
	//set numbers to true if we are given a number gathering object
	numbers = (ng != null);
    }
    
    public void doit() 
    {
	buf = new StringBuffer();
	
	if (KjcOptions.magic_net) 
	    buf.append("gTurnOffNativeCompilation = 1;\n");

	buf.append("include(\"<dev/basic.bc>\");\n");

	//workaround for magic instruction support...
	buf.append("include(\"<dev/magic_instruction.bc>\");\n");
	
	//let the simulation know how many tiles are mapped to 
	//filters or joiners
	buf.append("global gMHz = 250;\n");
	buf.append("global gStreamItTilesUsed = " + rawChip.computingTiles() + ";\n");
	buf.append("global gStreamItTiles = " + rawChip.getXSize() * rawChip.getYSize() +
		 ";\n");
	buf.append("global streamit_home = getenv(\"STREAMIT_HOME\");\n\n");      

	buf.append("global to_file_path = malloc(strlen(streamit_home) + 30);\n"); 
	buf.append("global from_file_path = malloc(strlen(streamit_home) + 30);\n\n"); 

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
	
	files();

	if (KjcOptions.decoupled)
	    decoupled();
	mappedFunction();

	if (KjcOptions.magicdram) 
	    magicDram();

	if (KjcOptions.magic_net)
	    magicNet();
	flopsCount();
	setupFunction();

	writeFile();
    }
    
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
	buf.append("global gMappedTiles = " + mappedTiles + ";\n");
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
				       "\", slavePort" + dram.getPort() + 
				       (dram.getFileReader().isFP() ? ", 1" : ", 0") +
				       ");\n");
			}
			if (dram.isFileWriter()) {
			    //if numbers call the ng file writer
			    if (numbers) 
				buf.append("  dev_NG_to_file(" + 
					   ng.getID((FileOutputContent)dram.getFileWriter().getContent()) + 
					   ", \"");
			    else 
				buf.append("  dev_to_file(\"");
			    
			    buf.append(dram.getFileWriter().getFileName() +
				       "\", slavePort" + dram.getPort() + 
				       (dram.getFileWriter().isFP() ? ", 1" : ", 0") +
				       ");\n");
			}
		    }
		}
	    }
	}
	buf.append("\n}\n");
    }
    
    
    //create the function that sets everything up in the simulator
    private void setupFunction() 
    {
	buf.append("\n{\n");	
	
	if (KjcOptions.magic_net)
	    buf.append("  addMagicNetwork();\n");

	buf.append("\n}\n");
    }

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
             "  printf(\"// **** count_FLOPS: %4d FLOPS, %4d mFLOPS\n\",\n" +
             "         gAUTOFLOPS, (250*gAUTOFLOPS)/steps);\n" +
             "}\n" +
             "\n");	
    }
    

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

    //create the function to tell the simulator what tiles are mapped
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
