package at.dms.kjc.flatgraph2;

public class IntArrayList {
    int[] value;
    IntList prev;
    IntList next;

    IntArrayList(int[] value,IntList prev,IntList next) {
	this.value=value;
	this.prev=prev;
	this.next=next;
    }
}
