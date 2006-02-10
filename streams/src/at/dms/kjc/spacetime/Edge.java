package at.dms.kjc.spacetime;

import at.dms.kjc.*;

/**
 *  This class represents an edge in the partitioned stream graph between slices (traces).
 *  But it actually connectes <OutputTraceNodes> to <InputTraceNodes>.
 * 
 * @author mgordon
 *
 */
public class Edge {
    private OutputTraceNode src;

    private InputTraceNode dest;

    private CType type;

    public Edge(OutputTraceNode src, InputTraceNode dest) {
        assert src != null : "Source Null!";
        assert dest != null : "Dest Null!";
        this.src = src;
        this.dest = dest;
        type = null;
    }

    public Edge(OutputTraceNode src) {
        this.src = src;
    }

    public Edge(InputTraceNode dest) {
        this.dest = dest;
    }

    public CType getType() {
        if (type != null)
            return type;
        assert src.getPrevFilter().getFilter().getOutputType() == dest
            .getNextFilter().getFilter().getInputType() : "Error calculating type";
        type = src.getPrevFilter().getFilter().getOutputType();
        return type;
    }

    public OutputTraceNode getSrc() {
        return src;
    }

    public InputTraceNode getDest() {
        return dest;
    }

    public void setSrc(OutputTraceNode src) {
        this.src = src;
    }

    public void setDest(InputTraceNode dest) {
        this.dest = dest;
    }

    public String toString() {
        return src + "->" + dest + "(" + hashCode() + ")";
    }

    public int initItems() {
        int itemsReceived, itemsSent;

        // calculate the items the input trace receives
        FilterInfo next = FilterInfo.getFilterInfo((FilterTraceNode) dest
                                                   .getNext());
        itemsSent = (int) ((double) next.initItemsReceived() * dest.ratio(this));
        // calculate the items the output trace sends
        FilterInfo prev = FilterInfo.getFilterInfo((FilterTraceNode) src
                                                   .getPrevious());
        itemsReceived = (int) ((double) prev.initItemsSent() * src.ratio(this));

        /*
         * System.out.println(out); for (int i = 0; i < out.getWeights().length;
         * i++) { System.out.println(" ---- Weight = " + out.getWeights()[i]);
         * for (int j = 0; j < out.getDests()[i].length; j++)
         * System.out.println(out.getDests()[i][j] + " " +
         * out.getDests()[i][j].hashCode()); System.out.println(" ---- "); }
         * //System.out.println(out.getWeight(edge)+ " / " +
         * out.totalWeights());
         * //System.out.println(((double)out.getWeight(edge) /
         * out.totalWeights()));
         * 
         * System.out.println(in); //System.out.println(in.getWeights().length + " " +
         * in.getWeights()[0]); System.out.println("-------"); for (int i = 0; i <
         * in.getWeights().length; i++) { System.out.println(in.getSources()[i] + " " +
         * in.getWeights()[i] + " " + in.getSources()[i].hashCode()); }
         * System.out.println("-------");
         */
        // see if they are different
        assert (itemsSent == itemsReceived) : "Calculating steady state: items received != items send on buffer: "
            + src + " -> " + dest;

        return itemsSent;
    }

    public int steadyItems() {
        int itemsReceived, itemsSent;

        // calculate the items the input trace receives
        FilterInfo next = FilterInfo.getFilterInfo(dest.getNextFilter());
        itemsSent = (int) ((next.steadyMult * next.pop) * ((double) dest
                                                           .getWeight(this) / dest.totalWeights()));

        // calculate the items the output trace sends
        FilterInfo prev = FilterInfo.getFilterInfo((FilterTraceNode) src
                                                   .getPrevious());
        itemsReceived = (int) ((prev.steadyMult * prev.push) * ((double) src
                                                                .getWeight(this) / src.totalWeights()));

        assert (itemsSent == itemsReceived) : "Calculating steady state: items received != items send on buffer "
            + itemsSent + " " + itemsReceived + " " + prev + " " + next;

        return itemsSent;
    }

   /**
    * The number of items sent over this link in one call of the link in the prime
    * pump stage, the link might be used many times in the prime pump stage conceptually 
    * using the rotating buffers.
    * 
    * @return
    */
    public int primePumpItems() {
        return (int) ((double) FilterInfo.getFilterInfo(src.getPrevFilter())
                      .totalItemsSent(false, true) * src.ratio(this));
    }

}
