package at.dms.kjc.slicegraph;

/**
 * Linked list of UnflatEdge. Useful data representation for
 * FlattenGraph. This was written before Java1.5 generics so feel free
 * to replace.
 * @author jasperln
 */
public class EdgeList {
    /**
     * UnflatEdge contained in this linked list node.
     */
    UnflatEdge val;
    /**
     * Next pointer for next linked list node.
     */
    EdgeList next;

    /**
     * Creates EdgeList node and connect to already created list.
     * @param val The UnflatEdge to store in this link list node
     * @param next The EdgeList to link to.
     */
    EdgeList(UnflatEdge val,EdgeList next) {
        this.val=val;
        this.next=next;
    }

    /**
     * Creates EdgeList.
     * @param val Initial UnflatEdge to store in list.
     */
    EdgeList(UnflatEdge val) {
        this.val=val;
        this.next=null;
    }
}
