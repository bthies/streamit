package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;


/**
 * Generates replacement filter code using the information within a
 * LinearRedundancyAnalyzer to generate filters with less computational
 * requirements. Refer to the documentation in LinearRedundancy
 * for more information.<br>
 *
 * $Id: LinearRedundancyReplacer.java,v 1.13 2003-10-24 22:04:00 thies Exp $
 **/
public class LinearRedundancyReplacer extends LinearReplacer implements Constants{
    /** The prefix to use to name fields. **/
    public static final String STATE_FIELD_PREFIX = "statefield";
    /** The postfix to use for the index variable. **/
    public static final String INDEX_POSTFIX = "_index";

    /** The constant to signify that we are making a work function (and not init work). **/
    public static final int WORK = 1;
    /** The constant to signify that we are making the init work function (and not work). **/
    public static final int INITWORK = 2;
    
    
    /** The field that has all of the linear information necessary (eg streams->linear reps). **/
    LinearAnalyzer linearInformation;
    /** The field that has all of the redundancy information necessary to create replacements. **/
    LinearRedundancyAnalyzer redundancyInformation;
    
    /** Private constructor for the LinearRedundancyReplacer.**/
    LinearRedundancyReplacer(LinearAnalyzer lfa,
			     LinearRedundancyAnalyzer lra) {
	linearInformation = lfa;
	redundancyInformation = lra;
    }

    /** start the process of replacement on str using the Linearity information in lfa. **/
    public static void doReplace(LinearAnalyzer lfa,
				 LinearRedundancyAnalyzer lra,
				 SIRStream str) {
	LinearReplacer replacer = new LinearRedundancyReplacer(lfa, lra);
	// pump the replacer through the stream graph.
	IterFactory.createFactory().createIter(str).accept(replacer);
    }

   
    /**
     * Visit a pipeline, splitjoin or filter, replacing them with a new filter
     * that directly implements the linear representation. This only
     * occurs if the replace calculator says that this stream should be replaced.
     **/
    public boolean makeReplacement(SIRStream self) {
	LinearPrinter.println("starting redundancy replacement for " + self);
	SIRContainer parent = self.getParent();
	if (parent == null) {
	    // we are done, this is the top level stream
	    LinearPrinter.println(" aborting, top level stream: " + self);
	    LinearPrinter.println(" stop.");
	    return false;
	}
	LinearPrinter.println(" parent: " + parent);
	if (!this.linearInformation.hasLinearRepresentation(self)) {
	    LinearPrinter.println(" no linear information about: " + self);
	    LinearPrinter.println(" stop.");
	    return false;
	}
	
	// pull out the linear representation
	LinearFilterRepresentation linearRep = linearInformation.getLinearRepresentation(self);

	/* if there is not a real valued FIR (all coefficients are real), we are done. */
	if (!linearRep.isPurelyReal()) {
	    LinearPrinter.println("  aborting -- filter has non real coefficients."); 
	    return false;
	}

	
	/**
	 * Calcluate the data structures that we need to generate the
	 * replaced filter's body.
	 **/
	RedundancyReplacerData tupleData;
	LinearRedundancy redundancy = this.redundancyInformation.getRedundancy(self);
	tupleData = new RedundancyReplacerData(redundancy,linearRep.getPopCount());

	// since we need a two stage filter to implement this
	// transformation, we need to make a whole new SIRStream and
	// replace self with it.
	SIRStream newImplementation;
	newImplementation = makeEfficientImplementation(self, linearRep, tupleData);

	// do the acutal replacment of the current pipeline with the new implementation
	parent.replace(self, newImplementation);
	
	LinearPrinter.println("Relative child name: " + newImplementation.getRelativeName());

	// return true since we replaced something
	return true;
    }


    SIRStream makeEfficientImplementation(SIRStream self,
					  LinearFilterRepresentation linearRep,
					  RedundancyReplacerData tupleData) {
	// We are tasked to make an efficient implementation using the tuple
	// data and the filter rep. This should be a lot of fun.
	// first of all, make the appropriate field declarations,
	// one for each reused tuple.

	// this is the place for all of the fields (state and index);
	JFieldDeclaration[] newFields = appendFieldDeclarations(makeFields(tupleData), self.getFields());

	    // now, make the new init, initWork and work functions
	JMethodDeclaration init     = makeInit(tupleData);
	JMethodDeclaration initWork = makeWork(INITWORK, linearRep, tupleData);
	JMethodDeclaration work     = makeWork(WORK,     linearRep, tupleData);

	// put all of the new pieces together into a new two stage filter.
	int peek = linearRep.getPeekCount();
	int pop  = linearRep.getPopCount();
	int push = linearRep.getPushCount();
	SIRTwoStageFilter newFilter;
	newFilter = new SIRTwoStageFilter(self.getParent(),           /* parent */
					  "Redundant"+self.getIdent(),/* ident */
					  newFields,                  /* fields */
					  new JMethodDeclaration[0],  /* methods Note:
									 -- init, work, and
									 initWork are special. */
					  new JIntLiteral(peek),      /* peek */
					  new JIntLiteral(pop),       /* pop */
					  new JIntLiteral(push),      /* push */
					  work,                       /* work */
					  peek,                       /* initPeek */
					  pop,                        /* initPop */
					  push,                       /* initPush */
					  initWork,                   /* initWork */
					  self.getInputType(),        /* input type */
					  self.getOutputType());      /* output type */
	// need to explicitly set the init function (for some reason)
	newFilter.setInit(init);
	
	return newFilter;
    }
    
    /** Makes the state and index fields for each of the tuple. **/
    public JFieldDeclaration[] makeFields(RedundancyReplacerData tupleData) {
	// use the set of reused tuples from tupleData. For each tuple that is reused,
	// make a field with the name stored in nameMap.
	JFieldDeclaration[] newFields = new JFieldDeclaration[2*tupleData.reused.size()];

	Iterator tupleIter = tupleData.reused.iterator();
	int currentIndex = 0;
	while(tupleIter.hasNext()) {
 	    LinearComputationTuple tuple = (LinearComputationTuple)tupleIter.next();
	    // make the names for the two fields for this tuple
	    String stateFieldName = tupleData.getName(tuple);
	    if (stateFieldName == null) {throw new RuntimeException("null name in name map!");}
	    String indexFieldName = stateFieldName + INDEX_POSTFIX;
	    

	    // make the variable definition for the state field
	    JVariableDefinition stateDef = new JVariableDefinition(null, /* token reference */
								   ACC_FINAL, /* modifiers */
								   getArrayType(), /* type */
								   stateFieldName, /* identity */
								   null); /* initializer */
	    // make the variable definition for the index field
	    JVariableDefinition indexDef = new JVariableDefinition(null, /* token reference */
								   ACC_FINAL, /* modifiers */
								   CStdType.Integer, /* type */
								   indexFieldName, /* identity */
								   null); /* initializer */

	    // now, add the two fields to our array
	    newFields[2*currentIndex] = new JFieldDeclaration(null, /* token reference */
							      stateDef, /* variable def */
							      null, /* java doc */
							      makeComment("state for " + tuple));
	    newFields[2*currentIndex+1] = new JFieldDeclaration(null, /* token reference */
								indexDef, /* variable def */
								null, /* java doc */
								makeComment("index for " + tuple));
	    
	    currentIndex++;
	}
	return newFields;
    }

    
    /** Make a new init method that allocates the state fields and zeros the index fields. **/
    public JMethodDeclaration makeInit(RedundancyReplacerData tupleData) {
	// The new body of the init function.
	JBlock body = new JBlock();

	// the only thing that the init function has to do is to
	// allocate space for all of the state fields. Note that the
	// space that is necessary is equal to the size of a float times
	// the max use of the tuple.
	Iterator tupleIter = tupleData.reused.iterator();
	while(tupleIter.hasNext()) {

	    LinearComputationTuple t = (LinearComputationTuple)tupleIter.next();
	    String fieldName = tupleData.getName(t);
	    String indexName = fieldName + INDEX_POSTFIX;
	    int    fieldSize = tupleData.getMaxUse(t) + 1;
	    // make a field allocation for fieldName of size maxUse
	    body.addStatement(makeFieldAllocation(fieldName, fieldSize, "state for " + t));
	    body.addStatement(makeFieldInitialization(indexName, 0, "index for " + t));
	}

	return new JMethodDeclaration(null,                  /* token reference */
				      ACC_PUBLIC,            /* modifiers */
				      CStdType.Void,         /* return type */
				      "init",                /* identifier */
				      JFormalParameter.EMPTY,/* paramters */
				      CClassType.EMPTY,      /* exceptions */
				      body,                  /* body */
				      null,                  /* java doc */
				      null);                 /* java style comment */
	
    }

    /**
     * Make either the work or the init work function. If type is WORK, then work is made,
     * if type is INITWORK, then not surprisingly initWork gets made. Both have peek and
     * pop that are equal to the original filter.
     **/
    public JMethodDeclaration makeWork(int type,
				       LinearFilterRepresentation linearRep,
				       RedundancyReplacerData tupleData) {
	if ((type != WORK) && (type != INITWORK)) {
	    throw new IllegalArgumentException("type was not INITWORK or WORK");
	}


	// The new body of the function function.
	JBlock body = new JBlock();

	// if we are making initWork, add statements to fill up the
	// state arrays all the way.
	if (type == INITWORK) {
	    Iterator tupleIter = tupleData.reused.iterator();
	    while(tupleIter.hasNext()) {
		LinearComputationTuple t = (LinearComputationTuple)tupleIter.next();
		// basically, we want to store data for each use, 1-->maxUse
		// which corresponds to what the value would have been on
		// prior executions.
		int maxUse = tupleData.getMaxUse(t);
		for (int use = 1; use<(maxUse+1); use++) {
		    String fieldName = tupleData.getName(t);
		    JExpression arrayAccessExpression = makeArrayFieldAccessExpr(fieldName,use);
		    
		    // generate the appropriate computation expression
		    // eg the computation that would have been calculated use executions
		    // before
		    LinearComputationTuple effectiveTuple;
		    int pop = linearRep.getPopCount();
		    effectiveTuple = new LinearComputationTuple((t.getPosition() -
								 pop * use),
								t.getCoefficient());
		    JExpression computationExpression = makeTupleComputation(effectiveTuple);

		    // make an assignment from the computation expression (eg peek*const)
		    // to the field access array.
		    JExpression assignExpr = new JAssignmentExpression(null,
								       arrayAccessExpression,
								       computationExpression);
		    body.addStatement(new JExpressionStatement(null,assignExpr,null));
		}
	    }
	}

	// now, we should calculate all of the tuples for this execution
	// eg for use 0 (we do this here so that it always shows up in both
	// work and initWork) -- note that the array index expression is the
	// tuple index.
	Iterator tupleIter = tupleData.reused.iterator();
	while(tupleIter.hasNext()) {
	    LinearComputationTuple t = (LinearComputationTuple)tupleIter.next();
	    JExpression fieldAccessExpr = makeFieldAccessExpression(tupleData.getName(t));
	    JExpression arrayIndex = makeFieldAccessExpression(tupleData.getName(t)+INDEX_POSTFIX);
	    // this expression is state_field[state_field_index]
	    JExpression arrayAccessExpression = new JArrayAccessExpression(null,
									   fieldAccessExpr,
									   arrayIndex);
	    // generate the appropriate computation expression for this tuple
	    // note that no effective indexes need to be computed
	    JExpression computationExpression = makeTupleComputation(t);
	    
	    // make an assignment from the computation expression (eg peek*const)
	    // to the field access array.
	    JExpression assignExpr = new JAssignmentExpression(null,
							       arrayAccessExpression,
							       computationExpression);
	    // this statement is now state_field[state_field_index] = t.coeff*peek(t.pos)
	    body.addStatement(new JExpressionStatement(null, assignExpr, null));
	}
    
	// now, for each column in the linear rep (eg the coefficients for each
	// term we need to push out, make a list of all of the tuples
	// that we need to compute. We will then process the list to generate
	// the appropriate push expression
	FilterMatrix A = linearRep.getA();
	FilterVector b = linearRep.getb();
	for (int currentCol = A.getCols()-1; currentCol >= 0; currentCol--) {
	    List termList = new LinkedList();
	    for (int currentRow = 0; currentRow< A.getRows(); currentRow++) {
		// make a tuple for the current term
		LinearComputationTuple t = new LinearComputationTuple(A.getRows() - 1 - currentRow, // index
								      A.getElement(currentRow, currentCol));
		// stick this term in the front of the list
		termList.add(0,t);
	    }

	    // now, we want to go through each term that we need to make and
	    // generate the appropriate calculation -- either field access
	    // or computation, depending on if the tuple is in the 
	    // comp map or not.
	    List expressionList = new LinkedList(); // list to store JExpressions to compute terms
	    Iterator termIter = termList.iterator();
	    while(termIter.hasNext()) {
		LinearComputationTuple t = (LinearComputationTuple)termIter.next();
		// if this tuple is in comp map, we have a stoed version of it so we should use that
		JExpression termExpr = null;
		if (tupleData.compMap.containsKey(t)) {
		    termExpr = makeTupleAccess(t,tupleData);
		// otherwise, if this is a non zero coefficient
		} else if (!t.getCoefficient().equals(ComplexNumber.ZERO)){
		    termExpr = makeTupleComputation(t); // make a straightup computation
		}
		// if the term is not null, add it to the term list
		// (it is null if the coefficient is zero)
		if (termExpr != null) {
		    expressionList.add(termExpr);
		}
	    }

	    // if the current linear rep has a constant component,
	    // add a term for that as well.
	    ComplexNumber constantOffset = b.getElement(currentCol);
	    if (!constantOffset.equals(ComplexNumber.ZERO)) {
		// bomb ye olde error if we hit a complex value
		if (!constantOffset.isReal()) {
		    throw new RuntimeException("Complex valued offsets are not supported.");
		}
		expressionList.add(new JFloatLiteral(null, (float)constantOffset.getReal()));
	    }

	    // now, we need to combine all of the terms in the term expr list into
	    // a single push statement. (assuming of course, that there are terms in the expr list.
	    if (expressionList.size() > 0) {
		JExpression pushArg;
		// if there is only one expression, we are done
		if (expressionList.size() == 1) {
		    pushArg = (JExpression)expressionList.get(0);
		} else {
		    // list is gaurenteed to have at least two elements in it
		    Iterator exprIter = expressionList.iterator();
		    JExpression expr1 = (JExpression)exprIter.next();
		    JExpression expr2 = (JExpression)exprIter.next();
		    pushArg = new JAddExpression(null, expr1, expr2);
		    // iterate through the rest of the list, adding add exprs as we go
		    while(exprIter.hasNext()) {
			JExpression nextExpr = (JExpression)exprIter.next();
			pushArg = new JAddExpression(null, pushArg, nextExpr);
		    }
		}
		// now, add a push expression to the body of the function
		JExpression pushExpr = new SIRPushExpression(pushArg, CStdType.Float);
		body.addStatement(new JExpressionStatement(null,pushExpr,null));
	    }
	}

	// finally, generate appropriate index increments for each of the
	// reused tuples.
	Iterator reusedIter = tupleData.reused.iterator();
	while(reusedIter.hasNext()) {
	    LinearComputationTuple t = (LinearComputationTuple)reusedIter.next();
	    makeIndexUpdateStatement(body, t, tupleData);
	}

	// and as a final nudge to correctness, put in the appropriate number
	// of pop.
	for (int i=0; i<linearRep.getPopCount(); i++) {
	    JExpression popExpr = new SIRPopExpression(CStdType.Float);
	    body.addStatement(new JExpressionStatement(null, popExpr, null));
	}
	
	// finally, put together the body with all of the appropriate KOPI nonsense
	String ident = (type == WORK) ? "work" : "initWork";
	return new JMethodDeclaration(null,                  /* token reference */
				      ACC_PUBLIC,            /* modifiers */
				      CStdType.Void,         /* return type */
				      ident,                 /* identifier */
				      JFormalParameter.EMPTY,/* paramters */
				      CClassType.EMPTY,      /* exceptions */
				      body,                  /* body */
				      null,                  /* java doc */
				      null);                 /* java style comment */
	
    }
				       

    /**
     * Generate a JExpression that computes the value specified by
     * a computation tuple. Specifically, given (index,coefficient)
     * computes peek(index)*coefficient.
     **/
    public JExpression makeTupleComputation(LinearComputationTuple tuple) {
	// do some checks...
	if (!tuple.getCoefficient().isReal()) {
	    throw new IllegalArgumentException("non real coefficents are not supported.");
	}
	if (tuple.getCoefficient().equals(ComplexNumber.ZERO)) {
	    throw new IllegalArgumentException("zero coefficients not allowed.");
	}

	JExpression indexExpr    = new JIntLiteral(tuple.getPosition());
	JExpression peekExpr     = new SIRPeekExpression(indexExpr,CStdType.Float);
	JExpression coeffExpr    = new JFloatLiteral(null, (float)tuple.getCoefficient().getReal());
	JExpression multExpr     = new JMultExpression(null, coeffExpr, peekExpr);

	return multExpr;
    }


    /**
     * Creats a tuple access to a tuple state field for the specified
     * tuple. The expression is of the form:
     * tuple_field[(tuple_index + use) % maxUse(tuple)
     * where tuple is in the mapping from t->(use,tuple) in compMap.
     **/
    public JExpression makeTupleAccess(LinearComputationTuple t, RedundancyReplacerData tupleData) {
	// pull out the original tuple (that we are looking up the stored values from)
	TupleCoupling coupling = (TupleCoupling)tupleData.compMap.get(t);
	LinearComputationTuple origTuple = coupling.tuple;
	int                          use = coupling.use;
	// get an expression for the state and index fields
	JExpression fieldExpr = makeFieldAccessExpression(tupleData.getName(origTuple));
	JExpression indexExpr = makeFieldAccessExpression(tupleData.getName(origTuple)+INDEX_POSTFIX);
	// now, make the array index expression (eg (tuple_index + use) % maxUse)
	JExpression addExpr   = new JAddExpression(null, indexExpr, new JIntLiteral(use));
	JExpression maxUseExpr= new JIntLiteral(tupleData.getMaxUse(origTuple)+1);
	JExpression modExpr   = new JModuloExpression(null, addExpr, maxUseExpr);
	// now, finally make the array access expression
	return new JArrayAccessExpression(null, fieldExpr, modExpr);
    }

    /**
     * Creates an index increment of the form:
     * <pre>
     * tuple_index = tuple_index - 1;
     * if (tuple_index < 0) {
     *  tuple_inded = maxUse(tuple);
     * }
     * </pre>
     **/
    public void makeIndexUpdateStatement(JBlock body,
					 LinearComputationTuple t,
					 RedundancyReplacerData tupleData) {
	// make the tuple_index field 
	JExpression indexExpr = makeFieldAccessExpression(tupleData.getName(t)+INDEX_POSTFIX);
	JExpression minusExpr = new JMinusExpression(null, indexExpr, new JIntLiteral(1));
	
	// make the decrement expression
	JExpression decExpr   = new JAssignmentExpression(null, indexExpr, minusExpr);
	JStatement  decStmt   = new JExpressionStatement(null, decExpr, null);
	
	// make the if statement (if index<0) {index = maxUseExpr;}
	JExpression condExpr   = new JRelationalExpression(null, OPE_LT, indexExpr, new JIntLiteral(0));
	JExpression maxUseExpr = new JIntLiteral(tupleData.getMaxUse(t));
	JExpression resetExpr  = new JAssignmentExpression(null, indexExpr, maxUseExpr);
	JStatement  resetStmt  = new JExpressionStatement(null, resetExpr, null);
	JStatement  emptyStmt  = new JEmptyStatement(null, null);
	JStatement ifStmt      = new JIfStatement(null,      /* token ref */
						  condExpr,  /* condition */
						  resetStmt, /* if clause */
						  emptyStmt, /* else clause */
						  null);     /* comments */
	
	body.addStatement(decStmt);
	body.addStatement(ifStmt);
    }
	
				       


    

    ///////////////////////////////////////////////////////////////
    // Inner Classes that calculate the necessary data structures
    ///////////////////////////////////////////////////////////////

    /**
     * Inner class that contains data that we need to perform
     * the redundancy elimination optimization.
     **/
    class RedundancyReplacerData {
	
	/**
	 * Mapping of Tuples to maximum use (eg the max number of work function
	 * executions in the future that this tuple could be used in.
	 **/
	private HashMap minUse;
	/**
	 * Mapping of Tuples to maximum use (eg the max number of work function
	 * executions in the future that this tuple could be used in.
	 **/
	private HashMap maxUse;
	/**
	 * Mapping of tuples to an alpha numeric name (this is used
	 * to generate variable names in the replaced code.
	 **/
	private HashMap nameMap;

	/**
	 * Mapping of Tuples to tuple couplings. The tuple couplings
	 * represent the actual value that is necessary in terms of
	 * the index of the current tuple. The key is the "tuple" that is
	 * calculated in the current work function that can be replaced by the
	 * stored value in the tuple coupling. The mapping is necessary
	 * so we know which terms to replace with their stored value.
	 **/
	private HashMap compMap;

	/**
	 * Set of tuples that get reused.
	 **/
	private HashSet reused;

	

	/** constructor that will generate the appropriate data structures. **/
	RedundancyReplacerData(LinearRedundancy redundancy, int popCount) {
	    super();

	    //LinearPrinter.println("Linear redundancy:\n" + redundancy);
	    //LinearPrinter.println("Linear Redundnacy tuples:\n" +
	                            //redundancy.getTupleString());
	    minUse       = new HashMap();
	    maxUse       = new HashMap();
	    nameMap      = new HashMap();
	    compMap      = new HashMap();
	    reused       = new HashSet();

	    // calculate min and max use along with labeling all of the
	    // computation nodes with some string (a simple number).
	    // even though we know that the list the tuples map to are
	    // always sorted.
	    HashMap tuplesToUses = redundancy.getTuplesToUses();
	    Iterator tupleIter = tuplesToUses.keySet().iterator();
	    int tupleIndex = 0;
	    while(tupleIter.hasNext()) {
		Object tuple = tupleIter.next();
		Iterator useIter = ((List)tuplesToUses.get(tuple)).iterator();
		while(useIter.hasNext()) {
		    Integer currentUse = (Integer)useIter.next();
		    Integer oldMin = (Integer)this.minUse.get(tuple);
		    Integer oldMax = (Integer)this.maxUse.get(tuple);

		    // update min if necessary (eg if null or if current use is less than old use)
		    if ((oldMin == null) || (currentUse.intValue() < oldMin.intValue())) {
			this.minUse.put(tuple, currentUse);
		    }
		    // update min if necessary (eg if null or if current use is less than old use)
		    if ((oldMax == null) || (currentUse.intValue() > oldMax.intValue())) {
			this.maxUse.put(tuple, currentUse);
		    }
		}
		// label this tuple with "tup:tupleIndex" (and inc tupleIndex)
		this.nameMap.put(tuple, (STATE_FIELD_PREFIX + "_tup" + tupleIndex++));
	    }

	    // now, we are going to compute the redundant tuples set -- the set of tuples
	    // that need to be saved each step for future use. While we are doing
	    // this, we are also going to do some sanity checks
	    tupleIter = tuplesToUses.keySet().iterator();
	    while(tupleIter.hasNext()) {
	 	LinearComputationTuple t = (LinearComputationTuple)tupleIter.next();
		if ((this.getMinUse(t) == 0) &&
		    (this.getMaxUse(t) >  0)) {
		    this.reused.add(t);
		}
	    }
	    
	    // now, we are going to compute the compMap information
	    tupleIter = reused.iterator();
	    while(tupleIter.hasNext()) {
		LinearComputationTuple tuple = (LinearComputationTuple)tupleIter.next();
		// we know this tuple is reused, so we need to add a mapping
		// to itself so we use the stored value in the new filter
		// rather than computing it twice.
		this.compMap.put(tuple, new TupleCoupling(tuple, 0));

		// get an iterator over the list of uses
		Iterator useIter = ((List)tuplesToUses.get(tuple)).iterator();
		while(useIter.hasNext()) {
		    int use = ((Integer)useIter.next()).intValue();
		    
		    // make new tuple corresponding to the calculation of this tuple
		    // in the first work function execution. If that new tuple has
		    // min use of zero (eg is actually calculated in the first
		    // work function) we add a mapping from the new tuple to the
		    // (original tuple, original use);
		    LinearComputationTuple newTuple;
		    newTuple = new LinearComputationTuple((tuple.getPosition() -
							   use*popCount),
							  tuple.getCoefficient());
		    if (this.getMinUse(newTuple) == 0) {
			// get any old use of this computation
			int oldUse = (this.compMap.containsKey(newTuple) ?
				      ((TupleCoupling)this.compMap.get(newTuple)).use :
				      0);
			// if the old use is greater than the current use,
			// then we want to use the older source
			if (oldUse < use) {
			    this.compMap.put(newTuple, new TupleCoupling(tuple, use));
			}
		    }
		}
	    }
	}
	
	////// Accessors
	/** Get the int value of the min use for this tuple. **/
	public int getMinUse(LinearComputationTuple t) {
	    if (!(this.minUse.containsKey(t))) {
		throw new IllegalArgumentException("unknown tuple for min use: " + t);
	    }
	    return ((Integer)this.minUse.get(t)).intValue();
	}
	/** Get the int value of the max use for this tuple. **/
	public int getMaxUse(LinearComputationTuple t) {
	    if (!(this.maxUse.containsKey(t))) {
		throw new IllegalArgumentException("unknown tuple for max use: " + t);
	    }
	    return ((Integer)this.maxUse.get(t)).intValue();
	}
	/** Get the name associated with this tuple. **/
	public String getName(LinearComputationTuple t) {
	    if (!(this.nameMap.containsKey(t))) {
		throw new IllegalArgumentException("unknown tuple for name: " + t);
	    }
	    return (String)this.nameMap.get(t);
	}

	/** for debugging -- text dump of the data structures. **/
	public String toString() {
	    String returnString = "minUse:\n";
	    returnString += hash2String(this.minUse);
	    returnString += "maxUse:\n";
	    returnString += hash2String(this.maxUse);
	    returnString += "nameMap:\n";
	    returnString += hash2String(this.nameMap);
	    returnString += "compMap:\n";
	    returnString += hash2String(this.compMap);
	    returnString += "reused:\n";
	    returnString += set2String(this.reused);
	    return returnString;
	}
	/** utility to convert a hash to a string. **/
	private String hash2String(HashMap m) {
	    String str = "";
	    Iterator keyIter = m.keySet().iterator();
	    while(keyIter.hasNext()) {
		Object key = keyIter.next();
		str += (key + "-->" +
			m.get(key) + "\n");
	    }
	    return str;
	}
	/** Utility to convert a set to a string. **/
	private String set2String(HashSet s) {
	    String str = "(";
	    Iterator iter = s.iterator();
	    while(iter.hasNext()) {
		str += iter.next() + ",";
	    }
	    str = str.substring(0,str.length()-1) + ")";
	    return str;
	}
    }

    /**
     * internal class that represents the coupling of a tuple to its value in
     * some number of work executions.
     **/
    class TupleCoupling {
	public LinearComputationTuple tuple;
	public int use;
	public TupleCoupling(LinearComputationTuple t, int u) {
	    this.tuple = t;
	    this.use = u;
	}
	public String toString() {
	    return ("(" + tuple + "," + use + ")");
	}
    }
				 
}
