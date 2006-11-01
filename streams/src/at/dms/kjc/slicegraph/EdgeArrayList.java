package at.dms.kjc.slicegraph;

/**
 * Linked list of UnflatEdge[]. Useful data representation for
 * FlattenGraph. This was written before Java1.5 generics so feel free
 * to replace.
 * @author jasperln
 */
public class EdgeArrayList {
    /**
     * UnflatEdge[] contained in this linked list node.
     */
    UnflatEdge[] val;
    /**
     * Next pointer for next linked list node.
     */
    EdgeArrayList next;

    /**
     * Creates EdgeArrayList node and connect to already created list.
     * @param val The UnflatEdge[] to store in this link list node
     * @param next The EdgeArrayList to link to.
     */
    EdgeArrayList(UnflatEdge[] val,EdgeArrayList next) {
        this.val=val;
        this.next=next;
    }

    /**
     * Creates EdgeArrayList.
     * @param val Initial UnflatEdge[] to store in list.
     */
    EdgeArrayList(UnflatEdge[] val) {
        this.val=val;
        this.next=null;
    }

    /**
     * Returns size of EdgeArrayList.
     */
    int size() {
        int size=1;
        EdgeArrayList cur=this;
        while(cur.next!=null) {
            size++;
            cur=cur.next;
        }
        return size;
    }
}
