package at.dms.kjc.spacedynamic;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;
import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.stats.StatisticsGathering;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.lir.*;
import java.util.*;
import java.io.*;
import at.dms.util.Utils;

/**

*/

public class StreamGraph
{
    /** The toplevel stream container **/
    private SIRStream str;
    /** A list of all the static sub graphs **/
    private List staticSubGraphs;
    /** the entry point static subgraph **/
    private StaticStreamGraph topLevel;

    public StreamGraph(SIRStream top) 
    {
	this.str = top;
	staticSubGraphs = new LinkedList();
    }
    

    public void createStaticStreamGraphs() 
    {
	int id = 0;
	assert str instanceof SIRPipeline;
	
	List children = ((SIRPipeline)str).getChildren();	

	//loop thru all the streams in the toplevel pipeline and 
	//see if any have endpoints that are dynamic, make the 
	//boundaries of the static stream graphs at these points...

	StaticStreamGraph prev = null;


	while (!children.isEmpty()) {
	    SIRStream current = (SIRStream)children.remove(0);
	    //the new pipeline, adding as many pipelines after the first without dynamic
	    //entries assuming the first does not have a dynamic exit...
	    SIRPipeline currentPipeline = new SIRPipeline("CreatedPipeLine" + id++);
	    //the list of children for the new pipeline
	    LinkedList currentChildren = new LinkedList();
	    StaticStreamGraph ssg = new StaticStreamGraph(prev, currentPipeline);
	    //add this subgraph to the list 
	    staticSubGraphs.add(ssg);
	    if (prev != null)
		prev.setNext(ssg);

	    //set the toplevel
	    if (topLevel == null)
		topLevel = ssg;
	    
	    //we need to add a dummy input filter for compatiblity if the first stream has input
	    // and it is not dynamic
	    if (current.getInputType() != CStdType.Void && !dynamicEntry(current))
		currentChildren.add(new SIRDummySource(current.getInputType()));
	    
	    currentChildren.add(current);
	    
	    //if the first stream has a dynamic exit, then start a new graph
	    if (!dynamicExit(current)) {
		//see if we can add any more of the toplevel pipeline's children...
		while(!children.isEmpty()) {
		    SIRStream tryMe = (SIRStream)children.get(0);
		    //exit if tryMe has dynamic entry, it will start a new static sub graph...
		    if (dynamicEntry(tryMe))
			break;
		    //add tryme to the list of current children for the pipeline we are constructing
		    currentChildren.add(tryMe);
		    //remove tryme from the children of the toplevel pipeline
		    children.remove(0);
		    
		    //if tryme has dynamic output then end this pipeline...
		    if (dynamicExit(tryMe))
			break;
		}
	    }
	    
		
	    //set the children of the pipeline, this should correct set 
	    //the children's parent field
	    currentPipeline.setChildren(currentChildren);
 
	    //we need to add a dummy filter if the current pipeline outputs
	    // and is not dynamic for output
	    if (currentPipeline.getOutputType() != CStdType.Void && !dynamicExit(currentPipeline)) 
		currentPipeline.add(new SIRDummySink(currentPipeline.getOutputType()));

	    //set prev to ssg so we can 
	    prev = ssg;
	}

	//now all the static stream graphs are created 
	//convert all filters with dynamic rates to static rates...
	convertDynamicToStatic();

	//make sure no dynamic rates inside the staticstreamgraphs

    }
    /**
     * Convert all dynamic rate filter declarations to static approximations
     **/
    private void convertDynamicToStatic() 
    {
	class ConvertDynamicToStatic extends EmptyStreamVisitor
	{
	    //set this to try as soon as we visit one filter!, this is used
	    //to make sure that dynamic rates only occur at the endpoints of static sub graphs
	    private boolean visitedAFilter = false;
	    //set this after we have seen a dynamic push to true, we should see no more
	    //filters after that!!
	    private boolean shouldSeeNoMoreFilters = false;

	    /** This is not used anymore!!!, part of an old implementation !!**/
	    private JIntLiteral convertRangeToInt(SIRRangeExpression exp) 
	    {
		
		
		if (exp.getAve() instanceof JIntLiteral) {
		    //first see if there is an average hint that is a integer
		    return (JIntLiteral)exp.getAve();
		} else if (exp.getMin() instanceof JIntLiteral &&
			   exp.getMax() instanceof JIntLiteral) {
		    //if we have both a max and min bound, then just take the 2 
		    //return the average..
		    return new JIntLiteral((((JIntLiteral)exp.getMin()).intValue() +
					    ((JIntLiteral)exp.getMax()).intValue()) / 2);
		} else if (exp.getMin() instanceof JIntLiteral) {
		    //if we have a min, just return that...
		    return (JIntLiteral)exp.getMin();
		} else {
		    //otherwise just return 1 
		    return new JIntLiteral(1);
		}
	    }
	    /** convert any dynamic rate declarations to static **/
	    public void visitFilter(SIRFilter self,
				    SIRFilterIter iter) {
		assert !shouldSeeNoMoreFilters : "Dynamic Rates in middle of static sub graph " + self;

		
		if (self.getPop().isDynamic()) {
		    //convert any dynamic pop rate to zero!!
		    self.setPop(new JIntLiteral(0));
		    assert !visitedAFilter : "Dynamic Rates in the middle of static sub graph " + self;
		}
		if (self.getPeek().isDynamic()) {
		    //convert any dynamic peek rate to zero!!
		    self.setPeek(new JIntLiteral(0));
		    assert !visitedAFilter : "Dynamic Rates in the middle of static sub graph " + self;
		}
		if (self.getPush().isDynamic()) {
		    //convert any dynamic push rate to zero
		    self.setPush(new JIntLiteral(0));
		    //now that we have seen a dynamic push, this should be the last filter!!
		    shouldSeeNoMoreFilters = true;
		}
		visitedAFilter = true;
	    }

	    public void visitPhasedFilter(SIRPhasedFilter self,
					  SIRPhasedFilterIter iter) {
		assert false : "Phased filters not supported!";
	    }
	}

	StaticStreamGraph current = topLevel;
	//visit all the subgraphs converting dynamic rates to static...
	while (current != null) {
	 
	    IterFactory.createFactory().createIter(current.getTopLevelSIR()).accept(new ConvertDynamicToStatic());
	    
	    current = current.getNext();
	}	
    }
    

    public boolean dynamicEntry(SIRStream stream) 
    {
	if (stream instanceof SIRFilter) {
	    return ((SIRFilter)stream).getPop().isDynamic() ||
		((SIRFilter)stream).getPeek().isDynamic();
	}
	else if (stream instanceof SIRPipeline) {  
	    //call dynamicExit on the last element of the pipeline
	    return dynamicEntry(((SIRPipeline)stream).get(0));
	}
	else //right now we can never have a dynamic entryunless in the above cases
	    return false;
    }
    
    public boolean dynamicExit(SIRStream stream) 
    {
	if (stream instanceof SIRFilter) {
	    return ((SIRFilter)stream).getPush().isDynamic();
	}
	else if (stream instanceof SIRPipeline) {  
	    //call dynamicExit on the last element of the pipeline
	    return dynamicExit(((SIRPipeline)stream).get(((SIRPipeline)stream).size() - 1));
	}
	else //right now we can never have a dynamic exit unless in the above cases
	    return false;
    }
    
    public List getStaticSubGraphs() 
    {
	return staticSubGraphs;
    }
    
    /** for each sub-graph, assign a certain number of tiles to it **/
    public void tileAssignment() 
    {
	StaticStreamGraph current = topLevel;
	
	while (current != null) {
	    final int[] filters = {0};

	    IterFactory.createFactory().createIter(current.getTopLevelSIR()).accept(new EmptyStreamVisitor() {
		    public void visitFilter(SIRFilter self,
					    SIRFilterIter iter) {
			if (!(self instanceof SIRDummySource || self instanceof SIRDummySink)) {
			    filters[0]++;
			}
			
		    }
		}); 
	    current.setNumTiles(filters[0]);
	    current = current.getNext();
	}
    }


    public void dumpStaticStreamGraph() 
    {
	StaticStreamGraph current = topLevel;
	
	while (current != null) {
	    System.out.println("******* StaticStreamGraph ********");
	    System.out.println("Dynamic rate input = " + dynamicEntry(current.getTopLevelSIR()));
	    System.out.println("InputType = " + current.getTopLevelSIR().getInputType());
	    System.out.println(current.toString());
	    System.out.println("Tiles Assigned = " + current.getNumTiles());
	    System.out.println("OutputType = " + current.getTopLevelSIR().getOutputType());
	    System.out.println("Dynamic rate output = " + dynamicExit(current.getTopLevelSIR()));
	    StreamItDot.printGraph(current.getTopLevelSIR(), current.getTopLevelSIR().getIdent() + ".dot");
	    current = current.getNext();
	    System.out.println("**********************************");
	}
    }

}
