package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.io.*;


public class MakefileGenerator 
{
    public static void createMakefile() 
    {
	try {
	    FileWriter fw = new FileWriter("Makefile");
	    Iterator tiles = Layout.tileIterator();
	    
	    fw.write("#Makefile\n\n");
	    fw.write("SIM-CYCLES = 500000\n\n");
	    fw.write("include /u/mgordon/raw/starsearch/Makefile.include\n\n");
	    fw.write("RGCCFLAGS += -O3\n\n");
	    fw.write("TILES = ");
	    while (tiles.hasNext()) {
		int tile = ((Integer)tiles.next()).intValue();
		
		if (tile < 10)
		    fw.write("0" + tile + " ");
		else 
		    fw.write(tile + " ");
	    }
	    
	    fw.write("\n\n");
	    
	    tiles = Layout.tileIterator();
	    while(tiles.hasNext()) {
		int tile = ((Integer)tiles.next()).intValue();
		if (tile < 10) 
		    fw.write("OBJECT_FILE_0");
		else
		    fw.write("OBJECT_FILE_");
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
	    }
    }
}

		
