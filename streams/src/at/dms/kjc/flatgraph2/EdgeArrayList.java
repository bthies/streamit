package at.dms.kjc.flatgraph2;

public class EdgeArrayList {
    UnflatEdge[] val;
    EdgeArrayList next;

    EdgeArrayList(UnflatEdge[] val,EdgeArrayList next) {
	this.val=val;
	this.next=next;
    }

    EdgeArrayList(UnflatEdge[] val) {
	this.val=val;
	this.next=null;
    }

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
