package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;


/**
 * A pass to generate a run of the simulation that will print
 * various performance numbers, this pass must be run before 
 * RawExecutionCode*.
 **/
public class NumberGathering extends at.dms.util.Utils 
    implements Constants 
{
    //fields that are used by makefilegenerater
    //to produce the bC code...
    //true if we can generate code
    public static boolean successful = false;
    public static int printsPerSteady = 0;
    public static int skipPrints = 0;
    public static FlatNode sink;

    public static boolean doit(FlatNode top) 
    {
	successful = false;
	//find the sinks and make sure they are synchronized
	HashSet sinks = Sink.getSinks(top);
	//there could be multiple sinks, find one that works
	Iterator sinksIt = sinks.iterator();
	while (sinksIt.hasNext()) {
	    sink = (FlatNode)sinksIt.next();
	    //no sink or more than one sink
	    if (sink == null) {
		continue;
	    }
	    //the prints at the sink:
	    //no control flow
	    //in the work function
	    int prints = CheckPrint.check((SIRFilter)sink.contents);

	    //if this sink does not print, keep looking for one that does
	    if (prints == 0) 
		continue;
	    // if we failed, unroll the filter on the loops that we
	    // indicated
	    if (prints == -1) {
		SinkUnroller.doit(sink);
		prints = CheckPrint.check((SIRFilter)sink.contents);
	    }
	    //if there still prints in control flow after unrolling, keep searching
	    if (prints == -1) {
		System.out.println("Print(s) in control flow");
		continue;
	    }
	    
	    Integer initInteger = (Integer)RawBackend.initExecutionCounts.get(sink);
	    Integer steadyInteger = (Integer)RawBackend.steadyExecutionCounts.get(sink);
	    int init = 0, steady = 0;
	    
	    if (initInteger != null)
		init = initInteger.intValue();
	    if (steadyInteger != null) 
		steady = steadyInteger.intValue();
	    
	    if (steady < 1) {
		System.out.println
		    ("Sink not called in Steady State");
		continue;
	    }

	    System.out.println("Generating Number Gathering Code...");
	    //set the globals that are read by makefilegenerator	
	    successful = true;
	    skipPrints = init * prints;
	    printsPerSteady = prints * steady;
	    System.out.println("The Sink: " + sink.contents.getName());
	    return true;
	}
	System.out.println("Cannot Generate Number Gathering Code.  Could not find a suitable sink...");
	return false;
    }

    static class Sink implements FlatVisitor 
    {

	private static FlatNode sink;
	//used if there are multiple sinks, to analyze them...
	private static HashSet possibleSinks;
	private static boolean multipleSinks;
	private static boolean printOutsideSink;
	private static FlatNode toplevel;

	public static HashSet getSinks(FlatNode top) 
	{
	    toplevel = top;
	    sink = null;
	    possibleSinks = new HashSet();
	    multipleSinks = false;
	    top.accept(new Sink(), null, true);
	    //if more than one sink return null
	    //we do not handle this case now
	    if (multipleSinks) {
		FlatNode ancestor = getLeastCommonAncestor();
		System.out.println("The LCA is " + ancestor.contents.getName());
	    
		Iterator sinksIt = possibleSinks.iterator();
		while (sinksIt.hasNext()) {
		    FlatNode current = (FlatNode)sinksIt.next();
		    //traverse the path from the current sink to the ancestor
		    //and check to see if any of the filter have a pop(peek) rate
		    //equal to 0, if so, then the sinks are not synchronized
		    //how to check then?
		    if (!isSynchronized(ancestor, current)) {    
			System.out.println("Cannot generate number gathering code: " + 
					   "Multiple sinks that are not synchronized");
			return null;
		    }
		}
	    }
	    //now pick a sink, we will just pick the first one in possibleSinks
	    //we have to pick a sink that has a print
	    return possibleSinks;
	}
    
	private static FlatNode getLeastCommonAncestor() 
	{
	    //a vector of hashsets of all the ancestors of 
	    //each sink
	    Vector ancestors = new Vector();
	    Iterator sinksIt = possibleSinks.iterator();
	    //get all the ancestor for each sink
	    while (sinksIt.hasNext()) {
		FlatNode sink = (FlatNode)sinksIt.next();
		ancestors.add(getAllAncestors(sink, true));
	    }
	    //get the set representing the intersection of all
	    //the ancestor sets...this is the common ancestors
	    HashSet commonAncestors = (HashSet)ancestors.get(0);

	    for (int i = 1; i < ancestors.size(); i++)
		commonAncestors = intersection(commonAncestors, (HashSet)ancestors.get(i));    
	    
	    //find the most downstream ancestor...
	    FlatNode lca = null;
	    Iterator bft = BreadthFirstTraversal.getTraversal(toplevel).listIterator();
	    while (bft.hasNext()) {
		FlatNode current = (FlatNode)bft.next();
		if (commonAncestors.contains(current)) {
		    lca = current;
		}
	    }
	
	    //this is not the best way of doing it, but it was easy to code up!
	    return lca;
	}

	//get all the ancestors of a node and return the hashset, but do not add the 
	//node itself to the hash set, so firstcall is true on the first call,
	//and false on all the recursive calls.
	private static HashSet getAllAncestors(FlatNode node, boolean firstCall) 
	{
	    HashSet ret = new HashSet();
	    //add self
	    if (!firstCall)
		ret.add(node);
	    //add the upstream
	    if (node == null || node.incoming == null ) 
		return ret;
	
	    for (int i = 0; i < node.incoming.length; i++)
		RawBackend.addAll(ret, getAllAncestors(node.incoming[i], false));
	    return ret;
	}

	private static HashSet intersection(HashSet x, HashSet y) 
	{
	    HashSet ret = new HashSet();
	
	    Iterator xit = x.iterator();
	    while (xit.hasNext()) {
		Object current = xit.next();
		if (y.contains(current))
		    ret.add(current);
	    }
	    return ret;
	}
    
	    

	//returns true if every filter on every path from current to ancestor (following
	//back edges) has pop > 0, meaning it is synchronized to the upstream
	private static boolean isSynchronized(FlatNode ancestor, FlatNode current) 
	{
	    if (ancestor == current) 
		return true;

	    ///check the pop rate
	    if (current.isFilter())
		if (((SIRFilter)current.contents).getPopInt() == 0)
		    return false;

	    //old way
	    //check all the incoming arcs of the joiner return 
	    //true if all of them are true...
	    //for (int i = 0; i < current.incoming.length; i++)
	    //if (!isSynchronized(ancestor, current.incoming[i]))
	    //	    return false;

	    //find at least one path to the ancestor that 
	    //does not have any 0 pop filters...
	    for (int i = 0; i < current.incoming.length; i++)
		if (isSynchronized(ancestor, current.incoming[i]))
		    return true;

	    
	    return false;    
	}
    

	public void visitNode(FlatNode node) 
	{
	    if (node.isFilter()) {
		SIRFilter filter = (SIRFilter)node.contents;
		if (filter.getPushInt() == 0) {
		    //sink
		    if (sink != null)
			multipleSinks = true;
		    //add this to the hash map of sinks
		    possibleSinks.add(node);
		    //if this is the only sink, record it here
		    sink = node;
		}
	    }
	}
    }

    static class ExistsPrint extends SLIREmptyVisitor 
    {
	private static boolean found;
    
	public static boolean exists(SIRFilter filter) 
	{
	    ExistsPrint existPrint = new ExistsPrint();
	    found = false;
	
	    for (int i = 0; i < filter.getMethods().length; i++) {
		filter.getMethods()[i].accept(existPrint);
		if (found)
		    return true;
	    }
	    return false;
	}
	public void visitPrintStatement(SIRPrintStatement self,
					JExpression arg) {
	    found = true;
	}
    }

    static class CheckPrint extends SLIREmptyVisitor 
    {
    
	private static int prints;
	private static int controlFlow;
	private static boolean printInControlFlow;
    
	//returns true if we find communication statements/expressions
	//outside of the work function (i.e. in a helper function)
	//or a print is embedded inside control flow
	public static int check(SIRFilter filter) 
	{
	    prints = 0;
	    printInControlFlow = false;
	
	    for (int i = 0; i < filter.getMethods().length; i++) {
		//skip the work function
		if (filter.getMethods()[i].equals(filter.getWork()))
		    continue;

		//visit method

		filter.getMethods()[i].accept(new CheckPrint());
	    
		//print not in work function
		if (prints > 0) {
		    System.out.println("Cannot generate number gathering code: Print outside work()");
		    return -1;
		}
	    
		if (controlFlow != 0)
		    Utils.fail("Error in CheckPrint Visitor in NumberGathering");
	    }
	
	    filter.getWork().accept(new CheckPrint());
	    if (printInControlFlow) {
		return -1;
	    }
	    return prints;
	}
    
	/**
	 * Visits a print statement.
	 */
	public void visitPrintStatement(SIRPrintStatement self,
					JExpression arg) {
	    prints++;
	    if (controlFlow > 0)
		printInControlFlow = true;
	    if (controlFlow < 0)
		Utils.fail("Error in CheckPrint Visitor in NumberGathering");
	}


	public void visitWhileStatement(JWhileStatement self,
					JExpression cond,
					JStatement body) {
	    controlFlow++;
	    cond.accept(this);
	    body.accept(this);
	    controlFlow--;
	}

	/**
	 * prints a switch statement
	 */
	public void visitSwitchStatement(JSwitchStatement self,
					 JExpression expr,
					 JSwitchGroup[] body) {
	    expr.accept(this);
	    controlFlow++;
	    for (int i = 0; i < body.length; i++) {
		body[i].accept(this);
	    }
	    controlFlow--;
	}
    
	public void visitIfStatement(JIfStatement self,
				     JExpression cond,
				     JStatement thenClause,
				     JStatement elseClause) {
	    cond.accept(this);
	    controlFlow++;
	    thenClause.accept(this);
	    if (elseClause != null) {
		elseClause.accept(this);
	    }
	    controlFlow--;
	}
    
	public void visitForStatement(JForStatement self,
				      JStatement init,
				      JExpression cond,
				      JStatement incr,
				      JStatement body) {
	    if (init != null) {
		init.accept(this);
	    }
	    controlFlow++;
	    if (cond != null) {
		cond.accept(this);
	    }
	    if (incr != null) {
		incr.accept(this);
	    }
	    // see if we can already infer the number of times this loop executes
	    int numExec = Unroller.getNumExecutions(init, cond, incr, body);
	    if (numExec!=-1) {
		// if so, just multiply the prints by the execution count
		controlFlow--;
		int origPrints = prints;
		prints = 0;
		body.accept(this);
		prints = prints * numExec + origPrints;
	    } else {
		// if not, see if there are any prints in the loop anyway
		int origPrints = prints;
		body.accept(this);
		// mark the loop for unrolling if so
		if (origPrints<prints) {
		    self.setUnrolled(false);
		}
		controlFlow--;
	    }
	}
    
	public void visitDoStatement(JDoStatement self,
				     JExpression cond,
				     JStatement body) {
	    controlFlow++;
	    body.accept(this);
	    cond.accept(this);
	    controlFlow--;
	}

    }
}

