package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;

//each filter owns its popBuffer, the popBufferIndex, and the pushIndex
//into the next filters popBuffer.
public class FilterFusionState extends FusionState
{

    private static String POPCOUNTERNAME = "__POP_COUNTER_";
    private static String PUSHCOUNTERNAME = "__PUSH_COUNTER_";
    private static String PEEKBUFFERNAME = "__PEEK_BUFFER_";

    private static String FORINDEXNAME = "__work_counter_";
    private static String RESTORECOUNTER = "__restore_counter_";
    private static String BACKUPCOUNTER = "__backup_counter_";

    private JVariableDefinition popCounterVar;
    private JVariableDefinition pushCounterVar;
    private JVariableDefinition pushCounterVarInit;
    private JVariableDefinition peekBufferVar;
    private JVariableDefinition loopCounterVar;
    
    private SIRFilter filter;

    //if this is true, don't generate the declaration of the pop buffer,
    //this is set by a duplicate splitter if this filter shares its buffer
    //with other filters to implement the duplication
    private boolean dontGeneratePopDecl = false;

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

	createVariables();
	
	setNecessary();
    }
    
    private void setNecessary() 
    {
	if (filter instanceof SIRIdentity &&
	    peekBufferSize == 0) {
	    necessary = false;
	}
	else
	    necessary = true;
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


	peekBufferSize = getLastProducedInit() - myConsume;
	
	assert peekBufferSize >= (filter.getPeekInt() - filter.getPopInt()) &&
	    peekBufferSize >= 0 : peekBufferSize + " " + (filter.getPeekInt() - filter.getPopInt());

	//do it for init, then do it for steady
	bufferVarInit[0] = makePopBuffer(StrToRStream.getMult(node, true));
	bufferVar[0] = makePopBuffer(StrToRStream.getMult(node, false));

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
	    assert node.ways == 1;
	    assert node.edges[0] != null;
	    
	    FusionState next = getFusionState(node.edges[0]);
	    
	    pushCounterVar = new JVariableDefinition(null,
						     0,
						     CStdType.Integer,
						     PUSHCOUNTERNAME + myUniqueID,
						     new JIntLiteral(next.peekBufferSize - 1));
	    
	    pushCounterVarInit = new JVariableDefinition(null,
							 0,
							 CStdType.Integer,
							 PUSHCOUNTERNAME + myUniqueID,
							 new JIntLiteral(-1));
	    
	}
	
	JExpression[] dims = { new JIntLiteral(null, peekBufferSize)};
	
	
	peekBufferVar = new JVariableDefinition(null, 
						at.dms.kjc.Constants.ACC_FINAL,
						new CArrayType(Utils.voidToInt(filter.
									       getInputType()), 
							       1 /* dimension */ ),
						PEEKBUFFERNAME + myUniqueID,
						new JNewArrayExpression(null,
									Utils.voidToInt(filter.
											getInputType()),
									dims,
									null));
    }
    
    public JStatement[] getIndexDecls(boolean isInit) 
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
    
    

    public JVariableDefinition getPeekBufferVar() 
    {
	return peekBufferVar;
    }
    
   
    
    public JVariableDefinition getPopCounterVar()
    {
	return popCounterVar;
    }
    
    public JVariableDefinition getPushCounterVar(boolean isInit) 
    {
	return isInit ? pushCounterVarInit : pushCounterVar;
    }

    public int getBufferSize(FlatNode prev, boolean init) 
    {
	return StrToRStream.getMult(node, init) * filter.getPopInt() + peekBufferSize;
    }
    

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
    
    private int getLastProducedInit() 
    {
	if (node.inputs < 1) 
	    return 0;

	FlatNode last = node.incoming[0];

	return Util.getItemsPushed(last, node) * StrToRStream.getMult(last, true);
	
    }

    public JStatement getPopBufDecl(boolean isInit) 
    {
	if (dontGeneratePopDecl)
	    return null;
	
	JVariableDefinition buf = isInit ? bufferVarInit[0] : bufferVar[0];
	
	if (buf == null)
	    return null;
	
	return new JVariableDeclarationStatement(null,
						 buf,
						 null);
    }
    

    public JStatement getPeekBufDecl()
    {
	//if we need a peek buffer return the var decl
	if (peekBufferSize > 0)
	    return new JVariableDeclarationStatement(null,
						     peekBufferVar,
						     null);
	return null;
    }

    private JVariableDefinition makePopBuffer(int mult)
    {
	int itemsAccessed = mult * filter.getPopInt() + peekBufferSize;
	
	if (itemsAccessed == 0)
	    return null;

	JExpression[] dims = { new JIntLiteral(null, itemsAccessed) };
	JExpression initializer = 
	    new JNewArrayExpression(null,
				    Utils.voidToInt(filter.getInputType()),
				    dims,
				    null);
	// make a buffer for all the items looked at in a round
	return new JVariableDefinition(null,
				       at.dms.kjc.Constants.ACC_FINAL,
				       new CArrayType(Utils.voidToInt(filter.
								      getInputType()), 
						      1 /* dimension */ ),
				       BUFFERNAME + myUniqueID,
				       initializer);
    }
    

    public JStatement peekRestoreLoop(JVariableDefinition loopCounterRestore, boolean isInit) 
    {
	assert !isInit : "Calling peekRestore when in initialization schedule";
	
	if (peekBufferSize == 0) 
	    return new JEmptyStatement(null, null);


	// make a statement that will copy peeked items into the pop
	// buffer, assuming the counter will count from 0 to peekBufferSize

	// the lhs of the source of the assignment
	JExpression sourceLhs = 
	    new JLocalVariableExpression(null,
					 peekBufferVar);

	// the rhs of the source of the assignment  
	JExpression sourceRhs = 
	    new JLocalVariableExpression(null, 
					 loopCounterRestore);

	// the lhs of the dest of the assignment
	JExpression destLhs = 
	    new JLocalVariableExpression(null,
					 bufferVar[0]);
	    
	// the rhs of the dest of the assignment
	JExpression destRhs = 
	    new JLocalVariableExpression(null,
					 loopCounterRestore);

	// the expression that copies items from the pop buffer to the
	// peek buffer
	JExpression copyExp = 
	    new JAssignmentExpression(null,
				      new JArrayAccessExpression(null,
								 destLhs,
								 destRhs),
				      new JArrayAccessExpression(null,
								 sourceLhs,
								 sourceRhs));

	// finally we have the body of the loop
	JStatement body = new JExpressionStatement(null, copyExp, null);

	// return a for loop that executes (peek-pop) times.
	return GenerateCCode.makeDoLoop(body,
			   loopCounterRestore, 
			   new JIntLiteral(peekBufferSize));
    }


    /**
     * Given that a phase has already executed, backs up the state of
     * unpopped items into the peek buffer.
     */
    public JStatement peekBackupLoop(JVariableDefinition loopCounterBackup,
				     boolean isInit) 
    {
	if (peekBufferSize == 0)
	    return new JEmptyStatement(null, null);

	// make a statement that will copy unpopped items into the
	// peek buffer, assuming the counter will count from 0 to peekBufferSize

	// the lhs of the destination of the assignment
	JExpression destLhs = 
	    new JLocalVariableExpression(null,
					 peekBufferVar);
	// the rhs of the destination of the assignment
	JExpression destRhs = 
	    new JLocalVariableExpression(null, 
					 loopCounterBackup);

	// the lhs of the source of the assignment
	JExpression sourceLhs = 
	    new JLocalVariableExpression(null,
					 isInit ? bufferVarInit[0] : bufferVar[0]);
	    

	JExpression sourceRhs = 
	    new
	    JAddExpression(null, 
			   new JLocalVariableExpression(null, 
							loopCounterBackup),
			   new JIntLiteral(StrToRStream.getMult(node, isInit) * 
					   filter.getPopInt()));
	
	// the expression that copies items from the pop buffer to the
	// peek buffer
	JExpression copyExp = 
	    new JAssignmentExpression(null,
				      new JArrayAccessExpression(null,
								 destLhs,
								 destRhs),
				      new JArrayAccessExpression(null,
								 sourceLhs,
								 sourceRhs));

	// finally we have the body of the loop
	JStatement body = new JExpressionStatement(null, copyExp, null);

	// return a for loop that executes (peek-pop) times.
	return GenerateCCode.makeDoLoop(body,
					 loopCounterBackup, 
					 new JIntLiteral(peekBufferSize));
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
		filter.getMethods()[i] != filter.getWork())
		functions.add(filter.getMethods()[i]);
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
	JStatement peekBufDecl = getPeekBufDecl();
	if (peekBufDecl != null) 
	    main.addStatementFirst(peekBufDecl);
    }
    
    
    public JStatement[] getWork(JBlock enclosingBlock, boolean isInit) 
    {
	JBlock statements = new JBlock(null, new JStatement[0], null);

	//don't generate code if this filter is not needed
	if (!necessary)
	    return statements.getStatementArray();
	
	int mult = StrToRStream.getMult(getNode(), isInit);

	//add the declaration for the pop buffer
	JStatement popBufDecl = getPopBufDecl(isInit);
	if (popBufDecl != null) {
	    enclosingBlock.addStatementFirst(popBufDecl);
	}
	
	
	//now add the declaration of the pop index and the push index
	GenerateCCode.addStmtArrayFirst(enclosingBlock, getIndexDecls(isInit));

	//create the loop counter to restore the peeked items from 
	//the last firings of the filter to the pop buffer
	if (!isInit && peekBufferSize > 0) {
	    //create the loop counter
	    JVariableDefinition loopCounterRestore = 
		GenerateCCode.newIntLocal(RESTORECOUNTER, myUniqueID, 0);
	    //add the declaration of the loop counter
	    enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					     (null, loopCounterRestore, null));
	    statements.addStatement(peekRestoreLoop(loopCounterRestore,
						       isInit));
	}
	
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
	    
	    body.accept(new ConvertChannelExprs(this, isInit));
	    
	    statements.addStatement(body);
	}

	//create the loop to back up the unpop'ed items from the pop buffer
	//and store them into the peek buffer
	if (peekBufferSize > 0) {
	    //create the loop counter
	    JVariableDefinition loopCounterBackup = 
		GenerateCCode.newIntLocal(BACKUPCOUNTER, myUniqueID, 0);
	    //add the declaration of the counter
	    enclosingBlock.addStatementFirst(new JVariableDeclarationStatement
					     (null, loopCounterBackup, null));
	    statements.addStatement(peekBackupLoop(loopCounterBackup,
						      isInit));
	}

	return statements.getStatementArray();
    }
    
    public SIRFilter getFilter() 
    {
	return filter;
    }
}
