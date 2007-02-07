package at.dms.kjc.spacetime;

import at.dms.kjc.slicegraph.InterSliceEdge;

/**
 * This class encapsulates the port plus the distance of this port to the
 * source and dest of the edge in question (see assignmentOrder).  It 
 * implements comparable so that the TreeSet can sort it based on the
 * distance field.
 * 
 * @author mgordon
 *
 */
class PortDistance implements Comparable {
    public StreamingDram dram;
    private InterSliceEdge edge;
    
    private StreamingDram src;
    private StreamingDram dst;
    
    private int distance;
    
    // put this crap in so it sorts correctly with duplicate distances...
    private static int index;

    public int id;
    
    public PortDistance(InterSliceEdge edge, StreamingDram dram, 
            StreamingDram src, StreamingDram dst) {
        this.dram = dram;
        this.edge = edge;
        this.src = src;
        this.dst = dst;
        distance = computeDistance();
        id = index++;
    }

    /**
     * @return a distance metric for this dram from its src and dest.
     */
    private int computeDistance() {
        //if the src of this edge has only one output,
        //then weight the port already assigned to its output very low (good)
        if (edge.getSrc().oneOutput() && 
                src == dram)
            return 0;
        //if the des has only one input, then weight the port already assigned 
        //to its input very low (good)...
        if (edge.getDest().oneInput() && 
                dst == dram)
            return 0;
        
        //now compute the manhattan distance from the source and from the
        //dest
        int srcDist = Math.abs(src.getNeighboringTile().getX() - 
                dram.getNeighboringTile().getX()) + 
                Math.abs(src.getNeighboringTile().getY() - 
                        dram.getNeighboringTile().getY());
        int dstDist = Math.abs(dst.getNeighboringTile().getX() -
                dram.getNeighboringTile().getX()) +
                Math.abs(dst.getNeighboringTile().getY() - 
                        dram.getNeighboringTile().getY());
        //add one because we for the final hop of the route.
        return srcDist + dstDist + 1;
    }
    
    public boolean equals(PortDistance pd) {
        return (this.distance == pd.distance && dram == pd.dram);
    }

    public int compareTo(Object pd) {
        assert pd instanceof PortDistance;
        PortDistance portd = (PortDistance) pd;
        if (portd.distance == this.distance) {
            if (dram == portd.dram)
                return 0;
            if (id < portd.id)
                return -1;
            return 1;
        }
        if (this.distance < portd.distance)
            return -1;
        else
            return 1;
    }

}