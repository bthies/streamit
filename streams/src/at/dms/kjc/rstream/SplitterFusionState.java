package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import at.dms.compiler.*;

/**
 * This class represent the state and conversion necessary to convert 
 * a splitter FlatNode into imperative SIR code so it can be added to 
 * the application's SIR code.
 * 
 * For duplicate splitters, we just create a loop that will copy items from 
 * the incoming buffer to each outgoing buffer.
 * 
 * For round-robin splitters we create an outer loop that contains 
 * assignments from the incoming buffer to the outgoing buffer in order and frequency
 * given by the round-robin weights
 *
 * @author Michael Gordon
 * 
 */
public class SplitterFusionState extends FusionState
{
    /** the splitter object this represents **/
    private SIRSplitter splitter;

    /** the size of the pop buffer (including the remaining items) **/
    private int bufferSize;
    
    
    protected SplitterFusionState (FlatNode node)
    {
	super(node);
	
	assert node.isSplitter();

	calculateRemaining();

	splitter = (SIRSplitter)node.contents;

	necessary = setNecessary();
	
	bufferVar = new JVariableDefinition[1];
	
	bufferVar[0] = makePopBuffer();
    }

    /** calculate the number of items remaining on this splitter's incoming buffer
	after the init stage has executed **/
    private void calculateRemaining() 
    {
	int itemsSentTo = 0;  //the number of items sent to the splitter
	int itemsSentFrom = 0; //the number of items will send along
	
	if (node.inputs < 1) {
	    return;
	}
	
	assert node.inputs == 1;

	if (node.incoming[0] != null)
	    itemsSentTo += StrToRStream.getMult(node.incoming[0], true) *
		Util.getItemsPushed(node.incoming[0], node);
	
	//calc the number of items sent along by this splitter
	itemsSentFrom = StrToRStream.getMult(node, true) * 
	    distinctRoundItems();
	

	remaining[0] =  itemsSentTo - itemsSentFrom;

	if (remaining[0] > 0) 
	    System.out.println("** Items remaining on an incoming splitter buffer");
	
	assert remaining[0] >= 0 : "Error calculating remaing for splitter";
    }
    

    /** see if we need to generate code for this splitter, if the splitter is a duplicate
	and there is no funny business going on, then we can just share the incoming buffer
	across the parallel streams **/
    private boolean setNecessary() 
    {
	if (StrToRStream.GENERATE_UNNECESSARY)
	    return true;

	if (node.isDuplicateSplitter() && node.ways > 0) {
	    //check that all the down stream buffersize are equal for both init and steady, 
	    //there is no peek buffer for immediate downstream 
	    int bufferSizeSteady = FusionState.getFusionState(node.edges[0]).getBufferSize(node, false);
	    int bufferSizeInit = FusionState.getFusionState(node.edges[0]).getBufferSize(node, true);

	    //make sure there are no items remaining on the incoming buffer after initialization
	    if (remaining[0] > 0)
		return true;

	    //make sure that the downstream buffersizes equal the buffer size incoming to this splitter
	    if (this.getBufferSize(null, true) !=  bufferSizeInit) {
		//System.out.println("Splitter buffer size not equal to downstream buffersize (init)");
		return true;
	    }
	    if (this.getBufferSize(null, false) !=  bufferSizeSteady) {
		//System.out.println("Splitter buffer size not equal to downstream buffersize (steady) " + 
		return true;
	    }
	    //make sure that we have a filter
	    if (!node.edges[0].isFilter()) {
		//System.out.println("Downstream not filter");
		return true;
	    }

	    //make sure there is no remaining items
	    if (FusionState.getFusionState(node.edges[0]).getRemaining(node, true) > 0) {
		//System.out.println("Downstream peek buffer > 0");
		return true;
	    }
	    
	    for (int i = 1; i < node.ways; i++) {
		//make sure that there is a filter connected
		if (!node.edges[i].isFilter()) {
		    //System.out.println("Downstream not filter");
		    return true;
		}
		//check the peek buffer, it must be zero
		if (FusionState.getFusionState(node.edges[0]).getRemaining(node, true) > 0) {
		    //System.out.println("Downstream peek buffer > 0");
		    return true;
		}
		//if all buffer size's are equal, good
		if (bufferSizeSteady != FusionState.getFusionState(node.edges[i]).getBufferSize(node, false)) {
		    //System.out.println("Unequal buffer sizes of children (steady)");
		    return true;
		}
		if (bufferSizeInit != FusionState.getFusionState(node.edges[i]).getBufferSize(node, true)) {
		    //System.out.println("Unequal buffer sizes of children (init)");
		    return true;
		}
		
	    }
	    //got here, so everything passed! it is not true that this splitter needs to be generated
	    System.out.println("** Found unnecessary splitter " + splitter);
	    return false;
	}   
	//not a duplicate splitter
	return true;
    }


    /** perform any initialization tasks, including setting the downstream buffers to be 
	this splitter's incoming buffer if this splitter is unnecessary (thereby bypassing it)
	and declaring the incoming buffer **/
    public void initTasks(Vector fields, Vector functions,
			  JBlock initFunctionCalls, JBlock main) 
    {
	//if this filter is unnecessary, make sure all the downstream neighbor
	//filter's share the same incoming buffer, the buffer of the splitter...
	if (!necessary) {
	    for (int i = 0; i < node.ways; i++)
		((FilterFusionState)FusionState.getFusionState(node.edges[i])).
		    sharedBufferVar(bufferVar[0]);
	}

	//add the buffer declaration to the main method
	if (getBufferVar(null, true) != null)
	    main.addStatementFirst(new JVariableDeclarationStatement(null,
								     getBufferVar(null, true),
								     null));
    }
    
    
    /** Return the block containing imperative SIR code that will perform the 
	splitting of the incoming buffer.

	For duplicate splitters, we just create a loop that will copy items from 
	the incoming buffer to each outgoing buffer.

	For round-robin splitters we create an outer loop that contains 
	assignments from the incoming buffer to the outgoing buffer in order and frequency
	given by the round-robin weights
    **/
    public JStatement[] getWork(JBlock enclosingBlock, boolean isInit) 
    {
	
	JBlock statements = new JBlock(null, new JStatement[0], null);
	
	int mult = StrToRStream.getMult(getNode(), isInit);

	JavaStyleComment[] comment = {new JavaStyleComment(splitter.toString(),
							   true,
							   false,
							   false)};
	
	statements.addStatement(new JEmptyStatement(null, comment));
	
	if (mult == 0)
	    return statements.getStatementArray();
	
	//add the block to do the data reordering
	if (node.isDuplicateSplitter()) {
	    statements.addStatement(getDuplicateCode(enclosingBlock, mult, isInit));
	}
	else {
	    statements.addStatement(getRRCode(enclosingBlock, mult, isInit));
	}

	//either way, we have to generate code to save the non-pop'ed items now
	//by moving them to beginning of the buffer
	if (remaining[0] > 0) {
	    //create the loop counter
	    JVariableDefinition loopCounterBackup = 
		GenerateCCode.newIntLocal(BACKUPCOUNTER, myUniqueID, 0);
	    //add the declaration of the counter
	    enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					     (null, loopCounterBackup, null));
	    //make the back up loop, move peekBufferItems starting at mult*weight
	    //to the beginning 
	    statements.addStatement 
		(remainingBackupLoop(bufferVar[0],
				     loopCounterBackup,
				     StrToRStream.getMult(node, isInit) * distinctRoundItems(),
				     remaining[0]));
	}

	return statements.getStatementArray();
    }
    
    /** create the round-robin code, an outer loop that contains 
	assignments from the incoming buffer to the outgoing buffer in order and frequency
	given by the round-robin weights **/
    private JStatement getRRCode(JBlock enclosingBlock, int mult, boolean isInit)
    {
	JBlock loops = new JBlock(null, new JStatement[0], null);
	
	JVariableDefinition induction = 
	    GenerateCCode.newIntLocal(RRCOUNTER, myUniqueID, 0);

	//add the decl of the induction variable
	enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					(null, induction, null));
	//for each outgoing edge...
	for (int i = 0; i < node.ways; i++) {
	    //do nothing if this has 0 weight
	    if (node.weights[i] == 0)
		continue;
	    //get a new induction var for the inner loop
	    JVariableDefinition innerVar = 
		GenerateCCode.newIntLocal(RRINNERVAR + myUniqueID + "_", i, 0);
	    //get the downstream fusion state
	    FusionState downstream = FusionState.getFusionState(node.edges[i]);

	    //add the decl of the induction variable
	    enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					     (null, innerVar, null));
	    //the incoming buffer
	    JLocalVariableExpression incomingBuffer = 
		new JLocalVariableExpression(null,
					     getBufferVar(null, isInit));

	    //the outgoing buffer of this way
	    JLocalVariableExpression outgoingBuffer = 
		new JLocalVariableExpression
		(null,
		 downstream.getBufferVar(node, isInit));
	    
	    //now create the buffer assignment

	    //if init outgoing[induction * weights[i] + innervar]
	    //if steady outgoing[induction * weights[i] + innervar + remaining_outgoing]
	    JAddExpression outgoingIndex = 
		new JAddExpression(null,
				   new JMultExpression(null, 
						       new JLocalVariableExpression(null, induction),
						       new JIntLiteral(node.weights[i])),
				   new JLocalVariableExpression(null, innerVar));
	    
	    if (!isInit && downstream.getRemaining(node, isInit) > 0) 
		outgoingIndex = new JAddExpression(null,
						   outgoingIndex,
						   new JIntLiteral(downstream.getRemaining(node, isInit)));

	    JArrayAccessExpression outgoingAccess = 
		new JArrayAccessExpression(null,
					   outgoingBuffer, outgoingIndex);
	    

	    //incoming[induction * totalWeights + partialSum + innerVar]
	    JAddExpression incomingIndex = 
		new JAddExpression(null,
				   new JMultExpression(null,
						       new JLocalVariableExpression(null, induction),
						       new JIntLiteral(node.getTotalOutgoingWeights())),
				   new JAddExpression(null,
						      new JIntLiteral(node.getPartialOutgoingSum(i)),
						      new JLocalVariableExpression(null, innerVar)));
	    JArrayAccessExpression incomingAccess = 
		new JArrayAccessExpression(null,
					   incomingBuffer, incomingIndex);
	    

	    JExpressionStatement assignment = 
		new JExpressionStatement(null,
					 new JAssignmentExpression(null,
								   outgoingAccess,
								   incomingAccess),
					 null);
	    //now place the assignment in a for loop with trip count equal to the weight on the
	    //edge
	    loops.addStatement(GenerateCCode.makeDoLoop(assignment,
							 innerVar,
							 new JIntLiteral(node.weights[i])));
	}
	//loop the inner loops for the multiplicity of the splitter in this stage
	return GenerateCCode.makeDoLoop(loops, induction, new JIntLiteral(mult));
    }

    /** return the outgoing buffersize **/
    public int getBufferSize(FlatNode prev, boolean init) 
    {
	return  bufferSize;
    }
    
    /** return the outgoing buffer var for this splitter **/
    public JVariableDefinition getBufferVar(FlatNode prev, boolean init) 
    {
	return bufferVar[0];
    }
    
    /** 
	For duplicate splitters, we just create a loop that will copy items from 
	the incoming buffer to each outgoing buffer. This sounds redundant, but in 
	certain cases we still need to generate this, for instance, each downstream 
	buffer may have different remaining items...
    **/
    private JStatement getDuplicateCode(JBlock enclosingBlock, int mult, boolean isInit) 
    {
	JBlock assigns = new JBlock(null, new JStatement[0], null);

	if (!necessary)
	    return assigns;
	//the induction var of the loop
	JVariableDefinition induction = 
	    GenerateCCode.newIntLocal(DUPLICATECOUNTER, myUniqueID, 0);

	//add the decl of the induction variable
	enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					(null, induction, null));

	//for each outgoing edge.. add a statement to copy on item from the
	//incoming buffer to the current outgoing buffer
	for (int i = 0; i < node.ways; i++) {
	    assert node.weights[i] == 1;
	    //get the downstream fusion state
	    FusionState downstream =  FusionState.getFusionState(node.edges[i]);

	    //if init outgoing[induction] = incoming[induction]
	    //if steady outgoing[induction + remaining_outgoing] = incoming[induction]
	    JLocalVariableExpression incomingBuffer = 
		new JLocalVariableExpression(null,
					     getBufferVar(null, isInit));

	    JLocalVariableExpression outgoingBuffer = 
		new JLocalVariableExpression
		(null, downstream.getBufferVar(node, isInit));
								      
	    
	    JArrayAccessExpression rhs = 
		new JArrayAccessExpression(null, incomingBuffer, 
					   new JLocalVariableExpression
					   (null, induction));
	    
	    JExpression outgoingIndex = new JLocalVariableExpression(null, induction);
	    //add the downstream's remaining to the outgoing index expression
	    if (!isInit && downstream.getRemaining(node, isInit) > 0) 
		outgoingIndex = new JAddExpression(null,
						   outgoingIndex,
						   new JIntLiteral(downstream.getRemaining(node, isInit)));

	    JArrayAccessExpression lhs =
		new JArrayAccessExpression(null, outgoingBuffer,
					   outgoingIndex);
	    
	    JExpressionStatement assignment = 
		new JExpressionStatement(null,
					 new JAssignmentExpression(null,
								   lhs,
								   rhs),
					 null);
	    assigns.addStatement(assignment);
	}
	
	return GenerateCCode.makeDoLoop(assigns, induction, 
					     new JIntLiteral(mult));
    }
    
    /** create the pop (incoming) buffer for this node **/
    private JVariableDefinition makePopBuffer() 
    {
	//the multiplicity is the max of the steady and the init
	int mult = Math.max(StrToRStream.getMult(node, false),
			    StrToRStream.getMult(node, true));

	
	int itemsAccessed = mult * distinctRoundItems();
	//add the remaining items
	itemsAccessed += remaining[0];
	
	assert itemsAccessed >= 0;

	bufferSize = itemsAccessed;
	
	if (itemsAccessed == 0)
	    return null;
	
	return makeBuffer(itemsAccessed,
			  Util.getOutputType(node),
			  BUFFERNAME + myUniqueID);
    }
    /** return the items remaining on the incoming buffer after the init stage **/
    public int getRemaining(FlatNode prev, boolean isInit) 
    {
	return remaining[0];
    }
    
    
    /**
     * return the number of distinct items sent/received it a round of the splitter
    **/
    private int distinctRoundItems() 
    {
	if (node.isDuplicateSplitter())
	    return 1;
	else
	    return node.getTotalOutgoingWeights();
    }


    public static String DUPLICATECOUNTER = "__dup__counter_";
    public static String RRCOUNTER = "__rrsplit__counter_";
    public static String RRINNERVAR = "__rrsplit__inner_";
    
}

