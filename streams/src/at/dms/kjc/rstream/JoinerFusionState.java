package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;

//each filter owns its popBuffer, the popBufferIndex, and the pushIndex
//into the next filters popBuffer.
public class JoinerFusionState extends FusionState
{
    private SIRJoiner joiner;
    //flatnode (previous) -> Jvariabledeclaration (buffer) for steady
    private HashMap bufferMap;
  
    //size of incoming buffers
    int bufferSizes[];

    //true if parent is a feedback loop
    boolean isFBJoiner = false;

    public JoinerFusionState(FlatNode node) 
    {
	super(node);
	
	assert node.isJoiner();

	joiner = (SIRJoiner)node.contents;
	isFBJoiner = joiner.getParent() instanceof SIRFeedbackLoop;
	
	calculateRemaining();
	
	//share the buffers between init and steady...
	bufferVar = new JVariableDefinition[node.inputs];
	
	bufferSizes = new int[node.inputs];

	bufferMap = new HashMap();
		
	makeIncomingBuffers();
    }

    //make sure that all items are passed by the joiner, i.e. nothing 
    //remains "in" the joiner
    private void calculateRemaining()
    {
	//calculate remaining for the input channels
	for (int i = 0; i < node.inputs; i++) {
	    //the number of items sent on this channel from upstream
	    int itemsReceived = 0;
	    //the number of items pass along from this channel
	    int itemsSent = 0;
	    
	    if (node.incoming[i] == null) {
		remaining[i] = 0;
		continue;
	    }
	    
	    itemsReceived = 
		(StrToRStream.getMult(node.incoming[i], true) * 
		 Util.getItemsPushed(node.incoming[i], node));
	
	    itemsSent = StrToRStream.getMult(node, true) *
		node.incomingWeights[i];
	    
	    //now add the items from added from the init path delay that are not
	    //consumed, the second incoming way is always the loop back channel
	    if (isFBJoiner && i == 1) {
		SIRFeedbackLoop fbl = ((SIRFeedbackLoop)joiner.getParent());
		//the loopback connect is always index 1
		assert node.inputs == 2 : "Feedbackloop joiner does not have 2 inputs";
		
		itemsReceived += fbl.getDelayInt();
	    }

	    remaining[i] = itemsReceived - itemsSent;

	    if (remaining[i] > 0) 
		System.out.println("** Items remaining on an incoming joiner buffer " + i);
	    
	    
	    assert remaining[i] >= 0 : 
		"Error: negative value for items remaining after init on channel";
	}
	
    }
    
    public void initTasks(Vector fields, Vector functions,
			  JBlock initFunctionCalls, JBlock main) 
    {
	//add the declarations for all the incoming buffers
	for (int i = 0; i < node.inputs; i++) {
	    main.addStatementFirst(new JVariableDeclarationStatement(null,
								     bufferVar[i],
								     null));
	}

	if (isFBJoiner) {
	    SIRFeedbackLoop loop = (SIRFeedbackLoop)joiner.getParent();
	    int delay = loop.getDelayInt();

	    //don't do anything if delay is 0
	    if (delay < 1)
		return;

	    //	    System.out.println("Constructing initPath call for " + joiner);
	    
	   

	    //if this joiner is a feedbackloop joiner, 
	    //create code to place the initPath items
	    //in its loopback buffer
	    JVariableDefinition loopBackBuf = bufferVar[1];

	    
	    assert loopBackBuf != null;
	   
	    //add the initpath function to the function list
	    functions.add(loop.getInitPath());
 
	    JMethodDeclaration initPath = loop.getInitPath();
	    //create the induction variable
	    JVariableDefinition index = GenerateCCode.newIntLocal(INIT_PATH_INDEX, uniqueID, 0);
	    //declare the induction variable
	    initFunctionCalls.addStatementFirst(new JVariableDeclarationStatement(null,
										  index,
										  null));

	    //create the args for the initpath call
	    JExpression[] args = {new JLocalVariableExpression(null, index)};
	    
		    
	    JExpression rhs = new JMethodCallExpression(null,
							new JThisExpression(null),
							initPath.getName(),
							args);
	    JExpression lhs = 
		new JArrayAccessExpression(null,
					   new JLocalVariableExpression(null, loopBackBuf),
					   new JLocalVariableExpression(null, index));
	    
	    JAssignmentExpression assign = new JAssignmentExpression(null,
								     lhs,
								     rhs);
	    initFunctionCalls.addStatement
		(GenerateCCode.makeDoLoop(new JExpressionStatement(null, assign, null),
					  index,
					  new JIntLiteral(delay)));
	}
    }
    
    
    public JStatement[] getWork(JBlock enclosingBlock, boolean isInit) 
    {
	JBlock statements = new JBlock(null, new JStatement[0], null);
	JBlock innerLoops = new JBlock(null, new JStatement[0], null);

	int mult = StrToRStream.getMult(getNode(), isInit);

	if (mult == 0)
	    return statements.getStatementArray();
	
	assert node.edges[0] != null;

	FusionState downstream = FusionState.getFusionState(node.edges[0]);

	JVariableDefinition outgoingBuffer = downstream.getBufferVar(node, isInit);

	JVariableDefinition induction = 
	    GenerateCCode.newIntLocal(JOINERCOUNTER, myUniqueID, 0);
	
	//add the decl of the induction variable
	enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					(null, induction, null));
	
	for (int i = 0; i < node.inputs; i++) {
	    JVariableDefinition innerVar = 
		GenerateCCode.newIntLocal(JOINERINNERVAR + myUniqueID + "_", i, + 0);
	    //add the decl of the induction variable
	    enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					     (null, innerVar, null));
	    
	    //add the decls for the buffers
	    JVariableDefinition incomingBuffer = getBufferVar(node.incoming[i], isInit);
	    
	    
	    //add the code to perform the joining
	    
	    //outgoing[induction * totalWeights + partialSum + innerVar] if init
	    //outgoing[induction * totalWeights + partialSum + innerVar + peekBufferSize_of_outgoing] 
	    //if steady
	    JAddExpression outgoingIndex = 
		new JAddExpression(null,
				   new JMultExpression(null,
						       new JLocalVariableExpression(null,
										    induction),
						       new JIntLiteral(node.getTotalIncomingWeights())),
				   new JAddExpression(null,
						      new JIntLiteral(node.getPartialIncomingSum(i)),
						      new JLocalVariableExpression(null,
										   innerVar)));
	    if (!isInit && downstream.getRemaining(node, isInit) > 0)
		outgoingIndex = new JAddExpression(null,
						   outgoingIndex,
						   new JIntLiteral(downstream.getRemaining(node, isInit)));
	    
	    //incoming[weight * induction + innerVar]
	    JAddExpression incomingIndex = 
		new JAddExpression(null,
				   new JMultExpression(null, 
						       new JIntLiteral(node.incomingWeights[i]),
						       new JLocalVariableExpression(null,
										    induction)),
				   new JLocalVariableExpression(null,
								innerVar));
	    
	    JArrayAccessExpression outgoingAccess = 
		new JArrayAccessExpression(null,
					   new JLocalVariableExpression(null, outgoingBuffer),
					   outgoingIndex);
	    
	    JArrayAccessExpression incomingAccess = 
		new JArrayAccessExpression(null,
					   new JLocalVariableExpression(null, incomingBuffer),
					   incomingIndex);

	    JExpressionStatement assignment = 
		new JExpressionStatement(null,
					 new JAssignmentExpression(null,
								   outgoingAccess,
								   incomingAccess),
					 null);
	    innerLoops.addStatement(GenerateCCode.makeDoLoop(assignment,
							      innerVar,
							      new JIntLiteral(node.incomingWeights[i])));
	    
	}
	
	statements.addStatement(GenerateCCode.makeDoLoop(innerLoops,
							 induction,
							 new JIntLiteral(mult)));
	
	//move any remaining items on an input tape to the beginning of the tape 
	for (int i = 0; i < node.inputs; i++) {
	    int offset = StrToRStream.getMult(node, isInit) *
		node.incomingWeights[i];  //offset, consumed
	    
	    //System.out.println(getRemaining(node.incoming[i]) + " " + offset);
	    
	    if (getRemaining(node.incoming[i], isInit) > 0 && offset > 0) {
		//just reuse the joiner buffer counter from above
		statements.addStatement(remainingBackupLoop(getBufferVar(node.incoming[i], isInit),
							    induction,
							    offset,
							    getRemaining(node.incoming[i], isInit))); 
                                                            //number of items
						   
	    }
	}
	
	return statements.getStatementArray();
    }

    public JVariableDefinition getBufferVar(FlatNode node, boolean init)
    {
	assert bufferMap.containsKey(node);

	return (JVariableDefinition) bufferMap.get(node);
    }
    
    private void makeIncomingBuffers() 
    {
	for (int i = 0; i < node.inputs; i++) {
	    //use the same buffer for both init and steady
	    bufferVar[i] = makeIncomingBuffer(node.incoming[i], i);
	    bufferMap.put(node.incoming[i], bufferVar[i]);
	}
    }

    public int getBufferSize(FlatNode prev, boolean init) 
    {
	return bufferSizes[node.getIncomingWay(prev)];
    }
    

    private JVariableDefinition makeIncomingBuffer(FlatNode incoming, int way)
    {
	//use the stage multiplicity that has the largest value
	int mult = Math.max(StrToRStream.getMult(node, false),
			    StrToRStream.getMult(node, true));
	
	int itemsAccessed = mult * node.incomingWeights[way];

	//account for the initpath data in the buffer size calculation
	if (isFBJoiner) {
	    itemsAccessed = Math.max(itemsAccessed, 
				     ((SIRFeedbackLoop)joiner.getParent()).getDelayInt());
	}
	

	bufferSizes[way] = itemsAccessed;
	
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
				       BUFFERNAME + "_" + myUniqueID+ "_" + way,
				       initializer);
    }

    public int getRemaining(FlatNode prev, boolean isInit) 
    {
	//if this is the feedback incoming edge of a feedback loop joiner
	//then the joiner executes before the incoming edge's source
	//so remove these items from the remaining calculation
	if (!isInit && node.isFeedbackIncomingEdge(node.getIncomingWay(prev))) {
	    return remaining[node.getIncomingWay(prev)] - (StrToRStream.getMult(node, isInit) *
							   node.getIncomingWeight(prev));
	}
	
	
	return remaining[node.getIncomingWay(prev)];
    }
    

    public static String JOINERINNERVAR = "__joiner_inner_";
    public static String JOINERCOUNTER = "__joiner_counter_";
    public static String INIT_PATH_INDEX = "__init_path_index_";
    public static String INIT_PATH_COPY = "__init_path_copy_";
}
