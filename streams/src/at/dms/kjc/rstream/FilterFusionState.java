package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;

//each filter owns its popBuffer, the popBufferIndex, and the pushIndex
//into the next filters popBuffer.
public abstract class FilterFusionState extends FusionState
{

    protected static String POPCOUNTERNAME = "__POP_COUNTER_";
    protected static String PUSHCOUNTERNAME = "__PUSH_COUNTER_";

    protected static String FORINDEXNAME = "__work_counter_";
    protected static String BACKUPCOUNTER = "__backup_counter_";

    protected JVariableDefinition popCounterVar;
    protected JVariableDefinition pushCounterVar;
    protected JVariableDefinition pushCounterVarInit;
    protected JVariableDefinition loopCounterVar;

    protected SIRFilter filter;

    //if this is true, don't generate the declaration of the pop buffer,
    //this is set by a duplicate splitter if this filter shares its buffer
    //with other filters to implement the duplication
    protected boolean dontGeneratePopDecl = false;



    /** this will create both the init and the steady buffer **/
    public FilterFusionState(FlatNode fnode)
    {
	super(fnode);

	filter = (SIRFilter)node.contents;

	//two stage filters are currently only introduced by partitioning 
	assert !(filter instanceof SIRTwoStageFilter);
	
	assert node.ways <= 1 : "Filter FlatNode with more than one outgoing buffer";	    
	
	bufferVar = new JVariableDefinition[1];
	bufferVarInit = new JVariableDefinition[1];
    }
    
    public JVariableDefinition getPopCounterVar() 
    {
	return popCounterVar;
    }
    
    
    public JVariableDefinition getPushCounterVar(boolean isInit) 
    {
	return isInit ? pushCounterVarInit : pushCounterVar;
    }

    public abstract int getBufferSize(FlatNode prev, boolean init);
    

    public JVariableDefinition getBufferVar(FlatNode prev, boolean init) 
    {
	if (!necessary) {
	    return FusionState.getFusionState(node.edges[0]).getBufferVar(node, init);
	}
	return init ? bufferVarInit[0] : bufferVar[0];
    }
    
    //this is called by an unnecesary duplicate splitters to make sure that 
    //all its downstream neighbors share the same incoming buffer
    public void sharedBufferVar(JVariableDefinition bufInit,
				JVariableDefinition buf)
    {
	dontGeneratePopDecl = true;
	bufferVarInit[0] = bufInit;
	bufferVar[0] = buf;
    }
    
    public JVariableDefinition getPushBufferVar(boolean isInit)  
    {
	assert node.ways == 1;
	
	return getFusionState(node.edges[0]).getBufferVar(node, isInit);
    }
    
    public abstract void initTasks(Vector fields, Vector functions,
			  JBlock initFunctionCalls, JBlock main);
    
    public abstract JStatement[] getWork(JBlock enclosingBlock, boolean isInit);
    
    public SIRFilter getFilter() 
    {
	return filter;
    }
}
