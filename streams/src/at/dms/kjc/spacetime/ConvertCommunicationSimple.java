package at.dms.kjc.spacetime;

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
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;

public class ConvertCommunicationSimple extends SLIRReplacingVisitor 
{
    GeneratedVariables generatedVariables;
	
	
    public ConvertCommunicationSimple(GeneratedVariables generateds) 
    {
	generatedVariables = generateds;
    }
	
    //for pop expressions convert to the form
    // (recvBuffer[++recvBufferIndex % recvBufferSize])
    public Object visitPopExpression(SIRPopExpression oldSelf,
				     CType oldTapeType) {
	  
	// do the super
	SIRPopExpression self = 
	    (SIRPopExpression)
	    super.visitPopExpression(oldSelf, oldTapeType);

	//create the increment of the index var
	JPrefixExpression increment = 
	    new JPrefixExpression(null, 
				  OPE_PREINC,
				  new JFieldAccessExpression
				  (null, null, 
				   generatedVariables.simpleIndex.getIdent()));
	    
	//create the array access expression
	JArrayAccessExpression bufferAccess = 
	    new JArrayAccessExpression(null,
				       new JFieldAccessExpression
				       (null, null, 
					generatedVariables.recvBuffer.getIdent()),
				       increment);

	//return the parenthesed expression
	return new JParenthesedExpression(null,
					  bufferAccess);
    }
	
    //convert peek exps into:
    // (recvBuffer[(recvBufferIndex + (arg) + 1) mod recvBufferSize])
    public Object visitPeekExpression(SIRPeekExpression oldSelf,
				      CType oldTapeType,
				      JExpression oldArg) {
	// do the super
	SIRPeekExpression self = 
	    (SIRPeekExpression)
	    super.visitPeekExpression(oldSelf, oldTapeType, oldArg);


	JAddExpression argIncrement = 
	    new JAddExpression(null, self.getArg(), new JIntLiteral(1));
	    
	JAddExpression index = 
	    new JAddExpression(null,
			       new JFieldAccessExpression
			       (null, null,
				generatedVariables.simpleIndex.getIdent()),
			       argIncrement);

	//create the array access expression
	JArrayAccessExpression bufferAccess = 
	    new JArrayAccessExpression(null,
				       new JFieldAccessExpression
				       (null, null, 
					generatedVariables.recvBuffer.getIdent()),
				       index);

	//return the parenthesed expression
	return new JParenthesedExpression(null,
					  bufferAccess);
	    
    }
	
}
    
