package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import at.dms.compiler.*;

/** 
 * This class creates imperative SIR code to implement StreamIt's filter abstraction.
 * For each filter we have a pop buffer (incoming buffer) that the filter pops from.
 * It uses the downstream's FusionState incoming buffer to write its results to.
 * For peeking buffers, we move the non-pop'ed items remaining on the incoming buffer
 * after execution to the beginning of the buffer 
 **/
public class FFSNoPeekBuffer extends FilterFusionState
{
    /* the size of the pop buffer (including the peekRestore size) */
    private int bufferSize;

    /** this will create both the init and the steady buffer **/
    public FFSNoPeekBuffer(FlatNode fnode)
    {
	super(fnode);

	createVariables();
	
	setNecessary();
    }
    /** See if this filter's code needs to be generated **/
    private void setNecessary() 
    {
	/** if we are always generating code for unnecessary filters
	    just set to true and return **/
	if (StrToRStream.GENERATE_UNNECESSARY) {
	    necessary = true;
	    return;
	}
	//for now only SIR Identities with 0 remaining are necessary
	if (filter instanceof SIRIdentity &&
	    remaining[0] == 0) {
	    System.out.println("Found unnecessary identity");
	    necessary = false;
	}
	else
	    necessary = true;
    }

    /** return the incoming buffer (pop buffer) size **/
    public int getBufferSize(FlatNode prev, boolean init) 
    {
	return bufferSize;
    }
    
    /** create any variables this filter will need to use and calculate the
	items remaining on the incoming buffer after the initialization stage. **/
    private void createVariables() 
    {
	int myConsume;

	//the number of items this fitler consumes
	myConsume = StrToRStream.getMult(node, true) * filter.getPopInt();

	//remaining is the number of items produced by the upstream filter 
	//minus the number of items this filter consumes (all for the init stage)
	remaining[0] = getLastProducedInit() - myConsume;

	assert remaining[0] >= (filter.getPeekInt() - filter.getPopInt()) &&
	    remaining[0] >= 0 : remaining[0] + " " + (filter.getPeekInt() - filter.getPopInt());

	
	//create the pop (incoming) buffer
	bufferVar[0] = makePopBuffer();
	//create the var representing the pop buffer index
	popCounterVar = new JVariableDefinition(null,
						0,
						CStdType.Integer,
						POPCOUNTERNAME + myUniqueID,
						new JIntLiteral(-1));
	
	//for the steady-state, set the push buffer to initally be
	//peek buffer size - 1 of the downstream filter
	//(-1 because we are pre-incrementing) 
	//so the data will be placed in the pop buffer
	//after the positions where we will restore the peek buffer 
	if (filter.getPushInt() > 0) {
	    assert node.ways == 1 : "Filter with push > 0, but ways != 1";
	    assert node.edges[0] != null : "Filter with push > 0, but edge[0] == null";;
	    
	    FusionState next = getFusionState(node.edges[0]);
	    
	    pushCounterVar = new JVariableDefinition(null,
						     0,
						     CStdType.Integer,
						     PUSHCOUNTERNAME + myUniqueID,
						     new JIntLiteral(next.getRemaining(node, false) - 1));
	    
	    pushCounterVarInit = new JVariableDefinition(null,
							 0,
							 CStdType.Integer,
							 PUSHCOUNTERNAME + myUniqueID,
							 new JIntLiteral(-1));
	    
	}
    }
    
    /** return the declarations for the index variables of this filter,
	the stage depends on *isInit* **/
    private JStatement[] getIndexDecls(boolean isInit) 
    {
	Vector stmts = new Vector();

	if (isInit && StrToRStream.getMult(node, isInit) < 1)
	    return new JStatement[0];
	
	if (filter.getPopInt() > 0)
	    stmts.add(new JVariableDeclarationStatement(null,
							popCounterVar,
							null));
	if (filter.getPushInt() > 0)
    	    stmts.add(new JVariableDeclarationStatement(null,
							isInit ? pushCounterVarInit :
							pushCounterVar,
							null));
	
	return (JStatement[])stmts.toArray(new JStatement[0]);
    }
    
    /** return the number of items produced by the upstream node in the 
	init stage **/
    private int getLastProducedInit() 
    {
	if (node.inputs < 1) 
	    return 0;

	FlatNode last = node.incoming[0];

	return Util.getItemsPushed(last, node) * StrToRStream.getMult(last, true);
	
    }
    
    /** get the pop (incoming) buffer JVariableDeclarationStatement
	to declare the buffer **/
    private JStatement getPopBufDecl() 
    {
	if (dontGeneratePopDecl)
	    return null;
	
	if (bufferVar[0] == null)
	    return null;
	
	return new JVariableDeclarationStatement(null,
						 bufferVar[0],
						 null);
    }
    
    /** create the pop (incoming) buffer variable, returning  the 
	JVarDef, the size of the buffer is the max of steady and init **/
    private JVariableDefinition makePopBuffer()
    {
	// set mult to the max multiplicity of init and steady
	int mult = StrToRStream.getMult(node, false) > StrToRStream.getMult(node, true) ?
	    StrToRStream.getMult(node, false) : StrToRStream.getMult(node, true);

	//multiply mult by the pop rate and add the remaining items
	bufferSize = mult * filter.getPopInt() + remaining[0];
	
	assert bufferSize >= 0;
	
	if (bufferSize == 0)
	    return null;

	// make a buffer for all the items looked at in a round
	return makeBuffer(bufferSize, filter.getInputType(),
			  BUFFERNAME + myUniqueID);
    }
    

    /** Perform any initialization tasks necessary for the filter,
	including declaring the pop buffer, adding helper functions,
	adding fields, and adding the init function. **/
    public void initTasks(Vector fields, Vector functions,
			  JBlock initFunctionCalls, JBlock main) 
    {
	//don't do anything if this filter is not being generated
	if (!necessary)
	    return;

	//add helper functions
	for (int i = 0; i < filter.getMethods().length; i++) {
	    if (filter.getMethods()[i] != filter.getInit() &&
		filter.getMethods()[i] != filter.getWork()) {
		//check the helper function for accesses to Fields
		//we do not support helper function field access at this time..
		checkHelperFunction(filter.getMethods()[i]);
		//add the function
		functions.add(filter.getMethods()[i]);

	    }
	}

	//add fields 
	for (int i = 0; i < filter.getFields().length; i++) 
	    fields.add(filter.getFields()[i]);

	/*
	//add init function
	if (filter.getInit() != null)
	    functions.add(filter.getInit());
	
	
	//add call to the init function to the init block
	initFunctionCalls.addStatement(new JExpressionStatement
				       (null, 
					new JMethodCallExpression
					(null, new JThisExpression(null),
					 filter.getInit().getName(), new JExpression[0]),
					null));
	*/

	//inline init functions in initFunctionCalls
	//clone init function 
	JMethodDeclaration init = filter.getInit();
	JBlock oldBody = new JBlock(null, init.getStatements(), null);
	//add a comment to the init function block
	JStatement body = (JBlock)ObjectDeepCloner.deepCopy(oldBody);
	JavaStyleComment[] comment = {new JavaStyleComment(filter.toString() + " init()",
						      true, false, false)};
	initFunctionCalls.addStatement(new JEmptyStatement(null, comment));
	initFunctionCalls.addStatement(body);

	//add the declaration of the pop buffer
	JStatement popBufDecl = getPopBufDecl();
	if (popBufDecl != null) 
	    main.addStatementFirst(popBufDecl);
    }
    
    /**
     * Return a block has the necessary SIR imperative instructions 
     * to execution this filter in the init stage (*isInit* == true) or the
     * steady state (*isInit* == false), add all declaration to *enclosingBlock*.
    **/
    public JStatement[] getWork(JBlock enclosingBlock, boolean isInit) 
    {
	JBlock statements = new JBlock(null, new JStatement[0], null);

	//don't generate code if this filter is not needed
	if (!necessary)
	    return statements.getStatementArray();
	//add a comment to the SIR Code
	JavaStyleComment[] comment = {new JavaStyleComment(filter.toString(),
							   true,
							   false,
							   false)};

	if (StrToRStream.getMult(getNode(), isInit) > 0) {
	    
	    statements.addStatement(new JEmptyStatement(null, comment));
	    
	    int mult = StrToRStream.getMult(getNode(), isInit);
	    
	    //now add the for loop for the work function executions in this
	    //schedule

	    //clone work function 
	    JMethodDeclaration work = filter.getWork();
	    JBlock oldBody = new JBlock(null, work.getStatements(), null);
	    
	    JStatement body = (JBlock)ObjectDeepCloner.deepCopy(oldBody);
	    
	    //before we add the body make for loop
	    //get a new local variable
	    JVariableDefinition forIndex = GenerateCCode.newIntLocal(FORINDEXNAME,
						       myUniqueID, 0);
	    
	    //add the var decl for the index of the work loop
	    //but only if we are not generating doloops
	    if (!KjcOptions.doloops)
		enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
						 (null, forIndex, null));
	    
	    body = GenerateCCode.makeDoLoop(body, forIndex, new JIntLiteral(mult));
	    
	    //see if can generate MIV buffer indices, if true, then the conversion 
	    //was performed
	    ConvertChannelExprsMIV tryMIV = new ConvertChannelExprsMIV(this, isInit);
	    if (!StrToRStream.GENERATE_MIVS || !tryMIV.tryMIV((JDoLoopStatement)body)) {
		if (StrToRStream.GENERATE_MIVS)
		    System.out.println("Could not generate MIV indices for " + getNode().contents);
		//using incrementing index expressions for the buffer accesses
		body.accept(new ConvertChannelExprs(this, isInit));
		//now add the declaration of the pop index and the push index
		GenerateCCode.addStmtArrayFirst(enclosingBlock, getIndexDecls(isInit));
	    }
	    
	    statements.addStatement(body);
	}

	//create the loop to back up the unpop'ed items from the pop buffer
	//by moving them to beginning of the buffer
	if (remaining[0] > 0 && StrToRStream.getMult(node, isInit) > 0) {
	    //create the loop counter
	    JVariableDefinition loopCounterBackup = 
		GenerateCCode.newIntLocal(BACKUPCOUNTER, myUniqueID, 0);
	    //add the declaration of the counter
	    if (!KjcOptions.doloops)
		enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
						 (null, loopCounterBackup, null));
	    //make the back up loop, move peekBufferItems starting at pop*mult
	    //to the beginning
	    statements.addStatement(remainingBackupLoop(bufferVar[0],
							loopCounterBackup,
							filter.getPopInt() * 
							StrToRStream.getMult(node, isInit),
							remaining[0]));
	    //statements.addStatement(peekMoveLoop(loopCounterBackup,
	    //isInit));
	}

	return statements.getStatementArray();
    }
}
