package at.dms.kjc.rstream;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import at.dms.util.SIRPrinter;

/**
 * This visitor will convert the communication expressions of a filter (push, pop, peek)
 * into buffer accesses.  We introduce an incoming buffer for each filter.
 * Pop is converted into an access of the incoming buffer off of a pop counter that is
 * incremented by one for each pop, peek(i) is an access to the incoming buffer at the pop
 * index + i.  Push is converted into a write of the downstream node's incoming buffer that 
 * connects this node to that node, it is indexed by a push counter that is 
 * incremented at each push.
 */
public class ConvertChannelExprs extends SLIRReplacingVisitor {

    private JLocalVariable popBuffer;
    private JLocalVariable popCounter;
    private JLocalVariable pushBuffer;
    private JLocalVariable pushCounter;

    /** construct a new visitor to convert the channel expression of the filter of 
     * current, if init is true we are in the init stage **/
    public ConvertChannelExprs(FilterFusionState current, boolean init)
    {
	SIRFilter filter = (SIRFilter)current.getNode().contents;
	
	this.pushBuffer = null;
	this.pushCounter = null;

	//set the push buffer and the push counter if this filter pushes
	if (current.getNode().ways > 0) {
	    assert current.getNode().ways == 1;
	    //get the downstream incoming buffer
	    this.pushBuffer = current.getPushBufferVar(init);
	    
	    this.pushCounter = current.getPushCounterVar(init);	    
	}
	
	this.popBuffer = current.getBufferVar(null /*this is a filter, so only one previous */,
					      init);
	this.popCounter = current.getPopCounterVar();
	
    }
    
    /** 
     * visit a pop expression, converting the expression to a buffer access 
     **/
    public Object visitPopExpression(SIRPopExpression self,
				     CType tapeType) {
	
	// build ref to pop array
	JLocalVariableExpression lhs = 
	    new JLocalVariableExpression(null, popBuffer);

	// build increment of index to array
	JExpression rhs =
	    new JPrefixExpression(null, 
				  Constants.OPE_PREINC, 
				  new JLocalVariableExpression(null,
							       popCounter));
	// return a new array access expression
	return new JArrayAccessExpression(null, lhs, rhs);
    }
    
    /** 
     * visit a pop expression, converting the expression to a buffer access 
     **/
    public Object visitPeekExpression(SIRPeekExpression oldSelf,
				      CType oldTapeType,
				      JExpression oldArg) {

	// do the super
	SIRPeekExpression self = 
	    (SIRPeekExpression)
	    super.visitPeekExpression(oldSelf, oldTapeType, oldArg);
	
	// build ref to pop array
	JLocalVariableExpression lhs = 
	    new JLocalVariableExpression(null, popBuffer);

	// build subtraction of peek index from current pop index (add
	// one to the pop index because of our pre-inc convention)
	JExpression rhs =
	    new JAddExpression(null,
			       new JAddExpression(null,
						  new JIntLiteral(1),
						  new JLocalVariableExpression(null,
									       popCounter)),
			       self.getArg());

	// return a new array access expression
	return new JArrayAccessExpression(null, lhs, rhs);
    }
    
    /** 
     * visit a push expression, converting the expression to a buffer write
     **/
    public Object visitPushExpression(SIRPushExpression oldSelf,
				      CType oldTapeType,
				      JExpression oldArg) {
	// do the super
	SIRPushExpression self = 
	    (SIRPushExpression)
	    super.visitPushExpression(oldSelf, oldTapeType, oldArg);
	
	// build ref to push array
	JLocalVariableExpression lhs = 
	    new JLocalVariableExpression(null, pushBuffer);

	// build increment of index to array
	JExpression rhs =
	    new JPrefixExpression(null,
				  Constants.OPE_PREINC, 
				  new JLocalVariableExpression(null,
							       pushCounter));
	// return a new array assignment to the right spot
	return new JAssignmentExpression(null,
					 new JArrayAccessExpression(null, lhs, rhs),
					 self.getArg());
    }
}
