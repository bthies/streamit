package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Iterator;
import java.io.*;


public class MakefileGenerator 
{
    public static final String MAKEFILE_NAME = "Makefile.streamit";
    public static void createMakefile() 
    {
	try {
	    //FileWriter fw = new FileWriter("Makefile");
	    FileWriter fw = new FileWriter(MAKEFILE_NAME);
	    //create a set of all the tiles with code
	    HashSet tiles = new HashSet();
	    tiles.addAll(TileCode.realTiles);
	    tiles.addAll(TileCode.tiles);
	    
	    //remove the tiles assigned to FileReaders
	    //do not generate switchcode for Tiles assigned to file readers
	    //they are just dummy tiles
	    Iterator frs = FileVisitor.fileNodes.iterator();
	    while (frs.hasNext()) {
		tiles.remove(Layout.getTile((FlatNode)frs.next()));
	    }

	    Iterator tilesIterator = tiles.iterator();
	    
	    fw.write("#-*-Makefile-*-\n\n");
	    fw.write("LIMIT = TRUE\n"); // need to define limit for SIMCYCLES to matter
            fw.write("ATTRIBUTES = IMEM_LARGE\n");
	    fw.write("SIM-CYCLES = 500000\n\n");
	    fw.write("include $(TOPDIR)/Makefile.include\n\n");
	    fw.write("RGCCFLAGS += -O3\n\n");
            fw.write("BTL-MACHINE-FILE = fileio.bc\n\n");
	    if (FileVisitor.foundReader || FileVisitor.foundWriter)
		createBCFile(true);
            else
                createBCFile(false);
	    if (RawBackend.rawRows > 4)
		fw.write("TILE_PATTERN=8x8\n\n");
	    fw.write("TILES = ");
	    while (tilesIterator.hasNext()) {
		int tile = 
		    Layout.getTileNumber((Coordinate)tilesIterator.next());
		
		if (tile < 10)
		    fw.write("0" + tile + " ");
		else 
		    fw.write(tile + " ");
	    }
	    
	    fw.write("\n\n");
	    
	    tilesIterator = tiles.iterator();
	    while(tilesIterator.hasNext()) {
		int tile = 
		    Layout.getTileNumber((Coordinate)tilesIterator.next());
		
		if (tile < 10) 
		    fw.write("OBJECT_FILES_0");
		else
		    fw.write("OBJECT_FILES_");
		fw.write(tile + " = " +
			 "tile" + tile + ".o " +
			 "sw" + tile + ".o\n");
	    }
	    
	    fw.write("\ninclude $(COMMONDIR)/Makefile.all\n\n");
	    fw.write("clean:\n");
	    fw.write("\trm -f *.o\n\n");
	    if(KjcOptions.sketchycodegen) {
		fw.write("%.s:  %.c\n");
		fw.write("\t$(RGCC) $(RGCCFLAGS) $(INCLUDES) $(DEFS) -S $< -o $@\n");
		fw.write("\tgrep -v router_mem $@ > $@.new\n");
		fw.write("\tmv $@.new $@");
	    }
	    fw.close();
	}
	catch (Exception e) 
	    {
		System.err.println("Error writing Makefile");
		e.printStackTrace();
	    }
    }
    
    private static void createBCFile(boolean hasIO) throws Exception 
    {
	FileWriter fw = new FileWriter("fileio.bc");
	
	//number gathering code
	if (KjcOptions.numbers > 0 && NumberGathering.successful) {
	    fw.write("global printsPerSteady = " + NumberGathering.printsPerSteady + ";\n");
	    fw.write("global skipPrints = " + NumberGathering.skipPrints + ";\n");
	    fw.write("global quitAfter = " + KjcOptions.numbers + ";\n\n");
	    
	    fw.write("global streamit_home = getenv(\"STREAMIT_HOME\");\n");	
	    fw.write("global path = malloc(strlen(streamit_home) + 30);\n");
	    fw.write("sprintf(path, \"%s%s\", streamit_home, \"include/basic_mod.bc\");\n");
	    //include the modified basic.bc file
	    fw.write("include(path);\n");
	}
	else
	    fw.write("include(\"<dev/basic.bc>\");\n");
	
	fw.write(
		 "fn dev_quick_print_init(ioPort, portInfo, toFile, fileName)\n" +
		 "{\n" +
		 "	//ioPort the port to listen on\n" +
		 "	//portInfo whether to print extended info such as whom it is\n" +
		 "	//	from\n" +
		 "	//toFile a 1 if we are printing to a file 0 if we are printing\n" +
		 "	//	to stdout\n" +
		 "	//fileName the name of the file if we are printing to a file\n" +
		 "	local result;\n" +
		 "	local fp0;\n" +
		 "	local devQuickPrintStruct;\n" +
		 "	fp0 = 0;\n" +
		 "	devQuickPrintStruct = hms_new();\n" +
		 "	// fopen the files\n" +
		 "	if(toFile)\n" +
		 "	{\n" +
		 "		fp0 = fopen(fileName, \"w\");\n" +
		 "	}\n" +
		 "	else\n" +
		 "	{\n" +
		 "		fp0 = stdout;\n" +
		 "	}\n" +
		 "	devQuickPrintStruct.fileHandle = fp0;\n" +
		 "	devQuickPrintStruct.ioPort = ioPort;\n" +
		 "	devQuickPrintStruct.portInfo = portInfo;\n" +
		 "	result = SimAddDevice(\"Quick Print Device\", \"dev_quick_print_reset\", \"dev_quick_print_calc\", devQuickPrintStruct);\n" +
		 "	if(result == 0)\n" +
		 "	{\n" +
		 "		printf(\"// **** dev_quick_print: error adding device\\n\");\n" +
		 "		return 0;\n" +
		 "	}\n" +
		 "	return 1;\n" +
		 "}\n" +
		 "\n" +
		 "fn dev_quick_print_reset(devQuickPrintStruct)\n" +
		 "{\n" +
		 "	//this is currently just a stub.  In the future it \n" +
		 "	//might reinitialize the file streams.\n" +
		 "	return 1;\n" +
		 "}\n" +
		 "\n" +
		 "fn dev_quick_print_calc(devQuickPrintStruct)\n" +
		 "{\n" +
		 "	local readHeaderValue;\n" +
		 "	local readDataValue;\n" +
		 "	local bogus;\n" +
		 "	local requestLength;\n" +
		 "	local opcode;\n" +
		 "	local sender_y;\n" +
		 "	local sender_x;\n" +
		 "	local our_y;\n" +
		 "	local our_x;\n" +
		 "	local time_hi;\n" +
		 "	local time_lo;\n" +
		 "	while(1)\n" +
		 "	{\n" +
		 "		//read header word\n" +
		 "		readHeaderValue = threaded_general_io_receive(machine, devQuickPrintStruct.ioPort);\n" +
		 "		Proc_GetCycleCounter(Machine_GetProc(machine, 0), &time_hi, &time_lo);\n" +
		 "		yield;\n" +
		 "		DecodeDynHdr(readHeaderValue, &bogus, &requestLength, &opcode, &sender_y, &sender_x, &our_y, &our_x);\n" +
		 "		//read Data word\n" +
		 "		readDataValue = threaded_general_io_receive(machine, devQuickPrintStruct.ioPort);\n" +
		 "		yield;\n" +
		 "		//fprintf it to the file\n" +
		 "		if(devQuickPrintStruct.portInfo)\n" +
		 "		{\n" +
		 "			fprintf(devQuickPrintStruct.fileHandle, \"Recv. from tile X = %d Y = %d \", sender_x, sender_y);\n" +
		 "			if(opcode == 0)\n" +
		 "			{\n" +
		 "				fprintf(devQuickPrintStruct.fileHandle, \"in float format: \");\n" +
		 "			}\n" +
		 "			else\n" +
		 "			{\n" +
		 "				fprintf(devQuickPrintStruct.fileHandle, \"in int format: \");\n" +
		 "			}\n" +
		 "		}\n" +
		 "		fprintf(devQuickPrintStruct.fileHandle, \"[%x%x: %x%08x]: \", sender_y, sender_x, time_hi, time_lo);\n" +
		 "		//opcode zero is for float\n" +
		 "		if(opcode == 0)\n" +
		 "		{\n" +
		 "			fprintf(devQuickPrintStruct.fileHandle, \"%e\\n\", double(readDataValue));\n" +
		 "		}\n" +
		 "		else\n" +
		 "		{\n" +
		 "			fprintf(devQuickPrintStruct.fileHandle, \"%d\\n\", readDataValue);\n" +
		 "		}\n" +
		 "	}\n" +
		 "}\n");
	
	
	fw.write
            ("global gAUTOFLOPS = 0;\n" +
             "fn __clock_handler(hms)\n" +
             "{\n" +
             "  local i;\n" +
             "  for (i = 0; i < gNumProc; i++)\n" +
             "  {\n" +
             "    gAUTOFLOPS += imem_instr_is_fpu(get_imem_instr(i, get_pc_for_proc(i)));\n" +
             "  }\n" +
             "}\n" +
             "\n" +
             "EventManager_RegisterHandler(\"clock\", \"__clock_handler\");\n" +
             "\n" +
             "fn count_FLOPS(steps)\n" +
             "{\n" +
             "  gAUTOFLOPS = 0;\n" +
             "  step(steps);\n" +
             "  printf(\"// **** count_FLOPS: %4d FLOPS, %4d mFLOPS\n\",\n" +
             "         gAUTOFLOPS, (250*gAUTOFLOPS)/steps);\n" +
             "}\n" +
             "\n");
	
        if (hasIO){
	    // create preamble
	    fw.write("if (FindFunctionInSymbolHash(gSymbolTable, \"dev_data_transmitter_init\",3) == NULL)\n");
	    fw.write("include(\"<dev/data_transmitter.bc>\");\n\n");
	    
            // create the instrumentation function
            fw.write("// instrumentation code\n");
            fw.write("fn streamit_instrument(val){\n");
            fw.write("  local a;\n"); 
            fw.write("  local b;\n");
            fw.write("  Proc_GetCycleCounter(Machine_GetProc(machine,0), &a, &b);\n");
            fw.write("  //printf(\"cycleHi %X, cycleLo %X\\n\", a, b);\n");
            // use the same format string that generating a printf causes so we can use
            // the same results script;
            fw.write("  printf(\"[00: %08x%08x]: %d\\n\", a, b, val);\n");
            fw.write("}\n\n");
	    

            //create the function to write the data
            fw.write("fn dev_st_port_to_file_size(filename, size, port)\n{\n");
            fw.write("local receive_device_descriptor = hms_new();\n");
            fw.write("// open the file\n  ;");
            fw.write("receive_device_descriptor.fileName = filename;\n  ");
            fw.write("receive_device_descriptor.theFile = fopen(receive_device_descriptor.fileName,\"w\");\n");
            fw.write("verify(receive_device_descriptor.theFile != NULL, \"### Failed to open output file\");\n");
            fw.write("receive_device_descriptor.calc =\n");
            fw.write("& fn(this)\n  {\n");
            fw.write("local theFile = this.theFile;\n");
            fw.write("while (1)\n {\n");
            fw.write("     local value = this.receive();\n");
            fw.write("     fwrite(&value, size, 1, theFile);\n");
            fw.write("     streamit_instrument(value);\n");
            fw.write("     fflush(theFile);\n");
            fw.write("}\n");
            fw.write("};\n");
            fw.write("return dev_data_transmitter_init(\"st_port_to_file\", port,0,receive_device_descriptor);\n");
            fw.write("}");
	}
	//
	fw.write("\n{\n");	
	fw.write("dev_quick_print_init(11, 0, 0, 0);\n");
		
	if (hasIO) {
	    //generate the code for the fileReaders
	    Iterator frs = FileVisitor.fileReaders.iterator();
	    while (frs.hasNext()) {
		FlatNode node = (FlatNode)frs.next();
		SIRFileReader fr = (SIRFileReader)node.contents;
		fw.write("\tdev_serial_rom_init(\"" + fr.getFileName() +
			 "\", " + getIOPort(Layout.getTile(node)) + 
			 ", 1);\n");
	    }
	    //generate the code for the file writers
	    Iterator fws = FileVisitor.fileWriters.iterator();
	    while (fws.hasNext()) {
		FlatNode node = (FlatNode)fws.next();
		SIRFileWriter sfw = (SIRFileWriter)node.contents;
		int size = getTypeSize(((SIRFileWriter)node.contents).getInputType());
		fw.write("\tdev_st_port_to_file_size(\"" + sfw.getFileName() +
			 "\", " + size + ", " +
			 getIOPort(Layout.getTile(node)) + ");\n");
            }
	}
	
	fw.write("\n}\n");
	fw.close();
    }

    
    private static int getTypeSize(CType type) {
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
    else
	{
	       Utils.fail("Cannot write type to file: " + type);
	}
    return 0;
}

private static int getIOPort(Coordinate tile) 
{
    return RawBackend.rawColumns + 
	+ tile.getRow();
}


}

		
