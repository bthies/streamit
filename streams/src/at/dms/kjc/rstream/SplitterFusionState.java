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

	bufferVar = new JVariableDefinition[1];
	bufferVarInit = new JVariableDefinition[1];
	
	bufferVar[0] = makeBuffer(false);
	bufferVarInit[0] = makeBuffer(true);
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
	
	if (node.incoming[0] != null)
	    itemsSent += StrToRStream.getMult(node.incoming[0], isInit) *
		Util.getItemsPushed(node.incoming[0], node);
	
	
	itemsReceived = StrToRStream.getMult(node, isInit) * 
	    distinctRoundItems();
	    
	assert itemsSent == itemsReceived : "CheckSplitter(" + isInit + "): " + 
	    itemsReceived + " = " + itemsSent;
    }
    
    
    private JVariableDefinition makeBuffer(boolean isInit) 
    {
	int mult = StrToRStream.getMult(node, isInit);
	
	int itemsAccessed = mult * distinctRoundItems();

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

}

