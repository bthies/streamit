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
import java.util.Vector;
import java.util.HashSet;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;
import at.dms.kjc.flatgraph2.FilterContent;
import at.dms.util.SIRPrinter;

//if 
//not 2 stage
//peek == pop
//no peek expression 
//all pops before pushe

public class DirectCommunication extends RawExecutionCode 
    implements Constants 
{ 
    public static boolean testDC(FilterInfo fi) 
    {
	FilterContent filter = fi.filter;
	System.out.println(filter);
	
	//runs some tests to see if we can 
	//generate code direct commmunication code
	if (KjcOptions.ratematch)
	    return false;
	if (fi.isTwoStage())
	    return false;
	if (fi.bottomPeek > 0 ||
	    fi.remaining > 0)
	    return false;
	if (fi.peek > fi.pop)
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
	return true;
    }

    public DirectCommunication(FilterInfo filterInfo) 
    {
	super(filterInfo);
	System.out.println("Generating code for " + filterInfo.filter + " using Direct Comm.");

    }

    public JFieldDeclaration[] getVarDecls() 
    {
	Vector decls = new Vector();
	FilterContent filter = filterInfo.filter;

	for (int i = 0; i < filter.getFields().length; i++) 
	    decls.add(filter.getFields()[i]);
	
	//index variable for certain for loops
	JVariableDefinition exeIndexVar = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    exeIndex + uniqueID,
				    null);

	//remember the JVarDef for latter (in the raw main function)
	generatedVariables.exeIndex = exeIndexVar;
	decls.add(new JFieldDeclaration(null, exeIndexVar, null, null));
	
	//index variable for certain for loops
	JVariableDefinition exeIndex1Var = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    exeIndex1 + uniqueID,
				    null);

	generatedVariables.exeIndex1 = exeIndex1Var;
	decls.add(new JFieldDeclaration(null, exeIndex1Var, null, null));

	//convert the communication
	//all the communication is in the work function
	filter.getWork().accept(new DirectConvertCommunication());

	return (JFieldDeclaration[])decls.toArray(new JFieldDeclaration[0]);
    }

    public JMethodDeclaration getInitStageMethod() 
    {
	JBlock statements = new JBlock(null, new JStatement[0], null);
	FilterContent filter = filterInfo.filter;

	//add the calls for the work function in the initialization stage
	statements.addStatement(generateInitWorkLoop(filter));
	//add the calls to the work function in the prime pump stage
	statements.addStatement(getWorkFunctionBlock(filterInfo.primePump));	
	
	return new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
				      CStdType.Void,
				      initStage + uniqueID,
				      JFormalParameter.EMPTY,
				      CClassType.EMPTY,
				      statements,
				      null,
				      null);
    }
    
    public JMethodDeclaration[] getHelperMethods() 
    {
	Vector methods = new Vector();
	
	//add all helper methods, except work function
	for (int i = 0; i < filterInfo.filter.getMethods().length; i++) 
	    if (!(filterInfo.filter.getMethods()[i].equals(filterInfo.filter.getWork())))
		methods.add(filterInfo.filter.getMethods()[i]);
	
	return (JMethodDeclaration[])methods.toArray(new JMethodDeclaration[0]);	
    }
    
    /** 
     * Return the block to call the work function in the steady state
     */
    public JBlock getSteadyBlock() 
    {
	return getWorkFunctionBlock(filterInfo.steadyMult);
    }
    
    /**
     * Generate code to receive data and call the work function mult times
     **/
    private JBlock getWorkFunctionBlock(int mult)
    {
	JBlock block = new JBlock(null, new JStatement[0], null);
	FilterContent filter = filterInfo.filter;
	//inline the work function in a while loop
	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.
	    deepCopy(filter.getWork().getBody());
	
	//create the for loop that will execute the work function
	//local variable for the work loop
	JVariableDefinition loopCounter = new JVariableDefinition(null,
								  0,
								  CStdType.Integer,
								  workCounter,
								  null);
	
	

	JStatement loop = 
	    makeForLoop(workBlock, loopCounter, new JIntLiteral(mult));
	block.addStatement(new JVariableDeclarationStatement(null,
							     loopCounter,
							     null));
	block.addStatement(loop);
	/*
	return new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
				      CStdType.Void,
				      steadyStage + uniqueID,
				      JFormalParameter.EMPTY,
				      CClassType.EMPTY,
				      block,
				      null,
				      null);
	*/
	return block;
    }
    
    //generate the loop for the work function firings in the 
    //initialization schedule
    JStatement generateInitWorkLoop(FilterContent filter)
    {
	JBlock block = new JBlock(null, new JStatement[0], null);

	//clone the work function and inline it
	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.deepCopy(filter.getWork().getBody());
	
	//if we are in debug mode, print out that the filter is firing
	if (SpaceTimeBackend.FILTER_DEBUG_MODE) {
	    block.addStatement
		(new SIRPrintStatement(null,
				       new JStringLiteral(null, filter.getName() + " firing (init)."),
				       null));
	}
	
	block.addStatement(workBlock);
	
	//return the for loop that executes the block init - 1
	//times
	return makeForLoop(block, generatedVariables.exeIndex1, 
			   new JIntLiteral(filterInfo.initMult));
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
						     structReceiveMethodPrefix + 
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
}
