package at.dms.kjc.slicegraph;

/**
 * Doubly linked list of int[]. Useful data representation for
 * FlattenGraph. This was written before Java1.5 generics so feel free
 * to replace.
 * @author jasperln
 */
public class IntArrayList {
    /**
     * The int[] to store in this node.
     */
    int[] value;
    /**
     * The pointer to prev node.
     */
    IntList prev;
    /**
     * The pointer to next node.
     */
    IntList next;

    /**
     * Create IntArrayList and store value inside. Endpoints are
     * denoted with null.
     * @param value The int[] to store in this IntArrayList.
     * @param prev Pointer to previous IntList
     * @param next Pointer to next IntList
     */
    IntArrayList(int[] value,IntList prev,IntList next) {
        this.value=value;
        this.prev=prev;
        this.next=next;
    }
}
