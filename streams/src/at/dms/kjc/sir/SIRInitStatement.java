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
     * Accepts the specified visitor - NOT SUPPORTED YET.
     */
    public void accept(KjcVisitor p) {
	at.dms.util.Utils.fail("Visitors to SIR nodes not supported yet.");
    }

    /**
     * Generates a sequence of bytescodes - NOT SUPPORTED YET.
     */
    public void genCode(CodeSequence code) {
	at.dms.util.Utils.fail("Codegen of SIR nodes not supported yet.");
    }
}
