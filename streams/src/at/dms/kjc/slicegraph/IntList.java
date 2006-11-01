package at.dms.kjc.slicegraph;

/**
 * Linked list of int. Useful data representation for
 * FlattenGraph. This was written before Java1.5 generics so feel free
 * to replace.
 * @author jasperln
 */
public class IntList {
    /**
     * The int to store in this node.
     */
    int val;
    /**
     * The pointer to next node.
     */
    IntList next;

    /**
     * Create IntList node and link to existing IntList.
     * @param val The int to store in this node.
     * @param next Pointer to existing IntList.
     */
    IntList(int val,IntList next) {
        this.val=val;
        this.next=next;
    }
    
    /**
     * Create new IntList with initial entry.
     * @param val The initial int to store in list.
     */
    IntList(int val) {
        this.val=val;
        this.next=null;
    }
    
    /**
     * Returns size of IntList.
     */
    int size() {
        int size=1;
        IntList cur=this;
        while(cur.next!=null) {
            size++;
            cur=cur.next;
        }
        return size;
    }
}
