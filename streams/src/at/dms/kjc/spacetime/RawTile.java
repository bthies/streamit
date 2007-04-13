package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.kjc.slicegraph.*;
import at.dms.util.Utils;
import java.util.*;

/**
 * This class represents a raw tile on the raw chip with its compute
 * processor and its switch processor and the code that runs on each. 
 *
 * @author mgordon
 *
 */
public class RawTile extends RawComputeNode {
    /** true if this tile has switch code */
    private boolean switches;

    /** true if this tile has compute code */
    private boolean computes;

    // true if a filter has been mapped to it
    private boolean mapped;

    private SwitchCodeStore switchCode;

    private StreamingDram dram;

    private IODevice[] IODevices;

    private HashSet<OffChipBuffer> offChipBuffers;

    /** seems to only be used in {@link LayoutDot} */
    private LinkedList<SliceNode> initFilters;
    private LinkedList<SliceNode> primepumpFilters;
    private LinkedList<SliceNode> steadyFilters;

    public RawTile(int x, int y, RawChip rawChip) {
        super(rawChip);
        X = x;
        Y = y;
        setTileNumber();
        computes = false;
        mapped = false;
        switches = false;
        switchCode = new SwitchCodeStore(this);
        computeCode = new RawComputeCodeStore(this);
        IODevices = new IODevice[0];
        offChipBuffers = new HashSet<OffChipBuffer>();
        initFilters = new LinkedList<SliceNode>();
        primepumpFilters = new LinkedList<SliceNode>();
        steadyFilters = new LinkedList<SliceNode>();

    }

    public String toString() {
        return "Tile[" + X + "," + Y + "]";
    }

    public boolean hasIODevice() {
        return (IODevices.length >= 1);
    }

    public void addIODevice(IODevice io) {
        assert IODevices.length < 2 : "Trying to add too many neighboring IO devices";
        IODevice[] newIOs = new IODevice[IODevices.length + 1];
        for (int i = 0; i < IODevices.length; i++)
            newIOs[i] = IODevices[i];
        newIOs[newIOs.length - 1] = io;
        IODevices = newIOs;
    }

    public void addBuffer(OffChipBuffer buf) {
        offChipBuffers.add(buf);
    }

    public HashSet<OffChipBuffer> getBuffers() {
        return offChipBuffers;
    }

    public StreamingDram getDRAM() {
        return dram;
    }

    public void setDRAM(StreamingDram sd) {
        dram = sd;
    }

    public boolean isAttached(IODevice dev) {
        for (int i = 0; i < IODevices.length; i++)
            if (dev == IODevices[i])
                return true;
        return false;
    }

    public int getIOIndex(IODevice dev) {
        for (int i = 0; i < IODevices.length; i++)
            if (dev == IODevices[i])
                return i;
        assert false : "IODevice not attached to tile";
        return -1;
    }

    public IODevice[] getIODevices() {
        return IODevices;
    }

    public IODevice getIODevice() {
        assert IODevices.length == 1 : "cannot use getIODevice()";
        return IODevices[0];
    }

    private void setTileNumber() {
         setUniqueId((Y * rawChip.getXSize()) + X);
        /*
         * //because the simulator only simulates 4x4 or 8x8 we //have to
         * translate the tile number according to these layouts int columns = 4;
         * if (rawChip.getYSize() > 4 || rawChip.getXSize() > 4) columns = 8;
         * tileNumber = (Y * columns) + X;
         */
    }

    public int getTileNumber() {
        return getUniqueId();
    }

    public boolean hasComputeCode() {
        return computes;
    }

    public boolean hasSwitchCode() {
        return switches;
    }

    public SwitchCodeStore getSwitchCode() {
        return switchCode;
    }

    // this is set by SwitchCodeStore
    public void setSwitches() {
        switches = true;
    }

    public void setMapped() {
        mapped = true;
        setComputes();
    }

    public boolean isMapped() {
        return mapped;
    }

    // this is set by ComputeCodeStore
    public void setComputes() {
        computes = true;
    }

    public void printDram() {
        if (dram != null)
            System.out.print("Tile: " + getTileNumber() + " -> port: "
                             + dram.getPort() + " ");
        else
            System.out.print("Tile: " + getTileNumber() + " -> null ");
        if (IODevices.length > 0) {
            System.out.print("(neighbors: ");
            for (int i = 0; i < IODevices.length; i++) {
                System.out.print("port " + IODevices[i].getPort());
                if (i < IODevices.length - 1)
                    System.out.print(", ");
            }
            System.out.print(")");
        }
        System.out.println();
    }

    public static void printDramSetup(RawChip chip) {
        System.out.println("Memory Mapping:");
//        if (!KjcOptions.magicdram)
            for (int x = 0; x < chip.getXSize(); x++)
                for (int y = 0; y < chip.getYSize(); y++)
                    chip.getTile(x, y).printDram();
    }

    public Vector<RawTile> getNeighborTiles() {
        Vector<RawTile> ret = new Vector<RawTile>();
        if (X - 1 >= 0)
            ret.add(rawChip.getTile(X - 1, Y));
        if (X + 1 < rawChip.getXSize())
            ret.add(rawChip.getTile(X + 1, Y));
        if (Y - 1 >= 0)
            ret.add(rawChip.getTile(X, Y - 1));
        if (Y + 1 < rawChip.getYSize())
            ret.add(rawChip.getTile(X, Y + 1));
        return ret;
    }
    
  /**
     * Add a SliceNode to the the filters associated with this node.
     * 
     * @param init
     *            true if init stage, false if primepump or steady stage
     * @param primepump
     *            true if primepump stage, false if init or steady stage
     * @param node
     *            SliceNode to add
     */
    public void addSliceNode(boolean init, boolean primepump, SliceNode node) {
        if (init)
            initFilters.add(node);
        else if (primepump)
            primepumpFilters.add(node);
        else
            steadyFilters.add(node);
    }

    /**
     * Get the SliceNode's associated with this ComputeNode.
     * 
     * @param init
     *            true if init stage, false if primepump or steady stage
     * @param primepump
     *            true if primepump stage, false if init or steady stage
     * @return the FilterSliceNodes added by addFilterSlice for the passed
     *         values of init and primepump
     */
    public Iterable<SliceNode> getSliceNodes(boolean init, boolean primepump) {
        if (init)
            return initFilters;
        else if (primepump)
            return primepumpFilters;
        else
            return steadyFilters;
    }

}