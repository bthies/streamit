package at.dms.kjc.flatgraph2;

public class IntList {
    int val;
    //IntList prev;
    IntList next;

    IntList(int val,IntList next) {
	this.val=val;
	//this.prev=prev;
	this.next=next;
    }

    IntList(int val) {
	this.val=val;
	this.next=null;
    }
    
    int size() {
	/*if(next==null)
	  return 1;
	  else
	  return 1+next.size();*/
	int size=1;
	IntList cur=this;
	while(cur.next!=null) {
	    size++;
	    cur=cur.next;
	}
	return size;
    }
}
