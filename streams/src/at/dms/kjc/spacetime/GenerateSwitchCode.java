package at.dms.kjc.spacetime;

import java.io.FileWriter;
import at.dms.kjc.*;

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
			if (KjcOptions.magicdram) 
			    fw.write(startIO(chip, tile));
			else {
			    //normal streaming dram operation
			    //just write out the instructions...
			    //write the initialization code
			    for (int i = 0; i < tile.getSwitchCode().commAddrSize(); i++) {
				fw.write("\t" + 
					 tile.getSwitchCode().getCommAddrIns(i).toString() + "\n");
			    }
			}
			
			fw.write("# End of Address Communication\n");
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
    
    private static String startIO(RawChip rawChip, RawTile tile) 
    {
	StringBuffer buf = new StringBuffer();
	if (tile.hasIODevice()) {
	    //move whatever is in $1 to the iodevice to get it started
	    buf.append("\tnop\troute $1->$c" + rawChip.getDirection(tile, tile.getIODevice()) + "o\n");
	}
	return buf.toString();
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
	//buf.append("\tmtsri	SW_PC, %lo(sw_begin)\n");
	buf.append("\tla $3, sw_begin\n");
	buf.append("\tmtsr SW_PC, $3\n");
	buf.append("\tmtsri	SW_FREEZE, 0\n");
	/*
		 if (FileVisitor.connectedToFR(tile))
		 buf.append("\tori! $0, $0, 1\n");
	*/

	buf.append("\tjr $31\n");
	return buf.toString();
    }
}	

