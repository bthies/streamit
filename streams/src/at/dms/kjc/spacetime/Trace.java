package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.ArrayList;

/** 
 * 
 **/
public class Trace 
{
    private Trace[] edges;
    private InputTraceNode head;
    private OutputTraceNode tail;
    private int len;
    private Trace[] depends;
    private ArrayList dependsTemp;
    private ArrayList edgesTemp;
    private int primePump;

    /*public Trace (Trace[] edges, Trace[] depends, InputTraceNode head) 
      {
      if (edges == null)
      this.edges = new Trace[0];
      else 
      this.edges = edges;
      
      this.head = head;
      head.setParent(this);
      
      if (depends == null)
      this.depends = new Trace[0];
      else 
      this.depends = depends;
      len=-1;
      }*/

    public Trace(InputTraceNode head) {
	this.head = head;
	head.setParent(this);
	//depends = new Trace[0];
	//edges = new Trace[0];
	len=-1;
	dependsTemp=new ArrayList();
	edgesTemp=new ArrayList();
    }

    public Trace(TraceNode node) {
	if(!(node instanceof FilterTraceNode))
	    Utils.fail("FilterTraceNode expected: "+node);
	head = new InputTraceNode();
	head.setParent(this);
	head.setNext(node);
	node.setPrevious(head);
	//depends = new Trace[0];
	//edges = new Trace[0];
	len=-1;
	dependsTemp=new ArrayList();
	edgesTemp=new ArrayList();
    }

    //Finishes creating Trace
    public int finish() {
	int size=0;
	TraceNode node=head;
	if(node instanceof InputTraceNode)
	    node=node.getNext();
	TraceNode end=node;
	while(node!=null&&node instanceof FilterTraceNode) {
	    node.setParent(this);
	    size++;
	    end=node;
	    node=node.getNext();
	}
	if(node!=null)
	    end=node;
	len=size;
	if(end instanceof OutputTraceNode)
	    tail=(OutputTraceNode)end;
	else {
	    tail=new OutputTraceNode();
	    end.setNext(tail);
	    tail.setPrevious(end);
	}
	tail.setParent(this);
	return size;
    }
    
    //finish() must have been called
    public int size() {
	assert len>-1:"finish() was not called";
	return len;
    }

    public void setHead(InputTraceNode node) 
    {
	head = node;
	node.setParent(this);
    }
    
    public InputTraceNode getHead() 
    {
	return head;
    }

    //finish() must have been called
    public OutputTraceNode getTail() {
	return tail;
    }
    
    public Trace[] getEdges() 
    {
	assert edges!=null:"Must call doneDependencies() beforehand";
	return edges;
    }

    public Trace[] getDepends()
    {
	assert depends!=null:"Must call doneDependencies() beforehand";
	return depends;
    }

    //Deprecated
    public void setEdges(Trace[] edges) {
	if (edges != null)
	    this.edges = edges;
    }
    
    //Deprecated
    public void setDepends(Trace[] depends) {
	if (depends != null) 
	    this.depends = depends;
    }
    
    public void connect(Trace target) {
	edges=new Trace[]{target};
	target.depends=new Trace[]{this};
    }

    public String toString() {
	return "Trace: "+head + "->" + head.getNext() + "->...";
    }

    public boolean depends(Trace trace) {
	if(depends==null) {
	    return dependsTemp.contains(trace);
	} else
	    for(int i=0;i<depends.length;i++)
		if(depends[i]==trace)
		    return true;
	return false;
    }

    public void doneDependencies() {
	depends=new Trace[dependsTemp.size()];
	dependsTemp.toArray(depends);
	dependsTemp=null;
	edges=new Trace[edgesTemp.size()];
	edgesTemp.toArray(edges);
	edgesTemp=null;
    }

    public void addDependency(Trace prev) {
	assert prev!=this:"uhoh";
	if(!dependsTemp.contains(prev))
	    dependsTemp.add(prev);
	if(!prev.edgesTemp.contains(this))
	    prev.edgesTemp.add(this);
    }

    public void setPrimePump(int pp) {
	primePump=pp;
	/*TraceNode cur=head.getNext();
	  while(cur instanceof FilterTraceNode) {
	  ((FilterTraceNode)cur).getFilter().setPrimePump(pp);
	  cur=cur.getNext();
	  }*/
    }

    public int getPrimePump() {
	return primePump;
    }
    
    //return the number of filters in the trace
    public int getNumFilters() 
    {
	TraceNode node = getHead().getNext();
	int ret = 0;
	while (node instanceof FilterTraceNode) {
	    node = node.getNext();
	    ret++;
	}
	return ret;
    }
    
}




