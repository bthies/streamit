package at.dms.kjc.spacetime;

import java.io.FileWriter;

public class GenerateComputeCode {
    private static RawChip rawChip;

    public static void run(RawChip chip) 
    {
	try {
	    rawChip = chip;
	    for (int x = 0; x < rawChip.getXSize(); x++)
		for (int y = 0; y < rawChip.getYSize(); y++) {
		    RawTile tile = rawChip.getTile(x, y);
		    if (tile.hasComputeCode()) {
			FileWriter fw = new FileWriter("tile" + tile.getTileNumber() + 
						       ".c");
			fw.write(new TraceIRtoC(tile).toString());
			fw.close();
		    }
		}
	}
	catch (Exception e) {
	    System.err.println("Serious error writing compute code.");
	    System.exit(-1);
	}
    }
}
