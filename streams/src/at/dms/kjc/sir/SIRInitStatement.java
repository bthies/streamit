package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * Init Statement.
 *
 * This statement represents a call to an init function of a
 * sub-stream.  It should take the place of any add(...) statement in
 * StreaMIT syntax.  The arguments to the constructor of the
 * sub-stream should be the <args> in here.
 */
public class SIRInitStatement extends JStatement {

    /**
     * The arguments to the init function.
     */
    protected JExpression[] args;
    /**
     * The stream structure to initialize.
     */
    private SIRStream target;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node in the parsing tree
     */
    public SIRInitStatement(TokenReference where, 
			    JavaStyleComment[] comments, 
			    JExpression[] args, 
			    SIRStream str) {
	super(where, comments);

	this.args = args;
	this.target = target;
    }
    
    /**
     * Construct a node in the parsing tree
     */
    public SIRInitStatement() {
	super(null, null);

	this.args = null;
	this.target = null;
    }
    
    public void setArgs(JExpression[] a) {
	this.args = a;
    }
    public void setTarget(SIRStream s) {
	this.target = s;
    }

    // ----------------------------------------------------------------------
    // SEMANTIC ANALYSIS
    // ----------------------------------------------------------------------

    /**
     * Analyses the statement (semantically) - NOT SUPPORTED YET.
     */
    public void analyse(CBodyContext context) throws PositionedError {
	at.dms.util.Utils.fail("Analysis of SIR nodes not supported yet.");
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p) {
	if (p instanceof SLIRVisitor) {
	    ((SLIRVisitor)p).visitInitStatement(this, args, target);
	} else {
	    at.dms.util.Utils.fail("Use SLIR visitor to visit an SIR node.");
	}
    }

    /**
     * Accepts the specified attribute visitor - NOT SUPPORTED YET.
     */
    public Object accept(AttributeVisitor p) {
	at.dms.util.Utils.fail("Visitors to SIR nodes not supported yet.");
	return null;
    }

    /**
     * Generates a sequence of bytescodes - NOT SUPPORTED YET.
     */
    public void genCode(CodeSequence code) {
	at.dms.util.Utils.fail("Codegen of SIR nodes not supported yet.");
    }
}
