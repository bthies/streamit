package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;

//each filter owns its popBuffer, the popBufferIndex, and the pushIndex
//into the next filters popBuffer.
public abstract class FusionState 
{
    private static int uniqueID = 0;
    
    //true if this node *needs* to code generated for correctness
    //false for some identity's and duplicate splitters
    protected boolean necessary = true;

    protected FlatNode node;
    //the number of items remaining on the tape after the 
    //init stage has completed for each incoming channel of the node
    protected int remaining[];
    protected JVariableDefinition[] bufferVar;
    protected static HashMap fusionState;
    
    protected int myUniqueID;
    
    public static String BUFFERNAME = "__INTER_BUFFER_";    
    
    static 
    {
	fusionState = new HashMap();
    }

    public FusionState(FlatNode node)
    {
	this.node = node;
	this.myUniqueID = uniqueID++;
	remaining = new int[Math.max(1, node.inputs)];
	remaining[0] = 0;
    }

    public abstract int getBufferSize(FlatNode prev, boolean init);
    public abstract int getRemaining(FlatNode prev, boolean init);

    public boolean isNecesary() 
    {
	return necessary;
    }
    
    public abstract void initTasks(Vector fields, Vector functions,
				   JBlock initFunctionCalls, JBlock main);
    
    public abstract JStatement[] getWork(JBlock enclosingBlock, boolean isInit);

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
    
    public abstract JVariableDefinition getBufferVar(FlatNode prev, boolean init);
    
    
    public FlatNode getNode() 
    {
	return node;
    }


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
     * Given that a phase has already executed, move the un-pop'ed items
     * to the front of the pop buffer.
     */
    protected JStatement remainingBackupLoop(JVariableDefinition buffer,
					   JVariableDefinition loopCounterBackup,
					   int offset, 
					   int remainingItems)
				    
    {
	if (remainingItems == 0)
	    return new JEmptyStatement(null, null);

	// make a statement that will copy unpopped items into the
	// peek buffer, assuming the counter will count from 0 to peekBufferSize

	// the lhs of the destination of the assignment
	JExpression destLhs = 
	    new JLocalVariableExpression(null,
					 buffer);
	// the rhs of the destination of the assignment
	JExpression destRhs = 
	    new JLocalVariableExpression(null, 
					 loopCounterBackup);

	// the lhs of the source of the assignment
	JExpression sourceLhs = 
	    new JLocalVariableExpression(null,
					 buffer);
	    

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

    protected JVariableDefinition makeBuffer(int bufferSize,
					     CType elementType,
					     String bufferName) 
    {
	if (bufferSize == 0 || elementType == CStdType.Void)
	    return null;
	
	int dim = 1;  // the dimensionality of the pop buffer
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
	
	JExpression initializer = 
	    new JNewArrayExpression(null,
				    bufferType,
				    dims,
				    null);

	return new JVariableDefinition(null,
				       at.dms.kjc.Constants.ACC_FINAL,
				       bufferType,
				       bufferName,
				       initializer);
    }
    

    protected static String BACKUPCOUNTER = "__backup_counter_";



}

