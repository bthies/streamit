package at.dms.kjc.spacetime;

import java.lang.StringBuffer;
import at.dms.kjc.*;
import java.io.FileWriter;

public class BCFile 
{
    private RawChip rawChip;
    private StringBuffer buf;

    public static final String BCFILE_NAME = "setup.bc";

    public static void generate(RawChip rc) 
    {
	(new BCFile(rc)).doit();
    }
    
    public BCFile(RawChip rc) 
    {
	rawChip = rc;
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
	buf.append("global gStreamItTilesUsed = " + rawChip.computingTiles() + ";\n");
	buf.append("global gStreamItTiles = " + rawChip.getXSize() * rawChip.getYSize() +
		 ";\n");
	buf.append("global streamit_home = getenv(\"STREAMIT_HOME\");\n");      

	buf.append("fn quit_sim()\n{\n");
	buf.append("\tgInterrupted = 1;\n");
	buf.append("\texit_now(0);\n");
	buf.append("}\n");
	

	if (KjcOptions.decoupled)
	    decoupled();
	mappedFunction();

	if (KjcOptions.magicdram) 
	    magicDram();

	if (KjcOptions.numbers > 0) // && NumberGathering.successful)
	    numberGathering();
	if (KjcOptions.magic_net)
	    magicNet();
	flopsCount();
	setupFunction();

	writeFile();
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
    

    private void numberGathering() 
    {
	/*
	  buf.append("global printsPerSteady = " + NumberGathering.printsPerSteady + ";\n");
	  buf.append("global skipPrints = " + NumberGathering.skipPrints + ";\n");
	  buf.append("global quitAfter = " + KjcOptions.numbers + ";\n");
	  buf.append("global gSinkX = " + 
		   Layout.getTile(NumberGathering.sink).getColumn() +
		   ";\n");
	  buf.append("global gSinkY = " + 
		   Layout.getTile(NumberGathering.sink).getRow() +
		   ";\n");
	  
	  buf.append("{\n");
	  buf.append("  local numberpath = malloc(strlen(streamit_home) + 30);\n");
	  buf.append("  sprintf(numberpath, \"%s%s\", streamit_home, \"/include/gather_numbers.bc\");\n");
	  //include the number gathering code and install the device file
	  buf.append("  include(numberpath);\n");
	  //call the number gathering initialization function
	  buf.append("  gather_numbers_init();\n");
	  buf.append("}\n");
	*/
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
		if (tile.hasComputeCode()) {
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
	    buf.append("\tinclude(\"magicdram" + dram.getPort()+ ".bc\");\n");
	}
	//install devices
	for (int i = 0; i < rawChip.getDevices().length; i++) {
	    MagicDram dram = (MagicDram)rawChip.getDevices()[i];
	    if (dram == null) 
		continue;
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
