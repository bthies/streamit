package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;

//each filter owns its popBuffer, the popBufferIndex, and the pushIndex
//into the next filters popBuffer.
public class FilterFusionState
{
    private static HashMap filterState;
    
    private static int uniqueID = 0;
    private static String BUFFERNAME = "__INTER_BUFFER_";
    private static String POPCOUNTERNAME = "__POP_COUNTER_";
    private static String PUSHCOUNTERNAME = "__PUSH_COUNTER_";
    private static String PEEKBUFFERNAME = "__PEEK_BUFFER_";

    private JVariableDefinition popCounterVar;
    private JVariableDefinition pushCounterVar;
    private JVariableDefinition pushCounterVarInit;
    private JVariableDefinition peekBufferVar;
    private JVariableDefinition bufferVar;
    private JVariableDefinition bufferVarInit;
    private JVariableDefinition loopCounterVar;

    private int myUniqueID;

    private FlatNode node;
    private SIRFilter filter;
    private int peekBufferSize;
    
    static 
    {
	filterState = new HashMap();
    }
    

    public static FilterFusionState getFusionState(FlatNode node) 
    {
	if (!filterState.containsKey(node)) {
	    filterState.put(node, new FilterFusionState(node));
	}
	
	return (FilterFusionState)filterState.get(node);
    }

    

    /** this will create both the init and the steady buffer **/
    private FilterFusionState(FlatNode fnode)
    {
	node = fnode;
	myUniqueID = uniqueID++;

	filter = (SIRFilter)node.contents;
	
	createVariables();
    }
    
    private void createVariables() 
    {
	int myConsume;
	
	if (filter instanceof SIRTwoStageFilter &&
	    StrToRStream.getMult(node, true) > 0) 
	    myConsume = ((SIRTwoStageFilter)filter).getInitPop() + 
		(StrToRStream.getMult(node, true) - 1) * filter.getPopInt();
	else 
	    myConsume = StrToRStream.getMult(node, true) * filter.getPopInt();


	peekBufferSize = getLastProduced() - myConsume;
	

	assert peekBufferSize >= (filter.getPeekInt() - filter.getPopInt()) &&
	    peekBufferSize >= 0;

	//do it for init, then do it for steady
	bufferVarInit = makePopBuffer(StrToRStream.getMult(node, true));
	bufferVar = makePopBuffer(StrToRStream.getMult(node, false));

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
	    
	    FilterFusionState next = getFusionState(node.edges[0]);
	    
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
    
    public JVariableDefinition getBufferVar(boolean init)
    {
	return init ? bufferVarInit : bufferVar;
    }
    
    public JVariableDefinition getPopCounterVar()
    {
	return popCounterVar;
    }
    
    public JVariableDefinition getPushCounterVar(boolean isInit) 
    {
	return isInit ? pushCounterVarInit : pushCounterVar;
    }

    public JVariableDefinition getPushBufferVar(boolean isInit)  
    {
	assert node.ways == 1;
	
	return getFusionState(node.edges[0]).getBufferVar(isInit);
	
    }
    

    public int getPeekBufferSize() 
    {
	return peekBufferSize;
    }
    
    public FlatNode getNode() 
    {
	return node;
    }


    
    
    private JStatement intAssignStm(JVariableDefinition def, int value) 
    {
	return new JExpressionStatement
	    (null,
	     new JAssignmentExpression
	     (null, new JLocalVariableExpression(null, def),
	      new JIntLiteral(value)),
	     null);
    }		   

    private int getLastProduced() 
    {
	if (node.inputs < 1) 
	    return 0;

	FlatNode last = node.incoming[0];
	
	assert last.isFilter();

	if (last.contents instanceof SIRTwoStageFilter) 
	    return ((SIRTwoStageFilter)last.contents).getInitPush() +
		(StrToRStream.getMult(last, true) - 1) * ((SIRFilter)last.contents).getPushInt();
	else
	    return ((SIRFilter)last.contents).getPushInt() * StrToRStream.getMult(last, true);
    }

    public JStatement getPopBufDecl(boolean isInit) 
    {
	JVariableDefinition buf = isInit ? bufferVarInit : bufferVar;
	
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
					 bufferVar);
	    
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
	return GenerateCCode.makeForLoop(body,
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
					 isInit ? bufferVarInit : bufferVar);
	    

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
	return GenerateCCode.makeForLoop(body,
					 loopCounterBackup, 
					 new JIntLiteral(peekBufferSize));
    }

    /*  not needed anymore
      public JStatement[] resetIndices(boolean isInit) 
    {
	//don't call this on init...
	assert !isInit;
	
	Vector statements = new Vector();

	if (filter.getPopInt() > 0) {
	    statements.add(intAssignStm(popCounterVar, -1));
	}
	if (filter.getPushInt() > 0) {
	    assert node.ways == 1;
	    assert node.edges[0] != null;
	    
	    FilterFusionState next = getFusionState(node.edges[0]);
	    int pushInit = next.peekBufferSize - 1;
	    statements.add(intAssignStm(pushCounterVar, pushInit));
	}
	
	return (JStatement[])statements.toArray(new JStatement[0]);
    }    
    */
}
