package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;

//if 
//not 2 stage
//peek == pop
//no peek expression 
//all pops before pushe

public class DirectCommunication extends at.dms.util.Utils 
    implements Constants 
{
    public static boolean doit(SIRFilter filter) 
    {
	//runs some tests to see if we can 
	//generate code direct commmunication code
	if (KjcOptions.ratematch)
	    return false;
	if (filter instanceof SIRTwoStageFilter)
	    return false;
	if (filter.getPeekInt() > filter.getPopInt())
	    return false;
	if (CommunicationOutsideWork.check(filter))
	    return false;
	if (PeekFinder.findPeek(filter.getWork()))
	    return false;
	if (PushBeforePop.check(filter.getWork()))
	    return false;
	//must popping a scalar
	if (filter.getInputType().isClassType() ||
	    filter.getInputType().isArrayType())
	    return false;
	//must be pushing a scalar
	if (filter.getOutputType().isClassType() ||
	    filter.getOutputType().isArrayType())
	    return false;
	//all tests pass
	
	//convert the communication
	//all the communication is in the work function
	filter.getWork().accept(new DirectConvertCommunication());
	//generate the raw main function 
	rawMainFunction(filter);
	return true;
    }
    
    private static void rawMainFunction(SIRFilter filter) 
    {
	JBlock statements = new JBlock();

	//create the params list, for some reason 
	//calling toArray() on the list breaks a later pass
	List paramList = filter.getParams();
	JExpression[] paramArray;
	if (paramList == null || paramList.size() == 0)
	    paramArray = new JExpression[0];
	else
	    paramArray = (JExpression[])paramList.toArray(new JExpression[0]);
	
	//add the call to the init function
	statements.addStatement
	    (new 
	     JExpressionStatement(null,
				  new JMethodCallExpression
				  (null,
				   new JThisExpression(null),
				   filter.getInit().getName(),
				   paramArray),
				  null));
	
	//inline the work function in a while loop
	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.
	    deepCopy(filter.getWork().getBody());

	if (RawBackend.FILTER_DEBUG_MODE) {
	    statements.addStatement
		(new SIRPrintStatement(null,
				       new JStringLiteral(null, filter.getName() + " Starting Steady-State"),
				       null));
	}

	//if we are in decoupled mode do not put the work function in a for loop
	//and add the print statements
	if (KjcOptions.decoupled) {
	    workBlock.addStatementFirst
		(new SIRPrintStatement(null, 
				       new JIntLiteral(0),
				       null));
	    workBlock.addStatement(workBlock.size(), 
				   new SIRPrintStatement(null, 
							 new JIntLiteral(1),
							 null));
	    statements.addStatement(workBlock);
	}
	else {
	    statements.addStatement
		(new JWhileStatement(null, 
				     new JBooleanLiteral(null, true),
				     workBlock, 
				     null));
	}
	
	JMethodDeclaration rawMainFunct = 
	    new JMethodDeclaration(null, 
				   at.dms.kjc.Constants.ACC_PUBLIC,
				   CStdType.Void,
				   RawExecutionCode.rawMain,
				   JFormalParameter.EMPTY,
				   CClassType.EMPTY,
				   statements,
				   null,
				   null);
	filter.addMethod(rawMainFunct);
	     
    }

    static class DirectConvertCommunication extends SLIRReplacingVisitor 
    {
	public Object visitAssignmentExpression(JAssignmentExpression oldself,
						JExpression oldleft,
						JExpression oldright) 
	{
	    //a little optimization, use the pointer version of the 
	    //structure's pop in struct.h to avoid copying		
	    if (oldright instanceof JCastExpression && 
		(((JCastExpression)oldright).getExpr() instanceof SIRPopExpression)) {
		SIRPopExpression pop = (SIRPopExpression)((JCastExpression)oldright).getExpr();
	    
		if (pop.getType().isClassType()) {
		    JExpression left = (JExpression)oldleft.accept(this);
		
		    JExpression[] arg = 
			{left};
		
		    return new JMethodCallExpression(null, new JThisExpression(null), 
						     RawExecutionCode.structReceiveMethodPrefix + 
						     pop.getType(),
						     arg);
		} 
		if (pop.getType().isArrayType()) {
		    return null;
		}
	    }

	    //otherwise do the normal thing
	    JExpression self = (JExpression)super.visitAssignmentExpression(oldself,
									    oldleft, 
									    oldright);
	    return self;
	}
    

	public Object visitPopExpression(SIRPopExpression oldSelf,
					 CType oldTapeType) {
	
	    // do the super
	    SIRPopExpression self = 
		(SIRPopExpression)
		super.visitPopExpression(oldSelf, oldTapeType);  

	    //if this is a struct, use the struct's pop method, generated in struct.h
	    if (self.getType().isClassType()) {
		return new JMethodCallExpression(null, new JThisExpression(null), 
						 "pop" + self.getType(), 
						 new JExpression[0]);
	    }
	    else if (self.getType().isArrayType()) {
		return null;
	    }
	    else {
		//I am keeping it the was it is because we should use static_receive
		//instead of receiving to memory as in the code in Util
		if (KjcOptions.altcodegen || KjcOptions.decoupled) 
		    return altCodeGen(self);
		else
		    return normalCodeGen(self);
	    }
	}
    
    
	private Object altCodeGen(SIRPopExpression self) {
	    //direct communcation is only generated if the input/output types are scalar
	    if (self.getType().isFloatingPoint())
		return 
		    new JLocalVariableExpression(null,
						 new JGeneratedLocalVariable(null, 0, 
									     CStdType.Float, 
									     Util.CSTIFPVAR,
									     null));
	    else 
		return 
		    new JLocalVariableExpression(null,
						 new JGeneratedLocalVariable(null, 0, 
									     CStdType.Integer,
									     Util.CSTIINTVAR,
									     null));
	}
    
	private Object normalCodeGen(SIRPopExpression self) {
	    String floatSuffix;
	
	    floatSuffix = "";

	    //append the _f if this pop expression pops floats
	    if (self.getType().isFloatingPoint())
		floatSuffix = "_f";
	
	    //create the method call for static_receive()
	    JMethodCallExpression static_receive = 
		new JMethodCallExpression(null, new JThisExpression(null),
					  "static_receive" + floatSuffix, 
					  new JExpression[0]);
	    //store the type in a var that I added to methoddeclaration
	    static_receive.setTapeType(self.getType());
	
	    return static_receive;
	}

	public Object visitPeekExpression(SIRPeekExpression oldSelf,
					  CType oldTapeType,
					  JExpression oldArg) {
	    Utils.fail("Should not see a peek expression when generating " +
		       "direct communication");
	    return null;
	}
    }


    static class CommunicationOutsideWork extends SLIREmptyVisitor 
    {
	private static boolean found;
    
	//returns true if we find communication statements/expressions
	//outside of the work function (i.e. in a helper function)
	public static boolean check(SIRFilter filter) 
	{
	    for (int i = 0; i < filter.getMethods().length; i++) {
		if (!filter.getMethods()[i].equals(filter.getWork())) {
		    found = false;
		    filter.getMethods()[i].accept(new CommunicationOutsideWork());
		    if (found)
			return true;
		}
	    }
	    return false;
	}

	public void visitPeekExpression(SIRPeekExpression self,
					CType tapeType,
					JExpression arg) {
	    found = true;
	}

	public void visitPopExpression(SIRPopExpression self,
				       CType tapeType) {
	    found = true;
	}

	public void visitPushExpression(SIRPushExpression self,
					CType tapeType,
					JExpression arg) {
	    arg.accept(this);
	}
    }


	    

    static class PushBeforePop extends SLIREmptyVisitor 
    {
	private static boolean sawPush;
	private static boolean pushBeforePop;

	public static boolean check(JMethodDeclaration method) 
	{
	    sawPush = false;
	    pushBeforePop = false;
	
	    method.accept(new PushBeforePop());
	    return pushBeforePop;
	}

	public void visitPeekExpression(SIRPeekExpression self,
					CType tapeType,
					JExpression arg) {
	    Utils.fail("Should not see a peek expression");
	}

	public void visitPopExpression(SIRPopExpression self,
				       CType tapeType) {
	    if (sawPush)
		pushBeforePop = true;
	}

	public void visitPushExpression(SIRPushExpression self,
					CType tapeType,
					JExpression arg) {
	    sawPush = true;
	}
    
	//for all loops, visit the cond and body twice to make sure that 
	//if a push statement occurs in the body and 
	//after all the pops, we will flag this as a 
	//case where a push comes before a pop
    
    
	public void visitWhileStatement(JWhileStatement self,
					JExpression cond,
					JStatement body) {
	    cond.accept(this);
	    body.accept(this);
	    //second pass
	    cond.accept(this);
	    body.accept(this);
	}

	public void visitForStatement(JForStatement self,
				      JStatement init,
				      JExpression cond,
				      JStatement incr,
				      JStatement body) {
	    if (init != null) {
		init.accept(this);
	    }
	    if (cond != null) {
		cond.accept(this);
	    }
	    if (incr != null) {
		incr.accept(this);
	    }
	    body.accept(this);
	    //second pass
	    if (cond != null) {
		cond.accept(this);
	    }
	    if (incr != null) {
		incr.accept(this);
	    }
	    body.accept(this);
	}

	public void visitDoStatement(JDoStatement self,
				     JExpression cond,
				     JStatement body) {
	    body.accept(this);
	    cond.accept(this);
	    //second pass
	    body.accept(this);
	    cond.accept(this);
	}
    }


    static class PeekFinder extends SLIREmptyVisitor 
    {
	private static boolean found;

	public static boolean findPeek(JMethodDeclaration method) 
	{
	    found = false;
	    method.accept(new PeekFinder());
	    return found;
	}
    
	/**
	 * if we find a peek expression set found to true;
	 */
	public void visitPeekExpression(SIRPeekExpression self,
					CType tapeType,
					JExpression arg) {
	    found = true;
	}
    }
}
