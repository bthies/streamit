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
    
    //true if this node *needs* to code generated for correctness
    //false for some identity's and duplicate splitters
    protected boolean necessary = true;

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

    public abstract int getBufferSize(FlatNode prev, boolean init);
    

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

