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
 * a joiner FlatNode into imperative SIR code so it can be added to 
 * the application's SIR code.  Essentially, we create an outer loop
 * with trip count equal to the multiplicity of the joiner, inside the loop 
 * we read from each incoming buffer for the number of times given by the weight
 * on the incoming edge corresponding to the buffer and place the items in the
 * single outgoing buffer.
 *
 * @author Michael Gordon
 * 
 */
public class JoinerFusionState extends FusionState
{
    /** the joiner this object represents **/
    private SIRJoiner joiner;
    /** flatnode (previous) -> Jvariabledeclaration (buffer) for steady **/
    private HashMap bufferMap;
  
    /** size of incoming buffers **/
    private int bufferSizes[];

    /** true if parent is a feedback loop and this a feebackloop joiner **/
    private boolean isFBJoiner = false;

    protected JoinerFusionState(FlatNode node) 
    {
	super(node);
	
	assert node.isJoiner();

	joiner = (SIRJoiner)node.contents;
	isFBJoiner = joiner.getParent() instanceof SIRFeedbackLoop;
	
	//calculate the items remaining on the incoming buffers after init
	calculateRemaining();
	//create the incoming buffers array
	bufferVar = new JVariableDefinition[node.inputs];
	//create the incoming bufffer sizes array
	bufferSizes = new int[node.inputs];

	bufferMap = new HashMap();
	//create the incoming buffer vars and set the buffer sizes
	makeIncomingBuffers();
    }

    /** calculate the number of items remaining on the incoming buffers 
     * 	after the initialization stage has finished **/
    private void calculateRemaining()
    {
	//calculate remaining for the input channels
	for (int i = 0; i < node.inputs; i++) {
	    //the number of items sent to this channel from upstream
	    int itemsReceived = 0;
	    //the number of items passed along from this channel by the joiner
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
	    
	    //now add the items from the init path delay
	    //the second incoming way is always the loop back channel
	    if (isFBJoiner && i == 1) {
		SIRFeedbackLoop fbl = ((SIRFeedbackLoop)joiner.getParent());
		//the loopback connect is always index 1
		assert node.inputs == 2 : "Feedbackloop joiner does not have 2 inputs";
		
		itemsReceived += fbl.getDelayInt();
	    }

	    //remaining is items received - the items sent along
	    remaining[i] = itemsReceived - itemsSent;

	    if (remaining[i] > 0) 
		System.out.println("** Items remaining on an incoming joiner buffer " + i);
	    
	    
	    assert remaining[i] >= 0 : 
		"Error: negative value for items remaining after init on channel";
	}
	
    }
    
    /** Perform the initialization tasks of this joiner, including declaring 
     * the incoming buffers and calling the init path function and placing the
     * results in the correct incoming buffer if this joiner is a feedback joiner **/
    public void initTasks(Vector fields, Vector functions,
			  JBlock initFunctionCalls, JBlock main) 
    {
	//add the declarations for all the incoming buffers
	for (int i = 0; i < node.inputs; i++) {
	    if (bufferVar[i] != null) 
		main.addStatementFirst(new JVariableDeclarationStatement(null,
									 bufferVar[i],
									 null));
	}
	//if this is a feedback joiner, we must place the initpath items 
	//in the second incoming buffer, so call the initPath() function and
	//assign the results to the buffer
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
	    JVariableDefinition index = GenerateCCode.newIntLocal(INIT_PATH_INDEX, myUniqueID, 0);
	    //declare the induction variable, not needed if we are generating doloops
	    if (!KjcOptions.doloops) 
		initFunctionCalls.addStatementFirst(new JVariableDeclarationStatement(null,
										      index,
										      null));

	    //create the args for the initpath call
	    JExpression[] args = {new JLocalVariableExpression(null, index)};
	    
	    //the init path call
	    JExpression rhs = new JMethodCallExpression(null,
							new JThisExpression(null),
							initPath.getName(),
							args);
	    //the buffer access
	    JExpression lhs = 
		new JArrayAccessExpression(null,
					   new JLocalVariableExpression(null, loopBackBuf),
					   new JLocalVariableExpression(null, index));
	    
	    JAssignmentExpression assign = new JAssignmentExpression(null,
								     lhs,
								     rhs);
	    //place the assignments in a loop
	    initFunctionCalls.addStatement
		(GenerateCCode.makeDoLoop(new JExpressionStatement(null, assign, null),
					  index,
					  new JIntLiteral(delay)));
	}
    }
    
    /** Construct the code necessary to perform the joining of the incoming buffers 
	as given by the round-robin weights of the joiner **/
    public JStatement[] getWork(JBlock enclosingBlock, boolean isInit) 
    {
	
	JBlock statements = new JBlock(null, new JStatement[0], null);
	//the inner loops the perform the copying for each incoming way
	JBlock innerLoops = new JBlock(null, new JStatement[0], null);

	int mult = StrToRStream.getMult(getNode(), isInit);
	
	//add a comment
	JavaStyleComment[] comment = {new JavaStyleComment(joiner.toString(),
							   true,
							   false,
							   false)};
	statements.addStatement(new JEmptyStatement(null, comment));

	//if we don't execute then just return the comment
	if (mult == 0)
	    return statements.getStatementArray();
	
	assert node.edges[0] != null;
	
	//the single downstream FusionState
	FusionState downstream = FusionState.getFusionState(node.edges[0]);
	//the single outgoing buffer
	JVariableDefinition outgoingBuffer = downstream.getBufferVar(node, isInit);
	//the induction var of the outer loop that counts to mult
	JVariableDefinition induction = 
	    GenerateCCode.newIntLocal(JOINERCOUNTER, myUniqueID, 0);
	
	//add the decl of the induction variable
	if (!KjcOptions.doloops)
	    enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					     (null, induction, null));
	
	//for each incoming way, create the loop that copies the items
	//from its incoming buffer to the outgoing buffer
	for (int i = 0; i < node.inputs; i++) {
	    //the induction var of the inner loop
	    JVariableDefinition innerVar = 
		GenerateCCode.newIntLocal(JOINERINNERVAR + myUniqueID + "_", i, + 0);
	    //add the decl of the induction variable
	    if (!KjcOptions.doloops)
		enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
						 (null, innerVar, null));
	    
	    //do nothing if incoming weight is zero
	    if (node.incomingWeights[i] == 0)
		continue;

	    //get the incoming buffer variable
	    JVariableDefinition incomingBuffer = getBufferVar(node.incoming[i], isInit);
	    
	    
	    //add the code to perform the joining
	    

	    //first create the outgoing buffer access:
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
	    //so if this is not init add the remaining items
	    if (!isInit && downstream.getRemaining(node, isInit) > 0)
		outgoingIndex = new JAddExpression(null,
						   outgoingIndex,
						   new JIntLiteral(downstream.getRemaining(node, isInit)));
	    
	    JArrayAccessExpression outgoingAccess = 
		new JArrayAccessExpression(null,
					   new JLocalVariableExpression(null, outgoingBuffer),
					   outgoingIndex);

	    //now construct the incoming buffer access
	    //incoming[weight * induction + innerVar]
	    JAddExpression incomingIndex = 
		new JAddExpression(null,
				   new JMultExpression(null, 
						       new JIntLiteral(node.incomingWeights[i]),
						       new JLocalVariableExpression(null,
										    induction)),
				   new JLocalVariableExpression(null,
								innerVar));
	    	   
	    JArrayAccessExpression incomingAccess = 
		new JArrayAccessExpression(null,
					   new JLocalVariableExpression(null, incomingBuffer),
					   incomingIndex);


	    //create the assignment expression
	    JExpressionStatement assignment = 
		new JExpressionStatement(null,
					 new JAssignmentExpression(null,
								   outgoingAccess,
								   incomingAccess),
					 null);

	    //loop the assign statement based on the weight of this incoming way
	    innerLoops.addStatement(GenerateCCode.makeDoLoop(assignment,
							      innerVar,
							      new JIntLiteral(node.incomingWeights[i])));
	    
	}
	//now make an outer do loop with trip count equal to the multiplicity of this joiner in this stage
	statements.addStatement(GenerateCCode.makeDoLoop(innerLoops,
							 induction,
							 new JIntLiteral(mult)));
	

	//now for each incoming way, we must
	//move any remaining items on an input tape to the beginning of the tape 
	//if there are remaining items, this items were not cosumed by the joiner 
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

    /** return the var def of the incoming (pop) buffer from *node* to this joiner **/
    public JVariableDefinition getBufferVar(FlatNode node, boolean init)
    {
	assert bufferMap.containsKey(node);

	return (JVariableDefinition) bufferMap.get(node);
    }
    
    /** create the incoming buffers JVariableDeclaration **/
    private void makeIncomingBuffers() 
    {
	for (int i = 0; i < node.inputs; i++) {
	    //use the same buffer for both init and steady
	    bufferVar[i] = makeIncomingBuffer(node.incoming[i], i);
	    bufferMap.put(node.incoming[i], bufferVar[i]);
	}
    }
    /** return the buffer size for the incoming buffer from *prev* to this node **/
    public int getBufferSize(FlatNode prev, boolean init) 
    {
	return bufferSizes[node.getIncomingWay(prev)];
    }
    
    /** make the incoming buffer representing the buffer from *incoming* to this node,
	at *way* index of the incoming edges **/
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
	
	assert itemsAccessed >= 0;
	bufferSizes[way] = itemsAccessed;
	

	if (itemsAccessed == 0)
	    return null;

	return makeBuffer(itemsAccessed, 
			  Util.getOutputType(node),
			  BUFFERNAME + "_" + myUniqueID+ "_" + way);
    }

    /** return the items remaining (after the init stage) 
	on the incoming buffer from *prev* to this node.
	remember to add the initpath items that were not consumed 
	by this joiner if it is a feedback joiner **/
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
