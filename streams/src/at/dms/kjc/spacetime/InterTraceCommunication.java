package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;

/** 
 * This class generates the inter-trace send code for a trace
 **/
public class InterTraceCommunication extends SLIRReplacingVisitor 
{
    FilterInfo filterInfo;
    FilterInfo[] upstream;

    public InterTraceCommunication(FilterInfo filterInfo) 
    {
	this.filterInfo = filterInfo;
	FilterTraceNode[] upNodes = filterInfo.getNextFilters();
	upstream = new FilterInfo[upNodes.length];
	for (int i = 0; i < upNodes.length; i++) {
	    upstream[i] = FilterInfo.getFilterInfo(upNodes[i]);
	}
    }
    
    public Object visitPushExpression(SIRPushExpression self,
				      CType tapeType,
				      JExpression arg) {
	JExpression newExp = (JExpression)arg.accept(this);
	JAssignmentExpression bufExp = null;

	if (upstream.length == 1) {
	    JFieldAccessExpression fieldBuf =
		new JFieldAccessExpression(null, new JThisExpression(null),
					   RawExecutionCode.recvBuffer +
					   upstream[0].filter.getName());

	    if (upstream[0].isSimple()) {
		JArrayAccessExpression bufAccess = 
		    new JArrayAccessExpression(null,
					       fieldBuf,
					       new JPrefixExpression(null, OPE_PREINC,
								     new JFieldAccessExpression
								     (null, new JThisExpression(null),
								      RawExecutionCode.simpleIndex +
								      upstream[0].filter.getName())));
		bufExp = new JAssignmentExpression(null,
						   bufAccess,
						   newExp);
	    }
	    else {
		//increment the end of the buffer
		JPrefixExpression bufferIncrement = 
		    new JPrefixExpression(null, 
					  OPE_PREINC,
					  new JFieldAccessExpression
					  (null, new JThisExpression(null),
					   RawExecutionCode.recvIndex + upstream[0].filter.getName()));
		
		//create the and expression
		JBitwiseExpression indexAnd = 
		    new JBitwiseExpression(null, 
					   OPE_BAND,
					   bufferIncrement, 
					   new JFieldAccessExpression
					   (null, new JThisExpression(null),
					    RawExecutionCode.recvBufferBits + upstream[0].filter.getName()));
		
		//access the buffer
		JArrayAccessExpression bufAccess = 
		    new JArrayAccessExpression(null,
					       fieldBuf,
					       indexAnd);
					       
		bufExp = new JAssignmentExpression(null,
						   bufAccess,
						   newExp);
	    }
	}
	else 
	    Utils.fail("split joins not supported");
	
	return bufExp;
    }
}
