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
    protected static int uniqueID = 0;
    
    
    protected FlatNode node;
    protected int peekBufferSize;
    protected JVariableDefinition[] bufferVar;
    protected JVariableDefinition[] bufferVarInit;
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
	peekBufferSize = 0;
    }

    public abstract void initTasks(Vector fields, Vector functions,
				   JBlock initFunctionCalls, JBlock main);
    
    public abstract JStatement[] getWork(JBlock enclosingBlock, boolean isInit);

    public static FusionState getFusionState(FlatNode node) 
    {
	if (!fusionState.containsKey(node)) {
	    if (node.isFilter())
		fusionState.put(node, new FilterFusionState(node));
	    else if (node.isJoiner())
		fusionState.put(node, new JoinerFusionState(node));
	    else if (node.isSplitter())
		fusionState.put(node, new SplitterFusionState(node));
	    else
		assert false;
	}
	
	return (FusionState)fusionState.get(node);
    }
    
    //for splitters and filters ignore prev, this is overrided for joiners
    public JVariableDefinition getBufferVar(FlatNode prev, boolean init)
    {
	return init ? bufferVarInit[0] : bufferVar[0];
    }


    public int getPeekBufferSize() 
    {
	return peekBufferSize;
    }
    
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
}

