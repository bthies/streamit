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
    //flatnode (previous) -> Jvariabledeclaration (buffer) for init
    private HashMap bufferMapInit;
    


    public JoinerFusionState(FlatNode node) 
    {
	super(node);
	
	assert node.isJoiner();
	
	checkJoiner(true);
	checkJoiner(false);

	joiner = (SIRJoiner)node.contents;
	
	bufferVar = new JVariableDefinition[node.inputs];
	bufferVarInit = new JVariableDefinition[node.inputs];

	bufferMap = new HashMap();
	bufferMapInit = new HashMap();
	
	
	makeIncomingBuffers();
    }

    //make sure that all items are passed by the joiner, i.e. nothing 
    //remains "in" the joiner
    private void checkJoiner(boolean isInit) 
    {
	int itemsReceived = 0, itemsSent = 0;
	
	for (int i = 0; i < node.inputs; i++) 
	    itemsReceived += 
		(StrToRStream.getMult(node.incoming[i], isInit) * 
		 Util.getItemsPushed(node.incoming[i], node));
	
	itemsSent = StrToRStream.getMult(node, isInit) *
	    node.getTotalIncomingWeights();

	assert itemsSent == itemsReceived : "Check Joiner " + itemsSent + " = " + itemsReceived;
    }
    
    public void initTasks(Vector fields, Vector functions,
			  JBlock initFunctionCalls, JBlock main) 
    {
	
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

	    enclosingBlock.addStatementFirst
		(new JVariableDeclarationStatement(null, 
						   incomingBuffer,
						   null));

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
	    if (!isInit && downstream.getPeekBufferSize() > 0)
		outgoingIndex = new JAddExpression(null,
						   outgoingIndex,
						   new JIntLiteral(downstream.getPeekBufferSize()));
	    
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
	    innerLoops.addStatement(GenerateCCode.makeForLoop(assignment,
							      innerVar,
							      new JIntLiteral(node.incomingWeights[i])));
	    
	}
	
	statements.addStatement(GenerateCCode.makeForLoop(innerLoops,
							   induction,
							   new JIntLiteral(mult)));
	
	return statements.getStatementArray();
    }

    public JVariableDefinition getBufferVar(FlatNode node, boolean init)
    {
	assert (init ? bufferMapInit : bufferMap).containsKey(node);

	return (JVariableDefinition)(init ? bufferMapInit : bufferMap).get(node);
    }
    
    private void makeIncomingBuffers() 
    {
	for (int i = 0; i < node.inputs; i++) {
	    bufferVarInit[i] = makeIncomingBuffer(node.incoming[i], i, true);
	    bufferMapInit.put(node.incoming[i], bufferVarInit[i]);
	    
	    bufferVar[i] = makeIncomingBuffer(node.incoming[i], i, false);
	    bufferMap.put(node.incoming[i], bufferVar[i]);
	}
    }

    private JVariableDefinition makeIncomingBuffer(FlatNode incoming, int way, boolean isInit) 
    {
	int mult = StrToRStream.getMult(node, isInit);
	
	int itemsAccessed = mult * node.incomingWeights[way];
	
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

    public static String JOINERINNERVAR = "__joiner_inner_";
    public static String JOINERCOUNTER = "__joiner_counter_";
}
