package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;

//each filter owns its popBuffer, the popBufferIndex, and the pushIndex
//into the next filters popBuffer.
public class FFSNoPeekBuffer extends FilterFusionState
{
    //the size of the pop buffer (including the peekRestore size)
    private int bufferSize;

    /** this will create both the init and the steady buffer **/
    public FFSNoPeekBuffer(FlatNode fnode)
    {
	super(fnode);

	createVariables();
	
	setNecessary();
    }
    
    private void setNecessary() 
    {
	if (StrToRStream.GENERATE_UNNECESSARY) {
	    necessary = true;
	    return;
	}
	
	if (filter instanceof SIRIdentity &&
	    remaining[0] == 0) {
	    System.out.println("Found unnecessary identity");
	    necessary = false;
	}
	else
	    necessary = true;
    }

    public int getBufferSize(FlatNode prev, boolean init) 
    {
	return bufferSize;
    }
    

    private void createVariables() 
    {
	int myConsume;
	
	/*	if (filter instanceof SIRTwoStageFilter &&
	    StrToRStream.getMult(node, true) > 0) 
	    myConsume = ((SIRTwoStageFilter)filter).getInitPop() + 
		(StrToRStream.getMult(node, true) - 1) * filter.getPopInt();
		else*/ 
	
	myConsume = StrToRStream.getMult(node, true) * filter.getPopInt();

	remaining[0] = getLastProducedInit() - myConsume;
	
	assert remaining[0] >= (filter.getPeekInt() - filter.getPopInt()) &&
	    remaining[0] >= 0 : remaining[0] + " " + (filter.getPeekInt() - filter.getPopInt());

	
	//do it for init, then do it for steady
	bufferVar[0] = makePopBuffer();

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
    
    private int getLastProducedInit() 
    {
	if (node.inputs < 1) 
	    return 0;

	FlatNode last = node.incoming[0];

	return Util.getItemsPushed(last, node) * StrToRStream.getMult(last, true);
	
    }

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
    
    private JVariableDefinition makePopBuffer()
    {
	int mult = StrToRStream.getMult(node, false) > StrToRStream.getMult(node, true) ?
	    StrToRStream.getMult(node, false) : StrToRStream.getMult(node, true);

	bufferSize = mult * filter.getPopInt() + remaining[0];
	
	assert bufferSize >= 0;
	
	if (bufferSize == 0)
	    return null;

	// make a buffer for all the items looked at in a round
	return makeBuffer(bufferSize, filter.getInputType(),
			  BUFFERNAME + myUniqueID);
    }
    
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
	
	JStatement body = (JBlock)ObjectDeepCloner.deepCopy(oldBody);

	initFunctionCalls.addStatement(body);

	//if this buffer peeks add the declaration for the peek buffer
	//to the main function
	JStatement popBufDecl = getPopBufDecl();
	if (popBufDecl != null) 
	    main.addStatementFirst(popBufDecl);
    }
    
    
    public JStatement[] getWork(JBlock enclosingBlock, boolean isInit) 
    {
	JBlock statements = new JBlock(null, new JStatement[0], null);

	//don't generate code if this filter is not needed
	if (!necessary)
	    return statements.getStatementArray();
	
	int mult = StrToRStream.getMult(getNode(), isInit);

	
	//now add the declaration of the pop index and the push index
	GenerateCCode.addStmtArrayFirst(enclosingBlock, getIndexDecls(isInit));
	
	//now add the for loop for the work function executions in this
	//schedule
	if (StrToRStream.getMult(getNode(), isInit) > 0) {
	    //clone work function 
	    JMethodDeclaration work = filter.getWork();
	    JBlock oldBody = new JBlock(null, work.getStatements(), null);
	    
	    JStatement body = (JBlock)ObjectDeepCloner.deepCopy(oldBody);
	    
	    //before we add the body make for loop
	    //get a new local variable
	    JVariableDefinition forIndex = GenerateCCode.newIntLocal(FORINDEXNAME,
						       myUniqueID, 0);
	    
	    enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					     (null, forIndex, null));
	    
	    
	    body = GenerateCCode.makeDoLoop(body, forIndex, new JIntLiteral(mult));
	    
	    //see if can generate MIV buffer indices, if true, then the conversion 
	    //was performed
	    ConvertChannelExprsMIV tryMIV = new ConvertChannelExprsMIV(this, isInit);
	    if (!StrToRStream.GENERATE_MIVS || !tryMIV.tryMIV((JDoLoopStatement)body)) {
		if (StrToRStream.GENERATE_MIVS)
		    System.out.println("Could not generate MIV indices for " + getNode().contents);
		body.accept(new ConvertChannelExprs(this, isInit));
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
