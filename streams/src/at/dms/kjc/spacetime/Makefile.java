package at.dms.kjc.spacetime;

import java.lang.StringBuffer;
import at.dms.kjc.*;
import java.io.FileWriter;

public class Makefile 
{
    public static final String MAKEFILE_NAME = "Makefile.streamit";

    private RawChip rawChip;
    private StringBuffer buf;

    public static void generate(RawChip rc) 
    {
	(new Makefile(rc)).doit();
    }
    
    public Makefile(RawChip rc) 
    {
	rawChip = rc;
    }
    
    public void doit() 
    {
	buf = new StringBuffer();
	
	makeHeader();
	makeTiles();
	makeTrailer();
	writeFile();
    }
    
    private void makeTiles() 
    {
	//let the make file know what tiles we are using
	buf.append("TILES = ");
	for (int i = 0; i < rawChip.getXSize(); i++) 
	    for (int j = 0; j < rawChip.getYSize(); j++) {
		RawTile tile = rawChip.getTile(i, j);
		
		if (tile.hasComputeCode() || 
		    tile.hasSwitchCode()) {
		    int num = tile.getTileNumber();
		    
		    if (num < 10)
			buf.append("0" + num + " ");
		    else
			buf.append(num + " ");
		}
	    }
	
	buf.append("\n\n");
	
	//now let the makefile know the file names
	//of the compute and switch code
	for (int i = 0; i < rawChip.getXSize(); i++) 
	    for (int j = 0; j < rawChip.getYSize(); j++) {
		RawTile tile = rawChip.getTile(i, j);
		int num = tile.getTileNumber();

		if (tile.hasComputeCode() || 
		    tile.hasSwitchCode()) {
		    if (num < 10)
			buf.append("OBJECT_FILES_0");
		    else
			buf.append("OBJECT_FILES_");
		    buf.append(num + " = ");
		    
		    if (tile.hasComputeCode())
			buf.append("tile" + num + ".o ");
		    if (tile.hasSwitchCode() && 
			!KjcOptions.magic_net && !KjcOptions.decoupled)
			buf.append("sw" + num + ".o");
		    buf.append("\n");
		}
	    }	    
    }

    private void makeTrailer() 
    {
	 //use sam's gcc and set the parameters of the tile
	if (KjcOptions.altcodegen) {
	    //	    buf.append
	    //	("\nRGCC=/home/bits7/NO_BACKUP/streamit/install/slgcc/bin/raw-gcc\n");
	    buf.append
		("\nRGCC=/home/pkg/brave_new_linux/0196.slgcc/install/slgcc/bin/raw-gcc\n");
	    buf.append("\nDMEM_PORTS  = 1\n");
	    buf.append("ISSUE_WIDTH = 1\n\n");
	    buf.append("EXTRA_BTL_ARGS += -issue_width $(ISSUE_WIDTH) -dmem_ports $(DMEM_PORTS)\n");
	    buf.append("RGCCFLAGS += -missue_width=$(ISSUE_WIDTH) -mdmem_ports=$(DMEM_PORTS)\n");
	}
	
	buf.append("\ninclude $(COMMONDIR)/Makefile.all\n");
	//add the drams to all sides
	if (!KjcOptions.magicdram) 
	    buf.append("BTL-ARGS += -dram lhs -enable_all_sides_for_dram -dram ths\n\n");
	
	buf.append("clean:\n");
	buf.append("\trm -f *.o\n");
	buf.append("\trm -f tile*.s\n\n");
    }
    

    private void makeHeader() 
    {
	buf.append("#-*-Makefile-*-\n\n");
	buf.append("ATTRIBUTES = IMEM_LARGE\n");
	//I don't think we can use the print service any more...
	//	buf.append("ATTRIBUTES += USES_PRINT_SERVICE\n")
;
	/* when I implement number gathering this will have to change
	// need to define limit for SIMCYCLES to matter
	if (!(KjcOptions.numbers > 0 && NumberGathering.successful))
	  buf.append("LIMIT = TRUE\n"); 
	//if we are generating number gathering code, 
	//we do not want to use the default print service...
	if (KjcOptions.numbers > 0 && NumberGathering.successful ||
	KjcOptions.decoupled) {
	buf.append("ATTRIBUTES += NO_PRINT_SERVICE\n");
	    buf.append("EXTRA_BTL_ARGS += -magic_instruction\n ");
	}
	*/
	if (!KjcOptions.magicdram)
	    buf.append("MEMORY_LAYOUT=FOUR_SIDES\n");
	
	buf.append("SIM-CYCLES = 500000\n\n");
	//if we are using the magic network, tell btl
	if (KjcOptions.magic_net)
	    buf.append("EXTRA_BTL_ARGS += " +
		       "-magic_instruction -magic_crossbar C1H1\n");
	
	buf.append("include $(TOPDIR)/Makefile.include\n\n");
	buf.append("RGCCFLAGS += -O3\n\n");
	buf.append("BTL-MACHINE-FILE = " + BCFile.BCFILE_NAME + "\n\n");
	
	if (rawChip.getXSize() > 4 || rawChip.getYSize() > 4)
	    buf.append("TILE_PATTERN = 8x8\n\n");
	
	//fix for snake boot race condition
	buf.append("MULTI_SNAKEBOOT = 0\n\n");
    }
    

    private void writeFile() 
    {
	try {
	    FileWriter fw = new FileWriter(MAKEFILE_NAME);
	    
	    fw.write(buf.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("System Error writing " + 
			       MAKEFILE_NAME);
	}
    }
}
