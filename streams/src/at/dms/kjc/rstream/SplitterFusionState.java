package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import at.dms.compiler.*;

//each filter owns its popBuffer, the popBufferIndex, and the pushIndex
//into the next filters popBuffer.
public class SplitterFusionState extends FusionState
{
    private SIRSplitter splitter;

    //the size of the pop buffer (including the peekRestore size)
    private int bufferSize;
    
    public SplitterFusionState (FlatNode node)
    {
	super(node);
	
	assert node.isSplitter();

	calculateRemaining();

	splitter = (SIRSplitter)node.contents;

	necessary = setNecessary();
	
	bufferVar = new JVariableDefinition[1];
	
	bufferVar[0] = makePopBuffer();
    }

    //calculate the remaining items store in peekBufferSize 
    private void calculateRemaining() 
    {
	int itemsSent = 0;  //the number of items sent to the splitter
	int itemsReceived = 0; //the number of items this splitter will receive
	
	if (node.inputs < 1) {
	    return;
	}
	
	assert node.inputs == 1;

	if (node.incoming[0] != null)
	    itemsSent += StrToRStream.getMult(node.incoming[0], true) *
		Util.getItemsPushed(node.incoming[0], node);
	
	
	itemsReceived = StrToRStream.getMult(node, true) * 
	    distinctRoundItems();
	

	remaining[0] =  itemsSent - itemsReceived;

	if (remaining[0] > 0) 
	    System.out.println("** Items remaining on an incoming splitter buffer");
	
	assert remaining[0] >= 0 : "Error calculating remaing for splitter";
    }
    

    //return true if we must generate code for this splitter
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
    
    private JStatement getRRCode(JBlock enclosingBlock, int mult, boolean isInit)
    {
	JBlock loops = new JBlock(null, new JStatement[0], null);
	
	JVariableDefinition induction = 
	    GenerateCCode.newIntLocal(RRCOUNTER, myUniqueID, 0);

	//add the decl of the induction variable
	enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					(null, induction, null));

	for (int i = 0; i < node.ways; i++) {
	    //do nothing if this has 0 weight
	    if (node.weights[i] == 0)
		continue;
	    
	    JVariableDefinition innerVar = 
		GenerateCCode.newIntLocal(RRINNERVAR + myUniqueID + "_", i, 0);
	    
	    FusionState downstream = FusionState.getFusionState(node.edges[i]);

	    //add the decl of the induction variable
	    enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					     (null, innerVar, null));

	    JLocalVariableExpression incomingBuffer = 
		new JLocalVariableExpression(null,
					     getBufferVar(null, isInit));

	    	    
	    JLocalVariableExpression outgoingBuffer = 
		new JLocalVariableExpression
		(null,
		 downstream.getBufferVar(node, isInit));
	    
	    //if init outgoing[induction * weights[i] + innervar]
	    //if steady outgoing[induction * weights[i] + innervar + peekbuffersize_outgoing]
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

	    //incoming[induction * totalWeights + partialSum + innerVar]
	    JAddExpression incomingIndex = 
		new JAddExpression(null,
				   new JMultExpression(null,
						       new JLocalVariableExpression(null, induction),
						       new JIntLiteral(node.getTotalOutgoingWeights())),
				   new JAddExpression(null,
						      new JIntLiteral(node.getPartialOutgoingSum(i)),
						      new JLocalVariableExpression(null, innerVar)));
	    JArrayAccessExpression outgoingAccess = 
		new JArrayAccessExpression(null,
					   outgoingBuffer, outgoingIndex);
	    
	    JArrayAccessExpression incomingAccess = 
		new JArrayAccessExpression(null,
					   incomingBuffer, incomingIndex);
	    

	    JExpressionStatement assignment = 
		new JExpressionStatement(null,
					 new JAssignmentExpression(null,
								   outgoingAccess,
								   incomingAccess),
					 null);
	    loops.addStatement(GenerateCCode.makeDoLoop(assignment,
							 innerVar,
							 new JIntLiteral(node.weights[i])));
	}
	
	return GenerateCCode.makeDoLoop(loops, induction, new JIntLiteral(mult));
    }

    public int getBufferSize(FlatNode prev, boolean init) 
    {
	return  bufferSize;
    }
    
				 
    public JVariableDefinition getBufferVar(FlatNode prev, boolean init) 
    {
	return bufferVar[0];
    }
    

    private JStatement getDuplicateCode(JBlock enclosingBlock, int mult, boolean isInit) 
    {
	JBlock assigns = new JBlock(null, new JStatement[0], null);

	if (!necessary)
	    return assigns;

	JVariableDefinition induction = 
	    GenerateCCode.newIntLocal(DUPLICATECOUNTER, myUniqueID, 0);

	//add the decl of the induction variable
	enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					(null, induction, null));

	for (int i = 0; i < node.ways; i++) {
	    assert node.weights[i] == 1;
	    
	    FusionState downstream =  FusionState.getFusionState(node.edges[i]);

	    //if init outgoing[induction] = incoming[induction]
	    //if steady outgoing[induction + peekBufSize_outgoing] = incoming[induction]
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
    

    private JVariableDefinition makePopBuffer() 
    {
	int mult = Math.max(StrToRStream.getMult(node, false),
			    StrToRStream.getMult(node, true));

	
	int itemsAccessed = mult * distinctRoundItems();

	itemsAccessed += remaining[0];
	
	assert itemsAccessed >= 0;

	bufferSize = itemsAccessed;
	
	if (itemsAccessed == 0)
	    return null;
	
	return makeBuffer(itemsAccessed,
			  Util.getOutputType(node),
			  BUFFERNAME + myUniqueID);
    }

    public int getRemaining(FlatNode prev, boolean isInit) 
    {
	return remaining[0];
    }
    
    
    /**
     * return the number of distinct items sent/received it a round
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

