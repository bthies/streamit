package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;

/**
 * This class represents a filter flatnode's fusion state that is 
 * shared over the 2 different SIR code generation schemes.  
 *
 *
 * @author Michael Gordon 
 * 
 */
public abstract class FilterFusionState extends FusionState
{
    /** prefix of the pop counter, if necessary **/
    protected static String POPCOUNTERNAME = "__POP_COUNTER_";
    /** prefix of the push counter, if necessary **/
    protected static String PUSHCOUNTERNAME = "__PUSH_COUNTER_";
    /** prefix of the induction var for the outer loop of the stage **/
    protected static String FORINDEXNAME = "__work_counter_";
    /** index variables indexing the different buffers **/
    protected JVariableDefinition popCounterVar;
    protected JVariableDefinition pushCounterVar;
    /** we have separate init and steady push counters because in the
	steady state, the push buffer is initialized to the downstream's 
	remaining items **/
    protected JVariableDefinition pushCounterVarInit;
    /** the induction variable for the outer loop **/
    protected JVariableDefinition loopCounterVar;
    /** the filter this FFS represents **/
    protected SIRFilter filter;

    /**if this is true, don't generate the declaration of the pop buffer,
       this is set by a duplicate splitter if this filter shares its buffer
       with other filters to implement the duplication **/
    protected boolean dontGeneratePopDecl = false;

    /** this will create both the init and the steady buffer **/
    protected FilterFusionState(FlatNode fnode)
    {
	super(fnode);

	filter = (SIRFilter)node.contents;

	//two stage filters are currently only introduced by partitioning 
	//so we should not see them, and we don't handle them
	assert !(filter instanceof SIRTwoStageFilter);
	
	assert node.ways <= 1 : "Filter FlatNode with more than one outgoing buffer";	    
	
	bufferVar = new JVariableDefinition[1];
    }
    
    /** return the number of items remaining on the pop buffer after this 
	filter has fired in the initialization stage **/
    public int getRemaining(FlatNode prev, boolean isInit) 
    {
	//if this filter is not necessary then return the downstream's
	//remaining count
	if (!necessary && node.ways > 0)
	    return FusionState.getFusionState(node.edges[0]).getRemaining(node, isInit);
	
	return remaining[0];
    }
    
    /** get the var representing the pop (incoming) buffer index **/
    public JVariableDefinition getPopCounterVar() 
    {
	return popCounterVar;
    }
    
    /** get the var representuing the push (outgoing) buffer index **/
    public JVariableDefinition getPushCounterVar(boolean isInit) 
    {
	return isInit ? pushCounterVarInit : pushCounterVar;
    }

    /** return the incoming buffer size for this filter **/
    public abstract int getBufferSize(FlatNode prev, boolean init);
    
    /** return the buffer var representing the incoming buffer for this filter **/
    public JVariableDefinition getBufferVar(FlatNode prev, boolean init) 
    {
	//if this filter is not necessary, return the downstream's incoming buffer var
	if (!necessary && node.ways > 0) {
	    return FusionState.getFusionState(node.edges[0]).getBufferVar(node, init);
	}
	return bufferVar[0];
    }
    
    /** Set the incoming (pop) buffer var of this filter to be *buf*.
	This is called by an unnecesary duplicate splitters to make sure that 
	its downstream neighbors share the same incoming buffer **/
    public void sharedBufferVar(JVariableDefinition buf)
    {
	dontGeneratePopDecl = true;
	bufferVar[0] = buf;
    }
    
    /** return the variable representing the push (outgoing) buffer of the filter **/
    public JVariableDefinition getPushBufferVar(boolean isInit)  
    {
	assert node.ways == 1;
	
	return getFusionState(node.edges[0]).getBufferVar(node, isInit);
    }
    
    /** Add any necessary initialization tasks to the SIR code containers **/
    public abstract void initTasks(Vector fields, Vector functions,
			  JBlock initFunctionCalls, JBlock main);
    
    /** get the SIR block representing the imperative code necessary to execute 
	this filter in the init stage (*isInit* == true) or the steady-state 
	stage (*isInit* == false)
    **/
    public abstract JStatement[] getWork(JBlock enclosingBlock, boolean isInit);
    
    /** 
	Check helper function *meth* (not init or work) for field accesses.
	Field accesses in helper functions are not supported at this time.
    **/
    protected void checkHelperFunction(JMethodDeclaration meth) 
    {
	//check the method for field accessed
	meth.accept(new SLIREmptyVisitor() {
		public void visitFieldExpression(JFieldAccessExpression self,
						 JExpression left,
						 String ident)
		{
		    assert false : "Field accesses in helper functions not supported at this time.";
		}
		
	    });
    }
    
    /** return the filter associated with this fitler fusion state object **/
    public SIRFilter getFilter() 
    {
	return filter;
    }
}
