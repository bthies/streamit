package at.dms.kjc.spacetime;

import java.io.FileWriter;

public class GenerateSwitchCode {
    private static RawChip rawChip;
    
    public static void run(RawChip chip) 
    {
	try {
	    rawChip = chip;
	    for (int x = 0; x < rawChip.getXSize(); x++)
		for (int y = 0; y < rawChip.getYSize(); y++) {
		    RawTile tile = rawChip.getTile(x, y);
		    if (tile.hasSwitchCode()) {
			FileWriter fw = new FileWriter("sw" + tile.getTileNumber() + 
						       ".s");
			
			fw.write("#  Switch code\n");
			fw.write(getHeader());
			
			//write the initialization code
			for (int i = 0; i < tile.getSwitchCode().size(true); i++) {
			    fw.write("\t" + 
				     tile.getSwitchCode().getIns(i, true).toString() + "\n");
			}
			

			fw.write("sw_loop:\n");
			//write the steady state code
			for (int i = 0; i < tile.getSwitchCode().size(false); i++) {
			    fw.write("\t" + tile.getSwitchCode().getIns(i, false).toString()
				     + "\n");
			}

			fw.write("\tj\tsw_loop\n\n");
			fw.write(getTrailer());
			fw.close();
		    }
		}
	}
	catch (Exception e) {
	    e.printStackTrace();
	    
	    System.err.println("Serious error writing switch code.");
	    System.exit(-1);
	}
	
    }
    
    
    private static String getHeader() 
    {
	StringBuffer buf = new StringBuffer();
	
	buf.append("#include \"module_test.h\"\n\n");
	buf.append(".swtext\n");
	buf.append(".global sw_begin\n");
	buf.append(".global raw_init\n");
        buf.append(".global raw_init2\n\n");
	buf.append("sw_begin:\n");
	
	return buf.toString();
    }
    
    private static String getTrailer()
    {
	StringBuffer buf = new StringBuffer();
	
	buf.append(".text\n\n");
	buf.append("raw_init:\n");
	buf.append("\tmtsri	SW_PC, %lo(sw_begin)\n");
	buf.append("\tmtsri	SW_FREEZE, 0\n");
	/*
		 if (FileVisitor.connectedToFR(tile))
		 buf.append("\tori! $0, $0, 1\n");
	*/

	buf.append("\tjr $31\n");
	return buf.toString();
    }
}	

