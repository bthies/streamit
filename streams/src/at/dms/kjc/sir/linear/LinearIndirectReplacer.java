package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;

/**
 * A LinearIndirectReplacer replaces the contents of the work
 * functions for linear filters (as determined by the linear filter
 * analyzer) with a sparse matrix multiply, using indirection through
 * an array (see makeLinearWork for example).  It also can replace
 * splitjoins and pipelines with linear representations with a single
 * filter that computes the same function.<br>
 *
 * $Id: LinearIndirectReplacer.java,v 1.5 2003-10-24 22:04:00 thies Exp $
 **/
public class LinearIndirectReplacer extends LinearDirectReplacer implements Constants{
    // names of fields
    private static final String NAME_A = "sparseA";
    private static final String NAME_B = "b";
    private static final String NAME_INDEX = "index";
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
     * Two-dimensional array of indices that gives the locations to peek at.
     */
    private JFieldDeclaration indexField;
    /**
     * lengthField[i] == sparseAField[i].length == indexField[i].length
     */
    private JFieldDeclaration lengthField;
    
    protected LinearIndirectReplacer(LinearAnalyzer lfa, LinearReplaceCalculator costs) {
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
	LinearIndirectReplacer replacer = new LinearIndirectReplacer(lfa, replaceCosts);
	// pump the replacer through the stream graph.
	IterFactory.createFactory().createIter(str).accept(replacer);
    }

    protected SIRFilter makeEfficientImplementation(SIRStream oldStream,
						    LinearFilterRepresentation linearRep) {
	// only deal with real things for now
	Utils.assert(linearRep.getA().isReal() && linearRep.getb().isReal(),
		     "Don't support linear replacement of complex coefficients for now.");
	// make coefficient and index fields
	makeFields(linearRep);
	// make actual filter
	SIRFilter result = super.makeEfficientImplementation(oldStream, linearRep);
	// set fields
	JFieldDeclaration[] fields = { this.sparseAField, 
				       this.bField,
				       this.indexField,
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
	// do same trick as above
	arrayType = new CArrayType(CStdType.Integer, 2);
	arrayType.setClass(CStdType.Object.getCClass());
	this.indexField = new JFieldDeclaration(null,
						new JVariableDefinition(null,
									0,
									arrayType,
									NAME_INDEX, 
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
     * Adds field initialization functions to init function init.
     */
    private void addInitialization(JMethodDeclaration init, LinearFilterRepresentation linearRep) {
	JBlock block = init.getBody();
	FilterMatrix A = linearRep.getA();
	int rows = A.getRows();
	int cols = A.getCols();
	// first build length, index arrays.  note that this
	// allocation of index is the full size of A for the sake of
	// simplicity here, but when we generate code we will cut its
	// dimension to the bounding box of the irregular indices
	int[] length = new int[cols];
	int[][] index = new int[cols][rows];
	// keep track of max length
	int maxLength = 0;
	int zeros = 0;
	// for each column of the matrix...
	for (int j=0; j<cols; j++) {
	    // length will count the number of non-zero elements
	    for (int i=0; i<rows; i++) {
		if (!A.getElement(i, j).equals(ComplexNumber.ZERO)) {
		    // store the peek index here
		    index[j][length[j]] = rows - i - 1;
		    length[j]++;
		} else {
		    zeros++;
		}
	    }
	    if (length[j] > maxLength) {
		maxLength = length[j];
	    }
	}
	LinearPrinter.println("Found " + zeros + " / " + (rows*cols) + " zeros in sparse matrix.");
	//System.err.println(linearRep.getA().getZeroString());
	// allocate sparseA
	JExpression[] dims = { new JIntLiteral(maxLength), new JIntLiteral(cols) };
	block.addStatement(makeAssignmentStatement(new JFieldAccessExpression(null, new JThisExpression(null), NAME_A),
						   new JNewArrayExpression(null, sparseABaseType, dims, null)));
	// allocate index
	block.addStatement(makeAssignmentStatement(new JFieldAccessExpression(null, new JThisExpression(null), NAME_INDEX),
						   new JNewArrayExpression(null, CStdType.Integer, dims, null)));
	// allocate b
	JExpression[] dims2 = { new JIntLiteral(cols) };
	block.addStatement(makeAssignmentStatement(new JFieldAccessExpression(null, new JThisExpression(null), NAME_B),
						   new JNewArrayExpression(null, bBaseType, dims2, null)));
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
		// "sparseA"[i][j] = A.getElement(i, index[j][i])
		rhs = ( sparseAField.getVariable().getType()==CStdType.Integer ? 
			(JExpression)new JIntLiteral((int)A.getElement(rows - index[j][i] - 1, j).getReal()) :
			(JExpression)new JFloatLiteral((float)A.getElement(rows - index[j][i] - 1, j).getReal()) );

		block.addStatement(makeAssignmentStatement(new JArrayAccessExpression(null,
										      makeArrayFieldAccessExpr(sparseAField.getVariable(), i),
										      new JIntLiteral(cols-j-1)),
							   rhs));
		// "index"[i][j] = index[i][j]
		block.addStatement(makeAssignmentStatement(new JArrayAccessExpression(null,
										      makeArrayFieldAccessExpr(indexField.getVariable(), i),
										      new JIntLiteral(cols-j-1)),
							   new JIntLiteral(index[j][i])));
	    }
	    // "b"[j] = b.getElement(j)
	    rhs = ( bField.getVariable().getType()==CStdType.Integer ?
		    (JExpression)new JIntLiteral((int)linearRep.getb().getElement(j).getReal()) :
		    (JExpression)new JFloatLiteral((float)linearRep.getb().getElement(j).getReal()) );
	    block.addStatement(makeAssignmentStatement(makeArrayFieldAccessExpr(bField.getVariable(), cols-j-1), rhs));
	    // "length"[j] = length[j]
	    block.addStatement(makeAssignmentStatement(makeArrayFieldAccessExpr(lengthField.getVariable(), cols-j-1), new JIntLiteral(length[j])));
	}
    }

    /**
     * Generate a Vector of Statements which implement (directly) the
     * matrix multiplication represented by the linear representation.
     *
     * The basic format of the resulting statements is:<p>
     * <pre>
     * int sum, count;
     * for (int j=0; j<numPush; j++) {
     *   float sum = 0.0;
     *   int count = length[j]
     *   for (int i=0; i<count; i++) {
     *     sum += sparseA[i][j] * peek(index[i][j]);
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

	// declare our sum and count variables
	String NAME_SUM = "sum";
	String NAME_COUNT = "count";
	JVariableDefinition sumVar = new JVariableDefinition(null, 0, outputType, NAME_SUM, null);
	JVariableDefinition[] def1 = { sumVar };
	result.add(new JVariableDeclarationStatement(null, def1, null));
	JVariableDefinition countVar = new JVariableDefinition(null, 0, CStdType.Integer, NAME_COUNT, null);
	JVariableDefinition[] def2 = { countVar };
	result.add(new JVariableDeclarationStatement(null, def2, null));

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
	// count = length[i]
	outerLoop.addStatement(makeAssignmentStatement(new JLocalVariableExpression(null, countVar), 
						       new JArrayAccessExpression(null,
										    new JFieldAccessExpression(null, new JThisExpression(null), NAME_LENGTH),
										    new JLocalVariableExpression(null, jVar))));
	// add the inner for loop
	outerLoop.addStatement(Utils.makeForLoop(innerLoop, new JLocalVariableExpression(null, countVar), iVar));
	
	// sum += b[i]
	outerLoop.addStatement(makeAssignmentStatement(new JLocalVariableExpression(null, sumVar),
						       new JAddExpression(null,
									  new JLocalVariableExpression(null, sumVar),
									  new JArrayAccessExpression(null,
												     new JFieldAccessExpression(null, new JThisExpression(null), NAME_B),
												     new JLocalVariableExpression(null, jVar)))));
	// push (sum)
	outerLoop.addStatement(new JExpressionStatement(null, new SIRPushExpression(new JLocalVariableExpression(null, sumVar), outputType), null));

	// now build up the inner loop...
	// sum += sparseA[i][j] * peek(index[i][j]);
	JExpression sparseAij = new JArrayAccessExpression(null,
							   makeArrayFieldAccessExpr(sparseAField.getVariable(),
										    new JLocalVariableExpression(null, iVar)),
							   new JLocalVariableExpression(null, jVar));
	JExpression indexij =   new JArrayAccessExpression(null,
							   makeArrayFieldAccessExpr(indexField.getVariable(),
										    new JLocalVariableExpression(null, iVar)),
							   new JLocalVariableExpression(null, jVar));
	innerLoop.addStatement(new JExpressionStatement(null, 
							new JAssignmentExpression(null,
										  new JLocalVariableExpression(null, sumVar),
										  new JAddExpression(null,
												     new JLocalVariableExpression(null, sumVar),
												     new JMultExpression(null, 
															 sparseAij, 
															 new SIRPeekExpression(indexij, inputType)))),
							null));
	
	return result;
    }
}
