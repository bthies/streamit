package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;

/**
 * This replacer works by calling the matrix multiply routines in the
 * ATLAS package, which it assumes are installed in $ATLAS_HOME.<br>
 *
 * $Id: LinearAtlasReplacer.java,v 1.3 2003-10-24 22:03:59 thies Exp $
 **/
public class LinearAtlasReplacer extends LinearDirectReplacer implements Constants{
    // names of fields
    private static final String NAME_A = "A";
    private static final String NAME_X = "x";
    private static final String NAME_B = "b";
    private static final String NAME_Y = "y";
    
    /**
     * Coefficient array (logically two dimensions, but represented as a 1-D array).
     */
    private JFieldDeclaration aField;
    /**
     * One-dimensional field holding a chunk of the input data.
     */
    private JFieldDeclaration xField;
    /**
     * One-dimensional constant field referenced in generated code.
     */
    private JFieldDeclaration bField;
    /**
     * One-dimensional field for the results.
     */
    private JFieldDeclaration yField;
    
    protected LinearAtlasReplacer(LinearAnalyzer lfa, LinearReplaceCalculator costs) {
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
	LinearAtlasReplacer replacer = new LinearAtlasReplacer(lfa, replaceCosts);
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
	JFieldDeclaration[] fields = { this.aField, 
				       this.xField,
				       this.bField,
				       this.yField };
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
	this.aField = new JFieldDeclaration(null,
					    new JVariableDefinition(null,
								    0,
								    new CArrayType(CStdType.Float, 1),
								    NAME_A,
								    null),
					    null,
					    null);
	this.xField = new JFieldDeclaration(null,
					    new JVariableDefinition(null,
								    0,
								    new CArrayType(CStdType.Float, 1),
								    NAME_X,
								    null),
					    null,
					    null);
	this.bField = new JFieldDeclaration(null,
					    new JVariableDefinition(null,
								    0,
								    new CArrayType(CStdType.Float, 1),
								    NAME_B,
								    null),
					    null,
					    null);
	this.yField = new JFieldDeclaration(null,
					    new JVariableDefinition(null,
								    0,
								    new CArrayType(CStdType.Float, 1),
								    NAME_Y,
								    null),
					    null,
					    null);
    }

    /**
     * Adds field initialization functions to init function <init>.
     */
    private void addInitialization(JMethodDeclaration init, LinearFilterRepresentation linearRep) {
	JBlock block = init.getBody();
	FilterMatrix A = linearRep.getA();
	FilterVector b = linearRep.getb();
	// allocate A
	JExpression[] dims1 = { new JIntLiteral(A.getRows()*A.getCols()) };
	block.addStatement(makeAssignmentStatement(new JFieldAccessExpression(null, new JThisExpression(null), NAME_A),
						   new JNewArrayExpression(null, CStdType.Float, dims1, null)));
	// allocate x
	JExpression[] dims2 = { new JIntLiteral(A.getRows()) };
	block.addStatement(makeAssignmentStatement(new JFieldAccessExpression(null, new JThisExpression(null), NAME_X),
						   new JNewArrayExpression(null, CStdType.Float, dims2, null)));
	// allocate b
	JExpression[] dims3 = { new JIntLiteral(A.getCols()) };
	block.addStatement(makeAssignmentStatement(new JFieldAccessExpression(null, new JThisExpression(null), NAME_B),
						   new JNewArrayExpression(null, CStdType.Float, dims3, null)));
	// allocate y
	JExpression[] dims4 = { new JIntLiteral(A.getCols()) };
	block.addStatement(makeAssignmentStatement(new JFieldAccessExpression(null, new JThisExpression(null), NAME_Y),
						   new JNewArrayExpression(null, CStdType.Float, dims4, null)));

	// initialize the entries of A.  Do this on TRANSPOSE of A,
	// since we're doing A*b instead of b*A.
	FilterMatrix AT = A.transpose();
	int rowsAT = AT.getRows();
	int colsAT = AT.getCols();
	for (int i=0; i<rowsAT; i++) {
	    for (int j=0; j<colsAT; j++) {
		// use row-major order
		JExpression lhs = makeArrayFieldAccessExpr(aField.getVariable(), i*colsAT + j);
		JExpression rhs = new JFloatLiteral((float)AT.getElement(i, j).getReal());
		block.addStatement(makeAssignmentStatement(lhs, rhs));
	    }
	}
	// initialize elements of b
	for (int j=0; j<A.getCols(); j++) {
	    JExpression lhs = makeArrayFieldAccessExpr(bField.getVariable(), j);
	    JExpression rhs = new JFloatLiteral((float)b.getElement(j).getReal());
	    block.addStatement(makeAssignmentStatement(lhs, rhs));
	}
    }

    /**
     * Generate a Vector of Statements which implements the matrix
     * multiply using a callout to ATLAS.
     *
     * The basic format of the resulting statements is:<p>
     * <pre>
     * for (int i=0; i<peekCount; i++) {
     *   x[i] = PEEK(peekCount-1-i)
     * }
     * atlasMatrixVectorProduct(A, x, b, pushCount, peekCount, y);
     * for (int j=pushCount-1; j>=0; j--) {
     *   PUSH(y[j]);
     * }
     * </pre>
     **/
    public Vector makePushStatementVector(LinearFilterRepresentation linearRep,
					  CType inputType,
					  CType outputType) {
	Vector result = new Vector();

	// make loop bodies and loop counters
	JVariableDefinition iVar = new JVariableDefinition(/* where */ null,  /* modifiers */ 0, /* type */ CStdType.Integer,
							   /* ident */ "i", /* initializer */ null);
	JVariableDefinition jVar = new JVariableDefinition(/* where */ null,  /* modifiers */ 0, /* type */ CStdType.Integer,
							   /* ident */ "j", /* initializer */ null);

	// make peek loop
	JExpression lhs = makeArrayFieldAccessExpr(xField.getVariable(), new JLocalVariableExpression(null, iVar));
	JExpression rhs = new SIRPeekExpression(new JMinusExpression(null, new JIntLiteral(linearRep.getPeekCount()-1), new JLocalVariableExpression(null, iVar)), inputType);
	result.add(Utils.makeForLoop(makeAssignmentStatement(lhs, rhs),
				     new JIntLiteral(linearRep.getPeekCount()),
				     iVar));

	// make call to atlas
	JExpression[] args = { new JFieldAccessExpression(null, new JThisExpression(null), NAME_A),
			       new JFieldAccessExpression(null, new JThisExpression(null), NAME_X),
			       new JFieldAccessExpression(null, new JThisExpression(null), NAME_B),
			       new JIntLiteral(linearRep.getPushCount()),
			       new JIntLiteral(linearRep.getPeekCount()),
			       new JFieldAccessExpression(null, new JThisExpression(null), NAME_Y) };
	result.add(new JExpressionStatement(null, new JMethodCallExpression(null, null, "atlasMatrixVectorProduct", args), null));

	// make push loop
	result.add(Utils.makeCountdownForLoop(new JExpressionStatement(null,
								       new SIRPushExpression(makeArrayFieldAccessExpr(yField.getVariable(), 
														      new JLocalVariableExpression(null, jVar)), 
											     inputType),
								       null),
					      new JIntLiteral(linearRep.getPushCount()),
					      jVar));

	return result;
    }
}
