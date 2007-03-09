package at.dms.kjc.spacetime;

import at.dms.kjc.spacetime.switchIR.SwitchIPort;
import at.dms.kjc.spacetime.switchIR.SwitchOPort;
import at.dms.util.Utils;

/**
 * The processing devices for RAW.  
 * This level deals with co-ordinates and I/O devices.
 * It has tiles and offchip devices and ports, etc.
 * See {@link RawChip} for data / routines dealing with {@link RawTile}s.
 * @author gordon refactored dimock
 *
 */
public class RawProcElements {

    protected IODevice[] devices;
    protected int gXSize;
    protected int gYSize;
    protected RawTile[][] tiles;

    /**
     * Data strucures for a <b>xSize</b> x <b>ySize</b> raw chip.
     *   
     * @param xSize
     * @param ySize
     */
    public RawProcElements(int xSize, int ySize) {
        super();
        // grid sizes
        gXSize = xSize;
        gYSize = ySize;
        // room for ComputeNode's, to be filled in by subclass
        tiles = new RawTile[xSize][ySize];
        // I/O devices around periphery of chip
        devices = new IODevice[2 * gXSize + 2 * gYSize];
    }

    public int getXSize() {
        return gXSize;
    }

    public int getYSize() {
        return gYSize;
    }

    public int getNumDev() {
        return devices.length;
    }

    public IODevice[] getDevices() {
        return devices;
    }

    /***************************************************************************
     * get the direction in X from src to dst, -1 for west 0 for same X, and 1
     * for east
     **************************************************************************/
    public int getXDir(RawComputeNode src, RawComputeNode dst) {
        if (dst.getX() - src.getX() < 0)
            return -1;
        if (dst.getX() - src.getX() > 0)
            return 1;
        return 0;
    }

    /**
     * get the direction in Y from src to dst, -1 for North 0 for same and 1 for
     * south.
     */
    public int getYDir(RawComputeNode src, RawComputeNode dst) {
        if (dst.getY() - src.getY() < 0)
            return -1;
        if (dst.getY() - src.getY() > 0)
            return 1;
        return 0;
    }

    /** 
     * @param n1
     * @param n2
     * @return The hops between <pre>n1</pre> and <pre>n2</pre>.
     */
    public int manhattanDistance(RawComputeNode n1, RawComputeNode n2) {
        //now compute the manhattan distance from the source and from the
        //dest
        int dist = Math.abs(n1.getX() - n2.getX()) + 
                Math.abs(n1.getY() - n2.getY());
    
        return dist;
    }

    /**
     * @param tile1
     * @param tile2
     * @return True if <pre>tile1</pre> neighbors <pre>tile2</pre>.
     */
    public boolean areNeighbors(RawComputeNode tile1, RawComputeNode tile2) {
        if (tile1 == tile2)
            return false;
        if (tile1.getY() == tile2.getY())
            if (Math.abs(tile1.getX() - tile2.getX()) == 1)
                return true;
        if (tile1.getX() == tile2.getX())
            if (Math.abs(tile1.getY() - tile2.getY()) == 1)
                return true;
        // not conntected
        return false;
    }

    public RawComputeNode getComputeNode(int x, int y) {
        assert !(x > gXSize || y > gYSize || x < -1 || y < -1) : "out of bounds in getComputeNode() of RawChip";
    
        if (x == gXSize || y == gYSize || x == -1 || y == -1) {
            if (y == -1)
                return devices[x];
            if (x == -1)
                return devices[(gYSize - 1) + (2 * gXSize + gYSize) - y];
            if (y == gYSize)
                return devices[(gXSize + 1) + (gXSize + gYSize) - x];
            if (x == gXSize)
                return devices[y + gXSize];
        }
        return tiles[x][y];
    }

    public String getDirection(RawComputeNode from, RawComputeNode to) {
        if (from == to)
            return "st";
    
        if (from.getX() == to.getX()) {
            int dir = from.getY() - to.getY();
            if (dir == -1)
                return "S";
            else if (dir == 1)
                return "N";
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        if (from.getY() == to.getY()) {
            int dir = from.getX() - to.getX();
            if (dir == -1)
                return "E";
            else if (dir == 1)
                return "W";
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        // System.out.println(from);
        // System.out.println(((MagicDram)to).getPort());
        // System.out.println("[" + from.getX() + ", " + from.getY() + "] -> ["
        // +
        // to.getX() + ", " + to.getY() + "]");
    
        Utils.fail("calling getDirection on non-neighbors");
        return "";
    }

    public SwitchOPort getOPort(RawComputeNode from, RawComputeNode to) {
        System.out.println("Get Out: " + from + " " + to);
        if (from == to)
            return SwitchOPort.CSTI;
        if (from.getX() == to.getX()) {
            int dir = from.getY() - to.getY();
            if (dir == -1)
                return SwitchOPort.S;
            else if (dir == 1)
                return SwitchOPort.N;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        if (from.getY() == to.getY()) {
            int dir = from.getX() - to.getX();
            if (dir == -1)
                return SwitchOPort.E;
            else if (dir == 1)
                return SwitchOPort.W;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        Utils.fail("calling getDirection on non-neighbors");
        return null;
    }

    public SwitchOPort getOPort2(RawComputeNode from, RawComputeNode to) {
        System.out.println("Get Out: " + from + " " + to);
        if (from == to)
            return SwitchOPort.CSTI2;
        if (from.getX() == to.getX()) {
            int dir = from.getY() - to.getY();
            if (dir == -1)
                return SwitchOPort.S2;
            else if (dir == 1)
                return SwitchOPort.N2;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        if (from.getY() == to.getY()) {
            int dir = from.getX() - to.getX();
            if (dir == -1)
                return SwitchOPort.E2;
            else if (dir == 1)
                return SwitchOPort.W2;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        Utils.fail("calling getDirection on non-neighbors");
        return null;
    }

    public SwitchIPort getIPort(RawComputeNode from, RawComputeNode to) {
        // System.out.println("Get In: "+from+" "+to);
        if (from == to)
            return SwitchIPort.CSTO;
        if (from.getX() == to.getX()) {
            int dir = from.getY() - to.getY();
            if (dir == -1)
                return SwitchIPort.N;
            else if (dir == 1)
                return SwitchIPort.S;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        if (from.getY() == to.getY()) {
            int dir = from.getX() - to.getX();
            if (dir == -1)
                return SwitchIPort.W;
            else if (dir == 1)
                return SwitchIPort.E;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        Utils.fail("calling getDirection on non-neighbors");
        return null;
    }

    public SwitchIPort getIPort2(RawComputeNode from, RawComputeNode to) {
        // System.out.println("Get In: "+from+" "+to);
        if (from == to)
            return SwitchIPort.CSTO;
        if (from.getX() == to.getX()) {
            int dir = from.getY() - to.getY();
            if (dir == -1)
                return SwitchIPort.N2;
            else if (dir == 1)
                return SwitchIPort.S2;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        if (from.getY() == to.getY()) {
            int dir = from.getX() - to.getX();
            if (dir == -1)
                return SwitchIPort.W2;
            else if (dir == 1)
                return SwitchIPort.E2;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        Utils.fail("calling getDirection on non-neighbors");
        return null;
    }

}