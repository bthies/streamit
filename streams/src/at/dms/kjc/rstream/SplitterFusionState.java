package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;

//each filter owns its popBuffer, the popBufferIndex, and the pushIndex
//into the next filters popBuffer.
public class SplitterFusionState extends FusionState
{
    private SIRSplitter splitter;
    
    public SplitterFusionState (FlatNode node)
    {
	super(node);
	
	assert node.isSplitter();
	
	checkSplitter(false);
	checkSplitter(true);

	splitter = (SIRSplitter)node.contents;

	necessary = setNecessary();
	
	//if (!necessary)
	    //System.out.println("** Found unnecessary splitter");

	bufferVar = new JVariableDefinition[1];
	bufferVarInit = new JVariableDefinition[1];
	
	bufferVar[0] = makeBuffer(false);
	bufferVarInit[0] = makeBuffer(true);
    }

    private boolean setNecessary() 
    {
	if (node.isDuplicateSplitter() && node.ways > 0) {
	    //check that all the down stream buffersize are equal for both init and steady, 
	    //there is no peek buffer for immediate downstream 
	    int bufferSizeSteady = FusionState.getFusionState(node.edges[0]).getBufferSize(node, false);
	    int bufferSizeInit = FusionState.getFusionState(node.edges[0]).getBufferSize(node, true);

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
	    if (FusionState.getFusionState(node.edges[0]).getPeekBufferSize() != 0) {
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
		if (FusionState.getFusionState(node.edges[0]).getPeekBufferSize() != 0) {
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
	    return false;
	}   
	//not a duplicate splitter
	return true;
    }
    
    

    /**
     * Check that all the data received from the splitter
     * from its inputs i.e, the splitter executes enough to 
     * account for all the data coming into it
     */
    private void checkSplitter(boolean isInit) 
    {
	int itemsSent = 0;  //the number of items sent to the splitter
	int itemsReceived = 0; //the number of items this splitter will receive
	
	if (node.inputs < 1)
	    return;
	
	if (node.incoming[0] != null)
	    itemsSent += StrToRStream.getMult(node.incoming[0], isInit) *
		Util.getItemsPushed(node.incoming[0], node);
	
	
	itemsReceived = StrToRStream.getMult(node, isInit) * 
	    distinctRoundItems();
	    
	assert itemsSent == itemsReceived : "CheckSplitter(" + isInit + "): " + 
	    itemsReceived + " = " + itemsSent;
    }

    public void initTasks(Vector fields, Vector functions,
			  JBlock initFunctionCalls, JBlock main) 
    {
	//if this filter is unnecessary, make sure all the downstream neighbor
	//filter's share the same incoming buffer, the buffer of the splitter...
	if (!necessary) {
	    for (int i = 0; i < node.ways; i++)
		((FilterFusionState)FusionState.getFusionState(node.edges[i])).
		    sharedBufferVar(bufferVarInit[0], bufferVar[0]);
	}
    }
    
    
    
    public JStatement[] getWork(JBlock enclosingBlock, boolean isInit) 
    {
	JBlock statements = new JBlock(null, new JStatement[0], null);
	
	int mult = StrToRStream.getMult(getNode(), isInit);

	if (mult == 0)
	    return statements.getStatementArray();
	
	//add the buffer declaration
	if (getBufferVar(null, isInit) != null)
	    enclosingBlock.addStatementFirst
		(new JVariableDeclarationStatement(null, 
						   getBufferVar(null, isInit),
						   null));
	
	//add the block to do the data reordering
	if (node.isDuplicateSplitter()) {
	    //enclosingBlock.addStatement(getDuplicateCode(enclosingBlock, mult, isInit));
	}
	else {
	    enclosingBlock.addStatement(getRRCode(enclosingBlock, mult, isInit));
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
	    
	    if (!isInit && downstream.getPeekBufferSize() > 0) 
		outgoingIndex = new JAddExpression(null,
						   outgoingIndex,
						   new JIntLiteral(downstream.getPeekBufferSize()));

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
	return  StrToRStream.getMult(node, init) * distinctRoundItems();
    }
    
				 
    public JVariableDefinition getBufferVar(FlatNode prev, boolean init) 
    {
	return init ? bufferVarInit[0] : bufferVar[0];
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
	    
	    if (!isInit && downstream.getPeekBufferSize() > 0) 
		outgoingIndex = new JAddExpression(null,
						   outgoingIndex,
						   new JIntLiteral(downstream.getPeekBufferSize()));

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
    

    private JVariableDefinition makeBuffer(boolean isInit) 
    {
	int mult = StrToRStream.getMult(node, isInit);
	
	int itemsAccessed = mult * distinctRoundItems();

	if (itemsAccessed == 0) {
	    return null;
	}
	
	JExpression[] dims = { new JIntLiteral(null, itemsAccessed) };
	JExpression initializer = 
	    new JNewArrayExpression(null,
				    Util.getOutputType(node),
				    dims,
				    null);
	// make a buffer for all the items looked at in a round
	return new JVariableDefinition(null,
				       at.dms.kjc.Constants.ACC_FINAL,
				       new CArrayType(Util.getOutputType(node),
						      1 /* dimension */ ),
				       BUFFERNAME + myUniqueID,
				       initializer);
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

