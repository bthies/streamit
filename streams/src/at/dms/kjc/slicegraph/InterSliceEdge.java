package at.dms.kjc.slicegraph;

import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.backendSupport.SchedulingPhase;

/**
 *  This class represents an edge in the partitioned stream graph between slices.
 *  But it actually connects {@link OutputSliceNode}s to {@link InputSliceNodes}.
 * 
 * @author mgordon
 *
 */
public class InterSliceEdge extends Edge {
    
    /**
     * Full constructor, (type will be inferred from src / dest).
     * @param src   Source of directed edge as OutputSliceNode
     * @param dest  Destination of directed edga as InputSliceNode
     */
    public InterSliceEdge(OutputSliceNode src, InputSliceNode dest) {
        super(src,dest);
    }

    /**
     * Partial constructor: {@link #setDest(InputSliceNode)} later.
     * @param src 
     */
    public InterSliceEdge(OutputSliceNode src) {
        super();
        this.src = src;
    }

    /**
     * Partial constructor: {@link #setSrc(OutputSliceNode)} later.
     * @param dest
     */
    public InterSliceEdge(InputSliceNode dest) {
        super();
        this.dest = dest;
    }

    @Override
    public OutputSliceNode getSrc() {
        return (OutputSliceNode)src;
    }

    @Override
    public InputSliceNode getDest() {
        return (InputSliceNode)dest;
    }

    @Override
    public void setSrc(SliceNode src) {
        assert src instanceof OutputSliceNode;
        super.setSrc(src);
    }

    @Override
    public void setDest(SliceNode dest) {
        assert dest instanceof InputSliceNode;
        super.setDest(dest);
    }
    /**
     * The number of items that traverse this edge in the initialization
     * stage.
     * 
     * @return The number of items that traverse this edge in the initialization
     * stage. 
     */
    public int initItems() {
        int itemsReceived, itemsSent;

        FilterInfo next = FilterInfo.getFilterInfo((FilterSliceNode) ((InputSliceNode)dest)
                                                   .getNext());
        
        itemsSent = (int) ((double) next.initItemsReceived() * ((InputSliceNode)dest).ratio(this));
        //System.out.println(next.initItemsReceived()  + " * " + ((InputSliceNode)dest).ratio(this));
        
        // calculate the items the output slice sends
        FilterInfo prev = FilterInfo.getFilterInfo((FilterSliceNode) ((OutputSliceNode)src)
                                                   .getPrevious());
        itemsReceived = (int) ((double) prev.initItemsSent() * ((OutputSliceNode)src).ratio(this));

        if (itemsSent != itemsReceived) {
            System.out.println("*** Init: Items received != Items Sent!");
            System.out.println(prev + " -> " + next);
            System.out.println("Mult: " + prev.getMult(SchedulingPhase.INIT) + " " +  
                    next.getMult(SchedulingPhase.INIT));
            System.out.println("Push: " + prev.prePush + " " + prev.push);
            System.out.println("Pop: " + next.pop);
            System.out.println("Init items Sent * Ratio: " + prev.initItemsSent() + " * " +
                    ((OutputSliceNode)src).ratio(this));
            System.out.println("Items Received: " + next.initItemsReceived(true));
            System.out.println("Ratio received: " + ((InputSliceNode)dest).ratio(this));
            
        }
        
        // see if they are different
        assert (itemsSent == itemsReceived) : "Calculating init stage: items received != items send on buffer: "
            + src + " (" + itemsSent + ") -> (" + itemsReceived + ") "+ dest;

        return itemsSent;
    }

    /**
     * @return The amount of items (not counting typesize) that flows 
     * over this edge in the steady state.
     */
    public int steadyItems() {
        int itemsReceived, itemsSent;

        // calculate the items the input slice receives
        FilterInfo next = FilterInfo.getFilterInfo(((InputSliceNode)dest).getNextFilter());
        itemsSent = (int) ((next.steadyMult * next.pop) * ((double) ((InputSliceNode)dest)
                                                           .getWeight(this) / ((InputSliceNode)dest).totalWeights()));

        // calculate the items the output slice sends
        FilterInfo prev = FilterInfo.getFilterInfo((FilterSliceNode) ((OutputSliceNode)src)
                                                   .getPrevious());
        itemsReceived = (int) ((prev.steadyMult * prev.push) * ((double) ((OutputSliceNode)src)
                                                                .getWeight(this) / ((OutputSliceNode)src).totalWeights()));

        assert (itemsSent == itemsReceived) : "Calculating steady state: items received != items sent on buffer "
            + itemsSent + " " + itemsReceived + " " + prev + " " + next;

        return itemsSent;
    }

   /**
    * The number of items sent over this link in one call of the link in the prime
    * pump stage, the link might be used many times in the prime pump stage conceptually 
    * using the rotating buffers.
    * 
    * @return ...
    */
    public int primePumpItems() {
        return (int) ((double) FilterInfo.getFilterInfo(((OutputSliceNode)src).getPrevFilter())
                      .totalItemsSent(SchedulingPhase.PRIMEPUMP) * ((OutputSliceNode)src).ratio(this));
    }

}
