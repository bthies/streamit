package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;

/**
 * This abstract class represents the state necessary for each 
 * FlatNode in the application to be converted to imperative SIR 
 * code by GenerateCCode.  Any state that can be shared over FilterFusionState,
 * JoinerFusionState, and SplitterFusionState is here. Plus it defines a common interface
 * (getWork(), initTasks()) for those nodes.
 *
 * @author Michael Gordon
 */

public abstract class FusionState 
{
    /** the next unique ID to assign to a FusionState object **/
    private static int uniqueID = 0;
    
    /** true if this node *needs* to code generated for correctness
	false for some identity's and duplicate splitters **/
    protected boolean necessary = true;
    /** the flatnode that this object represents **/
    protected FlatNode node;
    /** the number of items remaining on the tape after the 
	init stage has completed for each incoming channel of the node **/
    protected int remaining[];
    /** the size of the incoming buffers for each incoming edge **/
    protected JVariableDefinition[] bufferVar;
    /** a hashmap of all fusionstates created so far, indexed by flatnode **/
    protected static HashMap fusionState;
    /** the unique ID of this fusionstate **/
    protected int myUniqueID;
    /** the variaible prefix of the incoming buffer **/
    public static String BUFFERNAME = "__INTER_BUFFER_";    
    
    static 
    {
	fusionState = new HashMap();
    }

    /** create a new FusionState object that represents *node*, note 
	one should never create fusionstates, they are created using
	getFusionState(). **/
    protected FusionState(FlatNode node)
    {
	this.node = node;
	//set uniqueID
	this.myUniqueID = uniqueID++;
	remaining = new int[Math.max(1, node.inputs)];
	//buf 0 = 0
	remaining[0] = 0;
    }

    /** get the size of the buffer from *prev* to this node **/
    public abstract int getBufferSize(FlatNode prev, boolean init);
    /** get the number of items remaining after the init stage on the 
	incoming buffer from *prev* to this node **/
    public abstract int getRemaining(FlatNode prev, boolean init);

    public boolean isNecesary() 
    {
	return necessary;
    }

    /** called by GenerateCCode so that this node can add any initialization code
	to the application **/
    public abstract void initTasks(Vector fields, Vector functions,
				   JBlock initFunctionCalls, JBlock main);
    
    /** get the block that will perform the init stage (*isInit* = true) or the 
	steady-state (*isInit* = false), add any var decls to *enclosingBlock* **/
    public abstract JStatement[] getWork(JBlock enclosingBlock, boolean isInit);

    /** Given FlatNode *node*, return the fusion state object representing it. 
	This forces only one fusion state to be created for each node. 
	Remember all fusionstates in a hashmap indexed by FlatNode. **/
    public static FusionState getFusionState(FlatNode node) 
    {
	if (!fusionState.containsKey(node)) {
	    if (node.isFilter()) {
		if (StrToRStream.HEADER_FOOTER_PEEK_RESTORE)
		    fusionState.put(node, new FFSPeekBuffer(node));
		else
		    fusionState.put(node, new FFSNoPeekBuffer(node));
	    }
	    
	    else if (node.isJoiner())
		fusionState.put(node, new JoinerFusionState(node));
	    else if (node.isSplitter())
		fusionState.put(node, new SplitterFusionState(node));
	    else
		assert false;
	}
	
	return (FusionState)fusionState.get(node);
    }
    
    /** Return the JVariableDefinition associated with the incoming buffer
	from *prev* to this node. **/
    public abstract JVariableDefinition getBufferVar(FlatNode prev, boolean init);
    
    /** Return the FlatNode associated with this FusionState **/
    public FlatNode getNode() 
    {
	return node;
    }

    /** Returnn a JAssignmentStatement that will assign *value* to 
	*def*. **/
    protected  JStatement intAssignStm(JVariableDefinition def, int value) 
    {
	return new JExpressionStatement
	    (null,
	     new JAssignmentExpression
	     (null, new JLocalVariableExpression(null, def),
	      new JIntLiteral(value)),
	     null);
    }

    /**
     * Return SIR code to move *remainingItems* to the beginning of *buffer* 
     * starting at *offset*, using *loopCounterBackup* as the induction 
     * variable of the loop.
     */
    protected JStatement remainingBackupLoop(JVariableDefinition buffer,
					   JVariableDefinition loopCounterBackup,
					   int offset, 
					   int remainingItems)
				    
    {
	//do nothing if we have nothing to do
	if (remainingItems == 0)
	    return new JEmptyStatement(null, null);

	// make a statement that will copy unpopped items into the
	// peek buffer, assuming the counter will count from 0 to peekBufferSize

	// the lhs of the destination of the assignment
	JExpression destLhs = 
	    new JLocalVariableExpression(null,
					 buffer);
	// the rhs of the destination of the assignment, the index of the dest
	JExpression destRhs = 
	    new JLocalVariableExpression(null, 
					 loopCounterBackup);

	// the lhs of the source of the assignment
	JExpression sourceLhs = 
	    new JLocalVariableExpression(null,
					 buffer);
	    
	//the index of the source
	JExpression sourceRhs = 
	    new
	    JAddExpression(null, 
			   new JLocalVariableExpression(null, 
							loopCounterBackup),
			   new JIntLiteral(offset));
	
	// the expression that copies items from the pop buffer to the
	// peek buffer
	JExpression copyExp = 
	    new JAssignmentExpression(null,
				      new JArrayAccessExpression(null,
								 destLhs,
								 destRhs),
				      new JArrayAccessExpression(null,
								 sourceLhs,
								 sourceRhs));

	// finally we have the body of the loop
	JStatement body = new JExpressionStatement(null, copyExp, null);

	// return a for loop that executes (peek-pop) times.
	return GenerateCCode.makeDoLoop(body,
					loopCounterBackup, 
					new JIntLiteral(remainingItems));
    }
    
    /** 
     * create a JVarDef representing an array of type *elementType* (which itself 
     * can be an array, with *bufferSize* elements and name *bufferName*.
     **/
    protected JVariableDefinition makeBuffer(int bufferSize,
					     CType elementType,
					     String bufferName) 
    {
	if (bufferSize == 0 || elementType == CStdType.Void)
	    return null;
	
	// the dimensionality of the pop buffer
	int dim = 1;

	//the dims of the element type we are passing over the channle
	//for non-array's this will be null
	JExpression[] elementDims = new JExpression[0];
	    
	//we have an array type
	if (elementType.isArrayType()) {
	    elementDims = ((CArrayType)elementType).getDims();
	    dim += elementDims.length;
	}

	JExpression[] dims = new JExpression[dim];
	//set the 0 dim to the size of the buffer
	dims[0] = new JIntLiteral(bufferSize);

	//set the remaining dims to be equal to the dims
	//of the elements, if we have an array
	for (int i = 1; i < dims.length; i++)
	    dims[i] = elementDims[i-1];

	CArrayType bufferType = new CArrayType(elementType, 
					       1);
	
	//create a new array expression to initialize the buffer,
	JExpression initializer = 
	    new JNewArrayExpression(null,
				    bufferType,
				    dims,
				    null);
	//return the var def..
	return new JVariableDefinition(null,
				       at.dms.kjc.Constants.ACC_FINAL,
				       bufferType,
				       bufferName,
				       initializer);
    }
    

    protected static String BACKUPCOUNTER = "__backup_counter_";
}

