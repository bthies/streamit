package at.dms.kjc.flatgraph2;

public class EdgeList {
    UnflatEdge val;
    EdgeList next;

    EdgeList(UnflatEdge val,EdgeList next) {
	this.val=val;
	this.next=next;
    }

    EdgeList(UnflatEdge val) {
	this.val=val;
	this.next=null;
    }
}
