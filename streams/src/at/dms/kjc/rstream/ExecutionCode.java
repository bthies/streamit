package at.dms.kjc.rstream;

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
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;

public class ExecutionCode extends at.dms.util.Utils 
    implements Constants
{
    //These next fields are set by calculateItems()
    //see my thesis for a better explanation
    //number of items to receive between preWork() and work()
    private int bottomPeek = 0; 
    //number of times the filter fires in the init schedule
    private int initFire = 0;
    //number of items to receive after initialization
    private int remaining = 0;    
    
    public static void doit(FlatNode top, HashMap[] executionCounts) 
    {
	assert top.contents instanceof SIRFilter :
	    "Error top of graph is not filter";
	
	assert top.incoming.length == 0 && 
	    top.inputs == 0 &&
	    top.ways == 0 &&
	    top.edges.length == 0 : 
	    "Error: Fused filter contains neighbors";

	SIRFilter filter = (SIRFilter)top.contents;
	
	assert filter.getPushInt() == 0 &&
	    filter.getPopInt() == 0 &&
	    filter.getPeekInt() == 0 : 
	    "Error: fused filter declares non-zero I/O rate(s)";

	//make sure there are no push, pops or peeks...
	assert CheckForCommunication.check(filter) == false : 
	    "Error: Communication expression found in filter";

	assert !(filter instanceof SIRTwoStageFilter) :
	    "Error: Fused filter is a two stage";

	ExecutionCode exeCode = new ExecutionCode();
	
	exeCode.checkSchedule(executionCounts, filter);
	

	JBlock block = new JBlock(null, new JStatement[0], null);
	
	//create the main function of the C code that will call
	// the filter
	exeCode.mainFunction(filter, block);
	
	//create the method and add it to the filter
	JMethodDeclaration mainFunct = 
	    new JMethodDeclaration(null, 
				   at.dms.kjc.Constants.ACC_PUBLIC,
				   CStdType.Void,
				   Names.main,
				   JFormalParameter.EMPTY,
				   CClassType.EMPTY,
				   block,
				   null,
				   null);
	filter.addMethod(mainFunct);
    }

    private void mainFunction(SIRFilter filter, JBlock statements)
    {
	//create the params list, for some reason 
	//calling toArray() on the list breaks a later pass
	List paramList = filter.getParams();
	JExpression[] paramArray;
	if (paramList == null || paramList.size() == 0)
	    paramArray = new JExpression[0];
	else
	    paramArray = (JExpression[])paramList.toArray(new JExpression[0]);
	
	//add the call to the init function
	statements.addStatement
	    (new 
	     JExpressionStatement(null,
				  new JMethodCallExpression
				  (null,
				   new JThisExpression(null),
				   filter.getInit().getName(),
				   paramArray),
				  null));
	
	
	//add the call to the work function
	statements.addStatement(generateSteadyStateLoop(filter));
    }

    //generate the code for the steady state loop
    JStatement generateSteadyStateLoop(SIRFilter filter)
    {
	
	JBlock block = new JBlock(null, new JStatement[0], null);


	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.
	    deepCopy(filter.getWork().getBody());

	//add the cloned work function to the block
	block.addStatement(workBlock);
	
	//return the infinite loop
	return new JWhileStatement(null, 
				   new JBooleanLiteral(null, true),
				   block, 
				   null);
    }
    
    /**
     * Returns a for loop that uses field <var> to count
     * <count> times with the body of the loop being <body>.  If count
     * is non-positive, just returns empty (!not legal in the general case)
     */
    private static JStatement makeForLoop(JStatement body,
					  JLocalVariable var,
					  JExpression count) {
	if (body == null)
	    return new JEmptyStatement(null, null);
	
	// make init statement - assign zero to <var>.  We need to use
	// an expression list statement to follow the convention of
	// other for loops and to get the codegen right.
	JExpression initExpr[] = {
	    new JAssignmentExpression(null,
				      new JLocalVariableExpression(null,
								   var),
				      new JIntLiteral(0)) };
	JStatement init = new JExpressionListStatement(null, initExpr, null);
	// if count==0, just return init statement
	if (count instanceof JIntLiteral) {
	    int intCount = ((JIntLiteral)count).intValue();
	    if (intCount<=0) {
		// return assignment statement
		return new JEmptyStatement(null, null);
	    }
	}
	// make conditional - test if <var> less than <count>
	JExpression cond = 
	    new JRelationalExpression(null,
				      Constants.OPE_LT,
				      new JLocalVariableExpression(null,
								   var),
				      count);
	JExpression incrExpr = 
	    new JPostfixExpression(null, 
				   Constants.OPE_POSTINC, 
				   new JLocalVariableExpression(null,
								   var));
	JStatement incr = 
	    new JExpressionStatement(null, incrExpr, null);

	return new JForStatement(null, init, cond, incr, body, null);
    }
    
    
    /**
     * Check the schedule to see if it is correctly formed.
     * First check to see that the single filter does not 
     * execute in the init stage, and it is the only filtered
     * scheduled.
     *
     * @param executionCounts The execution counts hashmaps created
                              by the scheduler
     * @param filter The single fused filter representing the application
     *
     * @return Returns true if the constructed schedule fits the 
     *         criteria.
     *
     */
    
    private void checkSchedule(HashMap[] executionCounts,
				  SIRFilter filter) 
    {
	//executionCounts[0] = init
	//executionCounts[1] = steady
	
	//first check the initialization schedule 
	//it should be null or the only thing in there should
	//be the filter with a zero execution count
	assert executionCounts[0].keySet().size() == 0 || 
	    executionCounts[0].keySet().size() == 1;
	
	if (executionCounts[0].keySet().size() == 1) {
	    assert executionCounts[0].keySet().contains(filter);
	    assert ((Integer)executionCounts[0].get(filter)).intValue() == 0;
	}
	//now check the steady-state schedule and make sure
	//that the filter is the only thing in there and that
	//it executes
	assert executionCounts[1].keySet().size() == 1;
	assert executionCounts[1].keySet().contains(filter);
	assert ((Integer)executionCounts[1].get(filter)).intValue() > 0;
    }
 
}


