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
				       BUFFERNAME + myUniqueID,
				       initializer);
    }
}
