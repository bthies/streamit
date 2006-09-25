package at.dms.kjc.spacedynamic;

import at.dms.kjc.common.RawSimulatorPrint;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Iterator;
import java.io.*;

public class MakefileGenerator {
    public static final String MAKEFILE_NAME = "Makefile.streamit";

    private static SpdStreamGraph streamGraph;

    private static Layout layout;

    private static RawChip rawChip;

    public static void createMakefile(SpdStreamGraph sg) {
        streamGraph = sg;
        layout = sg.getLayout();
        rawChip = sg.getRawChip();

        try {
            
            // create a set of all the tiles with code
            HashSet<ComputeNode> computeTiles = new HashSet<ComputeNode>();
            computeTiles.addAll(TileCode.realTiles);
            computeTiles.addAll(TileCode.tiles);

            // remove joiners from the hashset if we are in decoupled mode,
            // we do not want to simulate joiners
            if (KjcOptions.decoupled || IMEMEstimation.TESTING_IMEM)
                removeJoiners(computeTiles);

            Iterator<ComputeNode> tilesIterator = computeTiles.iterator();

            //generate the bC file
            BCFile.generate(streamGraph, computeTiles);
            
            FileWriter fw = new FileWriter(MAKEFILE_NAME);
            fw.write("#-*-Makefile-*-\n\n");
            /*
             * if (KjcOptions.outputs < 0 && ! (KjcOptions.numbers > 0))
             * fw.write("LIMIT = TRUE\n"); // need to define limit for SIMCYCLES
             * to matter
             */

            if (!IMEMEstimation.TESTING_IMEM && !KjcOptions.decoupled) {
                // old settings
                // fw.write("BTL-DEVICES += -dram_freq 100\n");
                // turn off hardware icaching for now
                // fw.write("ATTRIBUTES += HWIC\n");
                // add some other stuff
                // fw.write("MEMORY_LAYOUT=LEFT_RIGHT_SIDES\n");
                // fw.write("BTL-DEVICES += -enable_all_sides_for_dram -dram
                // lhs\n");

                fw.write("ATTRIBUTES = IMEM_EXTRA_LARGE\n");
                //fw.write("MEMORY_LAYOUT=FOUR_SIDES\n");
                //fw.write("BTL-DEVICES += -dram lhs -enable_all_sides_for_dram -dram ths -dram bhs \n");
            }

            if (KjcOptions.hwic) {
                fw.write("ATTRIBUTES += HWIC\n");
            }
            
            //magic instruction support for printing...
            fw.write("EXTRA_BTL_ARGS += -magic_instruction\n ");

            // fw.write("SIM-CYCLES = 500000\n\n");
            fw.write("\n");
            // if we are using the magic network, tell btl
            if (KjcOptions.magic_net)
                fw.write("EXTRA_BTL_ARGS += "
                         + "-magic_crossbar C1H1\n");
            fw.write("include $(TOPDIR)/Makefile.include\n\n");
            fw.write("RGCCFLAGS += -O3\n\n");
            fw.write("BTL-MACHINE-FILE = fileio.bc\n\n");
           
            if (rawChip.getYSize() > 4) {
                fw.write("TILE_PATTERN = 8x8\n\n");
            }
            // fix for snake boot race condition
            fw.write("MULTI_SNAKEBOOT = 0\n\n");

            fw.write("TILES = ");
            while (tilesIterator.hasNext()) {
                int tile = ((RawTile) tilesIterator.next()).getTileNumber();

                if (tile < 10)
                    fw.write("0" + tile + " ");
                else
                    fw.write(tile + " ");
            }

            fw.write("\n\n");

            tilesIterator = computeTiles.iterator();
            while (tilesIterator.hasNext()) {
                int tile = ((RawTile) tilesIterator.next()).getTileNumber();

                if (tile < 10)
                    fw.write("OBJECT_FILES_0");
                else
                    fw.write("OBJECT_FILES_");

                fw.write(tile + " = " + "tile" + tile + ".o ");

                // make sure that there is some
                if (!(KjcOptions.magic_net || KjcOptions.decoupled))
                    fw.write("sw" + tile + ".o");

                fw.write("\n");
            }

            // use sam's gcc and set the parameters of the tile
//            if (KjcOptions.altcodegen) {
                // fw.write
                // ("\nRGCC=/home/pkg/brave_new_linux/0225.btl.rawlib.starbuild/install/slgcc/bin/raw-gcc\n");
                fw
                    .write("\nRGCC=/home/pkg/brave_new_linux/0277.newlib.rgdb.starbuild/install/slgcc/bin/raw-gcc\n");
                fw.write("\nDMEM_PORTS  = 1\n");
                fw.write("ISSUE_WIDTH = 1\n\n");
                fw
                    .write("EXTRA_BTL_ARGS += -issue_width $(ISSUE_WIDTH) -dmem_ports $(DMEM_PORTS)\n");
                fw
                    .write("RGCCFLAGS += -missue_width=$(ISSUE_WIDTH) -mdmem_ports=$(DMEM_PORTS)\n");
//            }

            fw.write("\ninclude $(COMMONDIR)/Makefile.all\n\n");
            fw.write("clean:\n");
            fw.write("\trm -f *.o\n");
            fw.write("\trm -f tile*.s\n\n");
            fw.close();
        } catch (Exception e) {
            System.err.println("Error writing Makefile");
            e.printStackTrace();
        }
    }

    // remove all tiles mapped to joiners from the coordinate hashset *tiles*
    private static void removeJoiners(HashSet<ComputeNode> tiles) {
        Iterator<FlatNode> it = layout.getJoiners().iterator();
        while (it.hasNext()) {
            tiles.remove(layout.getTile(it.next()));
        }
    }


    private static int getIOPort(RawTile tile) {
        return rawChip.getXSize() + +tile.getY();
    }

}
