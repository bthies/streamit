package at.dms.kjc.sir.lowering.fission;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.linear.*;

public class VerticalFission {
    /**
     * Keeps track of linear filters.
     */
    private final static LinearAnalyzer lfa = new LinearAnalyzer(false);
    /**
     * Name for the "last" field of generated filters.
     */
    private final static String NAME_VAR_LAST = "vLast";
    /**
     * Name for some counter variables.
     */
    private final static String NAME_VAR_COUNTER = "i";

    /**
     * Returns the maximum number of filters that <filter> could be
     * split into.  If <filter> cannot be fissed vertically, returns
     * -1.
     */
    public static int getMaxFiss(SIRFilter filter) {
	if (!(lfa.isNonLinear(filter) || lfa.hasLinearRepresentation(filter))) {
	    // test <filter> for linearity only if we haven't already
	    LinearAnalyzer.findLinearFilters(filter, KjcOptions.debug, lfa);
	}
	if (!lfa.hasLinearRepresentation(filter) || (filter.getPeekInt() % filter.getPopInt() != 0)) {  // for now, have bugs when pop doesn't divide peek
	    return -1;
	} else {
	    return filter.getPeekInt();
	}
    }

    /**
     * Do maximal fission of <filter>.
     *
     * Requires that <filter> is fissable into 2 or more components.
     */
    public static SIRPipeline fissMaximal(SIRFilter filter) {
	int num = getMaxFiss(filter);
	assert num>1: "Tried to fiss filter that is not fissable.";
	return fiss(filter, num);
    }
    /**
     * Fiss <filter> into a pipeline of no more than <numTargets> components.  
     *
     * Requires that <filter> is fissable into at least <numTargets>
     * components.
     */
    public static SIRPipeline fiss(SIRFilter filter, int numTargets) {
	assert numTargets<=getMaxFiss(filter):
            "Trying to fiss filter too far.";

	// extract linear components
	FilterMatrix A = lfa.getLinearRepresentation(filter).getA();
	FilterVector b = lfa.getLinearRepresentation(filter).getb();
	
	// establish some numeric constants, which correspond to
	// <v>ariables in example fission file
	int vPop  = filter.getPopInt();
	int vPush = filter.getPushInt();
	int vPeek = filter.getPeekInt();

	// calculate block size
	int vBlock = (int)Math.ceil(((float)vPeek) / ((float)numTargets));

	// make result, add children
	SIRPipeline result = new SIRPipeline("fiss_" + numTargets + "X_" + filter.getIdent());
	result.setInit(SIRStream.makeEmptyInit());

	for (int i=0; i<vPeek/vBlock; i++) {
	    result.add(makeFissComponent(filter, A, b, vPeek, vPop, vPush, vBlock, vPeek-vBlock*i-1));
	}
	if (vPeek%vBlock!=0) {
	    result.add(makeFissComponent(filter, A, b, vPeek, vPop, vPush, vPeek%vBlock, vPeek%vBlock-1));
	}
	
	return result;
    }

    /**
     * Makes filter of fiss component.
     */
    private static SIRFilter makeFissComponent(SIRFilter filter, FilterMatrix A, FilterVector b, int vPeek, int vPop, int vPush, int vBlock, int vStage) {
	
	// make field for <last> variable
	CType baseType = filter.getInputType();
	JFieldDeclaration vLastDecl = new JFieldDeclaration(null,
							    new JVariableDefinition(null, 0, new CArrayType(baseType, 1), NAME_VAR_LAST,  null),
							    null, null);

	// make init function
	JMethodDeclaration init = makeInit(baseType, vBlock);
	// make prework function, if applicable
	boolean twoStage = vStage==vBlock-1;
	JMethodDeclaration prework = twoStage ? makePrework(baseType, A, vPeek, vPop, vPush, vBlock, vStage) : null;
	// make work function
	JMethodDeclaration work = makeWork(baseType, A, b, vPeek, vPop, vPush, vBlock, vStage);

	// make filter
	SIRFilter result;
	JFieldDeclaration[] fields = { vLastDecl };
	if (twoStage) {
	    result = new SIRTwoStageFilter(null, // parent will be set upon addition to pipeline
					   "fiss_" + vStage + "_" + filter.getIdent(), 
					   fields,
					   JMethodDeclaration.EMPTY(),
					   new JIntLiteral(vPop+(vStage==vPeek-1 ? 0 : vPush)),  // peek
					   new JIntLiteral(vPop+(vStage==vPeek-1 ? 0 : vPush)),  // pop (==peek)
					   new JIntLiteral(vPush+(vStage==vBlock-1 ? 0 : vPop)), // push
					   work, 
					   (vPush+vPop)*(1+(vPeek*vPush/vPop)/vPush), // initPeek
					   (vPush+vPop)*(1+(vPeek*vPush/vPop)/vPush), // initPop (==initPeek)
					   vPush-((vPeek*vPush/vPop) % vPush), // initPush
					   prework, // initWork
					   baseType, baseType);
	} else {
	    result = new SIRFilter(null, // parent will be set upon addition to pipeline
				   "fiss_" + vStage + "_" + filter.getIdent(), 
				   fields,
				   JMethodDeclaration.EMPTY(),
				   new JIntLiteral(vPop+(vStage==vPeek-1 ? 0 : vPush)),  // peek
				   new JIntLiteral(vPop+(vStage==vPeek-1 ? 0 : vPush)),  // pop (==peek)
				   new JIntLiteral(vPush+(vStage==vBlock-1 ? 0 : vPop)), // push
				   work, baseType, baseType);
	}
	result.setInit(init);
	return result;
    }

    /**
     * Makes init function for fiss component, given <type> is I/O type of filter.
     */
    private static JMethodDeclaration makeInit(CType type, int vBlock) {
	// allocate vLast[vBlock]
	JExpression[] dims = { new JIntLiteral(vBlock) };
	JStatement alloc = LinearReplacer.makeAssignmentStatement(new JFieldAccessExpression(null, new JThisExpression(null), NAME_VAR_LAST),
								  new JNewArrayExpression(null, type, dims, null));

	JMethodDeclaration result = SIRStream.makeEmptyInit();
	result.getBody().addStatement(alloc);
	return result;
    }
    
    /**
     * Makes prework function for fiss component:
     *
     *   for (int i=0; i<(PEEK*PUSH/POP)/PUSH; i++) {     (1)
     *       for (int j=0; j<PUSH; j++) {                 (1a)
     *           pop();
     *       }
     *       decimate();                                  (1b)
     *   }
     *   int leftover = (PEEK*PUSH/POP) % PUSH;           (2)
     *   for (int j=0; j<leftover; j++) {                 (3)
     *       pop();
     *   }
     *   for (int j=leftover; j<PUSH; j++) {              (4)
     *       float sum = pop();                           (4a)
     *       for (int k=0; k<BLOCK; k++) {                (4b)
     *           sum += last[k]*h[k][j];                  (4c)
     *       }
     *       push(sum);                                   (4d)
     *   }
     *   decimate();                                      (5)
     *
     */
    private static JMethodDeclaration makePrework(CType baseType, FilterMatrix A, int vPeek, int vPop, int vPush, int vBlock, int vStage) {
	JBlock block = new JBlock();

	// declare sum
	final String NAME_VAR_SUM = "sum";
	JVariableDefinition sumVar = new JVariableDefinition(null, 0, baseType, NAME_VAR_SUM, null);
	JVariableDefinition[] def1 = { sumVar };
	block.addStatement(new JVariableDeclarationStatement(null, def1, null));

	// make (1)
	JStatement s1a = Utils.makeForLoop(new JExpressionStatement(null, new SIRPopExpression(baseType), null), vPush);
	JBlock s1body = new JBlock();
	s1body.addStatement(s1a);
	s1body.addAllStatements(makeDecimate(baseType, vPeek, vPop, vPush, vBlock, vStage));
	block.addStatement(Utils.makeForLoop(s1body, (vPeek*vPush/vPop)/vPush));

	// calculate leftover
	int leftover = (vPeek*vPush/vPop) % vPush;

	// make (3)
	block.addStatement(Utils.makeForLoop(new JExpressionStatement(null, new SIRPopExpression(baseType), null), leftover));

	// make (4) (unrolled for now)
	for (int j=leftover; j<vPush; j++) {
	    block.addStatement(LinearReplacer.makeAssignmentStatement(new JLocalVariableExpression(null, sumVar),
							     new SIRPopExpression(baseType)));
	    for (int k=0; k<vBlock; k++) {
		// sum += last[k]*h[k][j];
		block.addStatement(makeConvolveStep(baseType, A, vPeek, vPop, vPush, vBlock, vStage, sumVar, j, k));
	    }
	    // push(sum)
	    block.addStatement(new JExpressionStatement(null, new SIRPushExpression(new JLocalVariableExpression(null, sumVar), baseType), null));
	}
	
	// make (5)
	block.addAllStatements(makeDecimate(baseType, vPeek, vPop, vPush, vBlock, vStage));

	// make result
	JMethodDeclaration result = SIRStream.makeEmptyInitWork();
	result.getBody().addAllStatements(block);
	return result;
    }

    /**
     * Returns IR code for this statement:   
     *
     *   sum += last[k]*h[k][j];
     */
    private static JStatement makeConvolveStep(CType baseType, FilterMatrix A, int vPeek, int vPop, int vPush, int vBlock, int vStage, JLocalVariable sumVar, int j, int k) {
	return LinearReplacer.
	    makeAssignmentStatement(new JLocalVariableExpression(null, sumVar),
				    new JAddExpression(null, 
						       new JLocalVariableExpression(null, sumVar),
						       new JMultExpression(null,
									   new JArrayAccessExpression(null,
												      new JFieldAccessExpression(null, 
																 new JThisExpression(null), 
																 NAME_VAR_LAST),
												      new JIntLiteral(k)),
									   // use vStage-k instead of k since k was remapped in init function
									   baseType.isOrdinal() ? 
									   (JLiteral)(new JIntLiteral((int)A.getElement(vPeek-1-(vStage-k), vPush-1-j).getReal())) :
									   (JLiteral)(new JFloatLiteral((float)A.getElement(vPeek-1-(vStage-k), vPush-1-j).getReal())))));
    }

    /**
     * Makes work function for fiss component.
     */
    private static JMethodDeclaration makeWork(CType baseType, FilterMatrix A, FilterVector b, int vPeek, int vPop, int vPush, int vBlock, int vStage) {
	JBlock block = new JBlock();

	// call calculate
	block.addAllStatements(makeCalculate(baseType, A, b, vPeek, vPop, vPush, vBlock, vStage));
	// call decimate
	block.addAllStatements(makeDecimate(baseType, vPeek, vPop, vPush, vBlock, vStage));

	// make result
	JMethodDeclaration result = SIRStream.makeEmptyWork();
	result.getBody().addAllStatements(block);
	return result;
    }

    /**
     * Makes block of calculate phase:
     *
     *   for (int j=0; j<PUSH; j++) {
     *       float sum;
     *       if (stage==PEEK-1) {
     *           // first stage starts with a sum of zero
     *           sum = <value in constant vector>;
     *       } else {
     *           // later stages pop the sum from before
     *           sum = pop();
     *       }
     *       for (int k=0; k<BLOCK; k++) {
     *           sum += last[k]*h[k][j];
     *       }
     *       push(sum);
     *   }
     *  
     */
    private static JBlock makeCalculate(CType baseType, FilterMatrix A, FilterVector b, int vPeek, int vPop, int vPush, int vBlock, int vStage) {
	JBlock block = new JBlock();

	// declare sum
	final String NAME_VAR_SUM = "sum";
	JVariableDefinition sumVar = new JVariableDefinition(null, 0, baseType, NAME_VAR_SUM, null);
	JVariableDefinition[] def1 = { sumVar };
	block.addStatement(new JVariableDeclarationStatement(null, def1, null));
	
	for (int j=0; j<vPush; j++) {
	    if (vStage==vPeek-1) {
		// sum = <value in constant vector>
		block.addStatement(LinearReplacer.makeAssignmentStatement(new JLocalVariableExpression(null, sumVar),
									  baseType.isOrdinal() ? 
									  (JLiteral)(new JIntLiteral((int)b.getElement(vPush-1-j).getReal())) :
									  (JLiteral)(new JFloatLiteral((float)b.getElement(vPush-1-j).getReal()))));
	    } else {
		// sum = pop();
		block.addStatement(LinearReplacer.makeAssignmentStatement(new JLocalVariableExpression(null, sumVar),
									  new SIRPopExpression(baseType)));
	    }
	    for (int k=0; k<vBlock; k++) {
		// sum += last[k]*h[k][j];
		block.addStatement(makeConvolveStep(baseType, A, vPeek, vPop, vPush, vBlock, vStage, sumVar, j, k));
	    }
	    // push(sum)
	    block.addStatement(new JExpressionStatement(null, new SIRPushExpression(new JLocalVariableExpression(null, sumVar), baseType), null));
	}

	return block;
    }

    /**
     * Makes block of decimate phase.
     */
    private static JBlock makeDecimate(CType baseType, int vPeek, int vPop, int vPush, int vBlock, int vStage) {
	JBlock block = new JBlock();

	if (vPop < vBlock) {
	    block.addAllStatements(makeDecimateHelper(baseType, vPeek, vPop, vPush, vBlock, vStage, vPop));
	} else {
	    int rounds = vPop / vBlock;
	    int leftover = vPop - rounds * vBlock;
	    if (rounds>1) {
		// if there is more than one round, we can optimize by
		// not storing temporaries into last[k]...
		// first push what's in last
		if (vStage!=vBlock-1) { // last stage doesn't push
		    // --> for (int k=vBlock-1; k>=0; k--)
		    // -->   push(last[k]);
		    {
			JVariableDefinition kVar = new JVariableDefinition(null, 0, CStdType.Integer, NAME_VAR_COUNTER, null);
			block.addStatement(Utils.makeCountdownForLoop(new JExpressionStatement(null,
											       new SIRPushExpression(new JArrayAccessExpression(null,
															       new JFieldAccessExpression(null, 
																			  new JThisExpression(null), 
																			  NAME_VAR_LAST),
																		new JLocalVariableExpression(null, kVar)),
														     baseType),
											       null),
								      new JIntLiteral(vBlock),
								      kVar));
		    }
		    // then push what we pop for rounds-1
		    // --> for (int i=0; i<(rounds-1)*vBlock; i++)
		    // -->   push(pop());
		    {
			block.addStatement(Utils.makeForLoop(new JExpressionStatement(null, new SIRPushExpression(new SIRPopExpression(baseType), baseType), null),
							     new JIntLiteral((rounds-1)*vBlock)));
		    }
		} else {
		    // --> for (int i=0; i<(rounds-1)*vBlock; i++)
		    // -->   pop();
		    block.addStatement(Utils.makeForLoop(new JExpressionStatement(null, new SIRPopExpression(baseType), null),
							 new JIntLiteral((rounds-1)*vBlock)));
		}
		// then store popped values for last round into <last>
		// so that they can be seen again
		// --> for (int k=vBlock-1; k>=0; k--)
		// -->   last[k] = pop();
		{
		    JVariableDefinition kVar = new JVariableDefinition(null, 0, CStdType.Integer, NAME_VAR_COUNTER, null);
		    block.addStatement(Utils.makeCountdownForLoop(LinearReplacer.makeAssignmentStatement(new JArrayAccessExpression(null,
																    new JFieldAccessExpression(null, 
																			       new JThisExpression(null), 
																			       NAME_VAR_LAST),
																    new JLocalVariableExpression(null, kVar)),
													 new SIRPopExpression(baseType)),
								  new JIntLiteral(vBlock),
								  kVar));
		}
	    } else /* rounds==1 */ {
		// --> for (int k=vBlock-1; k>=0; k--)
		//       if (vStage!=vBlock-1) // last stage doesn't push
		// -->     push(last[k]);
		// -->   last[k] = pop(); 
		JVariableDefinition kVar = new JVariableDefinition(null, 0, CStdType.Integer, NAME_VAR_COUNTER, null);

		// first make body of this loop
		JBlock loopBody = new JBlock();
		if (vStage!=vBlock-1) {
		    loopBody.addStatement(new JExpressionStatement(null, new SIRPushExpression(new JArrayAccessExpression(null,
															  new JFieldAccessExpression(null, 
																		     new JThisExpression(null), 
																		     NAME_VAR_LAST),
															  new JLocalVariableExpression(null, kVar)),
											       baseType),
								   null));
		}
		loopBody.addStatement(LinearReplacer.makeAssignmentStatement(new JArrayAccessExpression(null,
													new JFieldAccessExpression(null, 
																   new JThisExpression(null), 
																   NAME_VAR_LAST),
													new JLocalVariableExpression(null, kVar)),
									     new SIRPopExpression(baseType)));
		block.addStatement(Utils.makeCountdownForLoop(loopBody, 
							      new JIntLiteral(vBlock),
							      kVar));
	    }
	    block.addAllStatements(makeDecimateHelper(baseType, vPeek, vPop, vPush, vBlock, vStage, leftover));
	}

	return block;
    }

    private static JBlock makeDecimateHelper(CType baseType, int vPeek, int vPop, int vPush, int vBlock, int vStage, int loopBound) {
	JBlock block = new JBlock();
	
	if (vStage!=vBlock-1) {
	    // the last stage doesn't push the decimated items
	    // --> for (int i=0; i<loopBound; i++)
	    // -->   push(last[vBlock-i-1]);
	    JVariableDefinition iVar = new JVariableDefinition(null, 0, CStdType.Integer, NAME_VAR_COUNTER, null);
	    block.addStatement(Utils.makeForLoop(new JExpressionStatement(null,
									  new SIRPushExpression(new JArrayAccessExpression(null,
															   new JFieldAccessExpression(null, 
																		      new JThisExpression(null), 
																		      NAME_VAR_LAST),
															   new JMinusExpression(null,
																		new JIntLiteral(vBlock-1),
																		new JLocalVariableExpression(null, iVar))),
												baseType),
									  null),
						 new JIntLiteral(loopBound),
						 iVar));
	}
	// --> for (int i=0; i<vBlock-loopBound; i++)
	// -->   last[vBlock-i-1] = last[vBlock-i-1-loopBound];
	{
	    JVariableDefinition iVar = new JVariableDefinition(null, 0, CStdType.Integer, NAME_VAR_COUNTER, null);
	    block.addStatement(Utils.makeForLoop(LinearReplacer.makeAssignmentStatement(new JArrayAccessExpression(null,
														   new JFieldAccessExpression(null, 
																	      new JThisExpression(null), 
																	      NAME_VAR_LAST),
														   new JMinusExpression(null,
																	new JIntLiteral(vBlock-1),
																	new JLocalVariableExpression(null, iVar))),
											new JArrayAccessExpression(null,
														   new JFieldAccessExpression(null, 
																	      new JThisExpression(null), 
																	      NAME_VAR_LAST),
														   new JMinusExpression(null,
																	new JIntLiteral(vBlock-1-loopBound),
																	new JLocalVariableExpression(null, iVar)))),
						 new JIntLiteral(vBlock-loopBound),
						 iVar));
	}
	// --> for (int i=0; i<loopBound; i++)
	// -->   last[loopBound-i-1] = pop();
	{
	    JVariableDefinition iVar = new JVariableDefinition(null, 0, CStdType.Integer, NAME_VAR_COUNTER, null);
	    block.addStatement(Utils.makeForLoop(LinearReplacer.makeAssignmentStatement(new JArrayAccessExpression(null,
														   new JFieldAccessExpression(null, 
																	      new JThisExpression(null), 
																	      NAME_VAR_LAST),
														   new JMinusExpression(null,
																	new JIntLiteral(loopBound-1),
																	new JLocalVariableExpression(null, iVar))),
											new SIRPopExpression(baseType)),
						 new JIntLiteral(loopBound),
						 iVar));
	}

	return block;
    }
}
