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
	    
	    fw.write("#Makefile\n\n");
	    fw.write("LIMIT = TRUE\n"); // need to define limit for SIMCYCLES to matter
	    fw.write("SIM-CYCLES = 500000\n\n");
	    fw.write("include $(TOPDIR)/Makefile.include\n\n");
	    fw.write("RGCCFLAGS += -O3\n\n");
	    if (FileVisitor.foundReader || FileVisitor.foundWriter) {
		fw.write("BTL-MACHINE-FILE = fileio.bc\n\n");
		createBCFile();
	    }
	    if (StreamItOptions.rawRows > 4)
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
	    fw.close();
	}
	catch (Exception e) 
	    {
		System.err.println("Error writing Makefile");
		e.printStackTrace();
	    }
    }
    
    private static void createBCFile() throws Exception 
    {
	FileWriter fw = new FileWriter("fileio.bc");
	
	fw.write("include(\"<dev/basic.bc>\");\n");
	
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

	fw.write("\n{\n");

	//generate the code for the fileReaders
	Iterator frs = FileVisitor.fileReaders.iterator();
	while (frs.hasNext()) {
	    FlatNode node = (FlatNode)frs.next();
	    SIRFileReader fr = (SIRFileReader)node.contents;
	    fw.write("\tdev_serial_rom_init(\"" + fr.getFileName() + "\", " + 
		     getIOPort(Layout.getTile(node)) + 
		     ", 1);\n");
	}
	//generate the code for the file writers
	Iterator fws = FileVisitor.fileWriters.iterator();
	while (fws.hasNext()) {
	    FlatNode node = (FlatNode)fws.next();
	    SIRFileWriter sfw = (SIRFileWriter)node.contents;
	    int size = getTypeSize(((SIRFileWriter)node.contents).getInputType());
	    fw.write("\tdev_st_port_to_file_size(\"" + sfw.getFileName() + "\", " + 
		     size + ", " + getIOPort(Layout.getTile(node)) + 
		     ");\n");
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
	return StreamItOptions.rawColumns + 
	    + tile.getRow();
    }
    

}

		
