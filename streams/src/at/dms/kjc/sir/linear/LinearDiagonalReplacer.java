package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;

/**
 * This replacer works well when the non-zero elements of the matrix
 * form a strip or diagonal -- more specifically, when some contiguous
 * elements in each column are non-zero.  It simply strips out the
 * zero multiplies on the top and bottom edge of each column.  (Thus
 * it deals equally well with diagonal, lower-triangular, and
 * upper-triangular mtatrices.) This replacer was inspired by the
 * Radar (CoarseSerializedBeamFormer) benchmark. <br>
 *
 * $Id: LinearDiagonalReplacer.java,v 1.4 2004-01-27 23:13:25 dmaze Exp $
 **/
public class LinearDiagonalReplacer extends LinearDirectReplacer implements Constants{
    // names of fields
    private static final String NAME_A = "sparseA";
    private static final String NAME_B = "b";
    private static final String NAME_START = "start";
    private static final String NAME_LENGTH = "length";
    /**
     * Base type of A.
     */
    private CType sparseABaseType;
    
    /**
     * Base type of b.
     */
    private CType bBaseType;
    
    /**
     * Two-dimensional coefficient field referenced in generated code.
     */
    private JFieldDeclaration sparseAField;
    /**
     * One-dimensional constant field referenced in generated code.
     */
    private JFieldDeclaration bField;
    /**
     * One-dimensional array of indices, giving the start of non-zero
     * items for a given column.
     */
    private JFieldDeclaration startField;
    /**
     * One-dimensional array of indices, giving the length of each non-zero segment.
     */
    private JFieldDeclaration lengthField;
    
    protected LinearDiagonalReplacer(LinearAnalyzer lfa, LinearReplaceCalculator costs) {
	super(lfa, costs);
    }

    /** start the process of replacement on str using the Linearity information in lfa. **/
    public static void doReplace(LinearAnalyzer lfa, SIRStream str) {
	// calculate the best way to replace linear components.
	LinearReplaceCalculator replaceCosts = new LinearReplaceCalculator(lfa);
	str.accept(replaceCosts);
	LinearPrinter.println("starting replacement pass. Will replace " + replaceCosts.getDoReplace().keySet().size() + " filters:");
	Iterator keyIter = replaceCosts.getDoReplace().keySet().iterator();
	while(keyIter.hasNext()) {
	    Object key = keyIter.next();
	    LinearPrinter.println(" " + key);
	}
	// make a new replacer with the information contained in the analyzer and the costs
	LinearDiagonalReplacer replacer = new LinearDiagonalReplacer(lfa, replaceCosts);
	// pump the replacer through the stream graph.
	IterFactory.createFactory().createIter(str).accept(replacer);
    }

    protected SIRFilter makeEfficientImplementation(SIRStream oldStream,
						    LinearFilterRepresentation linearRep) {
	// only deal with real things for now
	assert linearRep.getA().isReal() && linearRep.getb().isReal():
            "Don't support linear replacement of " +
            "complex coefficients for now.";
	// make coefficient and index fields
	makeFields(linearRep);
	// make actual filter
	SIRFilter result = super.makeEfficientImplementation(oldStream, linearRep);
	// set fields
	JFieldDeclaration[] fields = { this.sparseAField, 
				       this.bField,
				       this.startField,
				       this.lengthField };
	result.setFields(fields);
	// add initialization of fields to init function 
	addInitialization(result.getInit(), linearRep);
	return result;
    }

    /**
     * Builds field declarations for generated filter, storing them in
     * fields of this.
     */
    private void makeFields(LinearFilterRepresentation linearRep) {
	CClassType arrayType;
	this.sparseABaseType = linearRep.getA().isIntegral() ? (CType)CStdType.Integer : (CType)CStdType.Float;
	// for some reason we need to set the class of 2-dimensional
	// arrays to a plain object, since Kopi isn't analyzing them
	// for us
	arrayType = new CArrayType(sparseABaseType, 2);
	arrayType.setClass(CStdType.Object.getCClass());
	this.sparseAField = new JFieldDeclaration(null,
						  new JVariableDefinition(null,
									  0,
									  arrayType,
									  NAME_A, 
									  null),
						  null,
						  null);
	this.bBaseType = linearRep.getb().isIntegral() ? (CType)CStdType.Integer : (CType)CStdType.Float;
	this.bField = new JFieldDeclaration(null,
					    new JVariableDefinition(null,
								    0,
								    new CArrayType(bBaseType, 1),
								    NAME_B, 
								    null),
					    null,
					    null);
	this.startField = new JFieldDeclaration(null,
						new JVariableDefinition(null,
									0,
									new CArrayType(CStdType.Integer, 1),
									NAME_START, 
									null),
						null,
						null);
	this.lengthField = new JFieldDeclaration(null,
						 new JVariableDefinition(null,
									 0,
									 new CArrayType(CStdType.Integer, 1),
									 NAME_LENGTH, 
									 null),
						 null,
						 null);
    }

    /**
     * Adds field initialization functions to init function "init".
     */
    private void addInitialization(JMethodDeclaration init, LinearFilterRepresentation linearRep) {
	JBlock block = init.getBody();
	FilterMatrix A = linearRep.getA();
	int rows = A.getRows();
	int cols = A.getCols();
	// first build start, end arrays.
	int[] start = new int[cols];
	int[] length = new int[cols];
	// keep track of max length
	int maxLength = 0;
	// zeros for debugging
	int zeros = 0;
	// for each column of the matrix...
	for (int j=0; j<cols; j++) {
	    // find start
	    int i = 0;
	    while (i<rows && A.getElement(rows-i-1, j).equals(ComplexNumber.ZERO)) {
		start[j]++;
		i++;
	    }
	    // find end
	    i = rows-1;
	    int end = rows-1;
	    while (i>=0 && A.getElement(rows-i-1, j).equals(ComplexNumber.ZERO)) {
		end--;
		i--;
	    }
	    // calculate length
	    length[j] = end-start[j]+1;
	    if (length[j]>maxLength) {
		maxLength = length[j];
	    }
	}
	LinearPrinter.println("Found " + zeros + " / " + (rows*cols) + " zeros in sparse matrix.");
	// allocate sparseA
	JExpression[] dims = { new JIntLiteral(maxLength), new JIntLiteral(cols) };
	block.addStatement(makeAssignmentStatement(new JFieldAccessExpression(null, new JThisExpression(null), NAME_A),
						   new JNewArrayExpression(null, sparseABaseType, dims, null)));
	// allocate b
	JExpression[] dims2 = { new JIntLiteral(cols) };
	block.addStatement(makeAssignmentStatement(new JFieldAccessExpression(null, new JThisExpression(null), NAME_B),
						   new JNewArrayExpression(null, bBaseType, dims2, null)));
	// allocate start
	block.addStatement(makeAssignmentStatement(new JFieldAccessExpression(null, new JThisExpression(null), NAME_START),
						   new JNewArrayExpression(null, CStdType.Integer, dims2, null)));
	// allocate length
	block.addStatement(makeAssignmentStatement(new JFieldAccessExpression(null, new JThisExpression(null), NAME_LENGTH),
						   new JNewArrayExpression(null, CStdType.Integer, dims2, null)));

	// initialize the entries.  Note that here we are substituting
	// "cols-j-1" for "cols" in the LHS of each assignment that is
	// being generated.  This is because we want to push the
	// high-numbered columns first when we loop through in
	// increasing order in the work function.
	for (int j=0; j<cols; j++) {
	    JExpression rhs;
	    for (int i=0; i<length[j]; i++) {
		// "sparseA"[i][j] = A.getElement(rows-start[j]-i-i, j)
		rhs = ( sparseAField.getVariable().getType()==CStdType.Integer ? 
			(JExpression)new JIntLiteral((int)A.getElement(rows-start[j]-i-1, j).getReal()) :
			(JExpression)new JFloatLiteral((float)A.getElement(rows-start[j]-i-1, j).getReal()) );

		block.addStatement(makeAssignmentStatement(new JArrayAccessExpression(null,
										      makeArrayFieldAccessExpr(sparseAField.getVariable(), i),
										      new JIntLiteral(cols-j-1)),
							   rhs));
	    }
	    // "b"[j] = b.getElement(j)
	    rhs = ( bField.getVariable().getType()==CStdType.Integer ?
		    (JExpression)new JIntLiteral((int)linearRep.getb().getElement(j).getReal()) :
		    (JExpression)new JFloatLiteral((float)linearRep.getb().getElement(j).getReal()) );
	    block.addStatement(makeAssignmentStatement(makeArrayFieldAccessExpr(bField.getVariable(), cols-j-1), rhs));
	    // "start"[j] = start[j]
	    block.addStatement(makeAssignmentStatement(makeArrayFieldAccessExpr(startField.getVariable(), cols-j-1), new JIntLiteral(start[j])));
	    // "length"[j] = length[j]
	    block.addStatement(makeAssignmentStatement(makeArrayFieldAccessExpr(lengthField.getVariable(), cols-j-1), new JIntLiteral(length[j])));
	}
    }

    /**
     * Generate a Vector of Statements which implement (directly) the
     * matrix multiplication represented by the linear representation.<br>
     *
     * The basic format of the resulting statements is:<br>
     * <pre>
     * int sum, count, iters;
     * for (int j=0; j<numPush; j++) {
     *   float sum = 0.0;
     *   int count = start[j];
     *   int iters = length[j];
     *   for (int i=0; i<iters; i++) {
     *     sum += sparseA[i][j] * peek(count);
     *     count++;
     *   }
     *   sum += b[j];
     *   push (sum);
     * }
     * </pre>
     **/
    public Vector makePushStatementVector(LinearFilterRepresentation linearRep,
					  CType inputType,
					  CType outputType) {
	Vector result = new Vector();

	// declare our variable names
	String NAME_SUM = "sum";
	String NAME_COUNT = "count";
	String NAME_ITERS = "iters";
	// sum variable
	JVariableDefinition sumVar = new JVariableDefinition(null, 0, outputType, NAME_SUM, null);
	JVariableDefinition[] def1 = { sumVar };
	result.add(new JVariableDeclarationStatement(null, def1, null));
	// count variable
	JVariableDefinition countVar = new JVariableDefinition(null, 0, CStdType.Integer, NAME_COUNT, null);
	JVariableDefinition[] def2 = { countVar };
	result.add(new JVariableDeclarationStatement(null, def2, null));
	// iters variable
	JVariableDefinition itersVar = new JVariableDefinition(null, 0, CStdType.Integer, NAME_ITERS, null);
	JVariableDefinition[] def3 = { itersVar };
	result.add(new JVariableDeclarationStatement(null, def3, null));

	// make loop bodies and loop counters
	JBlock outerLoop = new JBlock();
	JBlock innerLoop = new JBlock();
	JVariableDefinition iVar = new JVariableDefinition(/* where */ null,  /* modifiers */ 0, /* type */ CStdType.Integer,
							   /* ident */ "i", /* initializer */ new JIntLiteral(0));
	JVariableDefinition jVar = new JVariableDefinition(/* where */ null,  /* modifiers */ 0, /* type */ CStdType.Integer,
							   /* ident */ "j", /* initializer */ new JIntLiteral(0));

	// we'll return the outer loop
	result.add(Utils.makeForLoop(outerLoop, new JIntLiteral(linearRep.getPushCount()), jVar));
	
	// build up outer loop...
	// sum = 0
	outerLoop.addStatement(makeAssignmentStatement(new JLocalVariableExpression(null, sumVar), 
						       new JIntLiteral(0)));
	// count = start[j]
	outerLoop.addStatement(makeAssignmentStatement(new JLocalVariableExpression(null, countVar), 
						       new JArrayAccessExpression(null,
										    new JFieldAccessExpression(null, new JThisExpression(null), NAME_START),
										    new JLocalVariableExpression(null, jVar))));
	// iters = length[j]
	outerLoop.addStatement(makeAssignmentStatement(new JLocalVariableExpression(null, itersVar), 
						       new JArrayAccessExpression(null,
										    new JFieldAccessExpression(null, new JThisExpression(null), NAME_LENGTH),
										    new JLocalVariableExpression(null, jVar))));
	// add the inner for loop
	outerLoop.addStatement(Utils.makeForLoop(innerLoop, new JLocalVariableExpression(null, itersVar), iVar));
	
	// sum += b[j]
	outerLoop.addStatement(makeAssignmentStatement(new JLocalVariableExpression(null, sumVar),
						       new JAddExpression(null,
									  new JLocalVariableExpression(null, sumVar),
									  new JArrayAccessExpression(null,
												     new JFieldAccessExpression(null, new JThisExpression(null), NAME_B),
												     new JLocalVariableExpression(null, jVar)))));
	// push (sum)
	outerLoop.addStatement(new JExpressionStatement(null, new SIRPushExpression(new JLocalVariableExpression(null, sumVar), outputType), null));

	// now build up the inner loop...
	// sum += sparseA[i][j] * peek(count);
	JExpression sparseAij = new JArrayAccessExpression(null,
							   makeArrayFieldAccessExpr(sparseAField.getVariable(),
										    new JLocalVariableExpression(null, iVar)),
							   new JLocalVariableExpression(null, jVar));
	JLocalVariableExpression countRef = new JLocalVariableExpression(null, countVar);
	innerLoop.addStatement(new JExpressionStatement(null, 
							new JAssignmentExpression(null,
										  new JLocalVariableExpression(null, sumVar),
										  new JAddExpression(null,
												     new JLocalVariableExpression(null, sumVar),
												     new JMultExpression(null, 
															 sparseAij, 
															 new SIRPeekExpression(countRef, inputType)))),
							null));
	// count++
	innerLoop.addStatement(new JExpressionStatement(null, 
							new JAssignmentExpression(null,
										  new JLocalVariableExpression(null, countVar),
										  new JAddExpression(null,
												     new JLocalVariableExpression(null, countVar),
												     new JIntLiteral(1))),
							null));
	return result;
    }
}
