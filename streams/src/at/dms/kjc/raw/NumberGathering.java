package at.dms.kjc.raw;

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

    public static boolean doit(FlatNode top) 
    {
	successful = false;
	FlatNode sink = Sink.getSink(top);
	//no sink or more than one sink
	if (sink == null) {
	    return false;
	}
	//the prints at the sink:
	//no control flow
	//in the work function
	int prints = CheckPrint.check((SIRFilter)sink.contents);
	if (prints < 1) {
	    return false;
	}
	
	System.out.println("Generating Number Gathering Code...");
	Integer initInteger = (Integer)RawBackend.initExecutionCounts.get(sink);
	Integer steadyInteger = (Integer)RawBackend.steadyExecutionCounts.get(sink);
	int init = 0, steady = 0;
	
	if (initInteger != null)
	    init = initInteger.intValue();
	if (steadyInteger != null) 
	    steady = steadyInteger.intValue();
	
	if (steady < 1) {
	    System.out.println
		("Cannot generate number gathering code: Sink not called in Steady State");
	    return false;
	}
	
	//set the globals that are read by makefilegenerator	
	successful = true;
	skipPrints = init * prints;
	printsPerSteady = prints * steady;
	return true;
    }
}



class Sink implements FlatVisitor 
{
    private static FlatNode sink;
    private static boolean multipleSinks;
    private static boolean printOutsideSink;
    
    public static FlatNode getSink(FlatNode top) 
    {
	sink = null;
	multipleSinks = false;
	printOutsideSink = false;
	top.accept(new Sink(), null, true);
	//if more than one sink return null
	//we do not handle this case now
	if (multipleSinks) {
	    System.out.println("Cannot generate number gathering code: Multiple sinks");
	    return null;
	}
	if (printOutsideSink) {
	    System.out.println("Cannot generate number gathering code: Print outside the sink");
	    return null;
	}
	
	return sink;
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()) {
	    SIRFilter filter = (SIRFilter)node.contents;
	    if (filter.getPushInt() == 0) {
		//sink
		if (sink != null)
		    multipleSinks = true;
		sink = node;
	    }
	    else {
		//check if there is a print statement 
		if (ExistsPrint.exists(filter)) {
		    printOutsideSink = true;
		}
		
	    }
	}
    }
}

class ExistsPrint extends SLIREmptyVisitor 
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

class CheckPrint extends SLIREmptyVisitor 
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
	    System.out.println("Cannot generate number gathering code: Print(s) in control flow");
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
	body.accept(this);
	controlFlow--;
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

