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
	fw.write("include(\"<dev/st_port_to_file.bc>\");\n");
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
	    fw.write("\tdev_st_port_to_file(\"" + sfw.getFileName() + "\", " + 
		     getIOPort(Layout.getTile(node)) + 
		     ");\n");
	}
	fw.write("\n}\n");
	fw.close();
    }
    
    private static int getIOPort(Coordinate tile) 
    {
	return StreamItOptions.rawColumns + StreamItOptions.rawRows + 
	    (StreamItOptions.rawColumns - 1) - tile.getColumn();
    }
    

}

		
