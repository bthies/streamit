package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.sir.lowering.LoweringConstants;
import at.dms.kjc.*;
import at.dms.util.*;

/**
 * This represents a StreaMIT filter that just read 1 item and sends it along
 */
public class SIRIdentity extends SIRFilter implements Cloneable {
    /**
     * The filename of the data source.
     */

    public SIRIdentity() {
	super();
	pop = new JIntLiteral(1);
	push = new JIntLiteral(1);
	peek = new JIntLiteral(1);
    }

    public SIRIdentity(SIRContainer parent,
		       String ident,
		       CType type) {
	super(parent,
	      ident,
	      /* fields */ JFieldDeclaration.EMPTY(), 
	      /* methods */ JMethodDeclaration.EMPTY(),
	      new JIntLiteral(1), new JIntLiteral(1), new JIntLiteral(1),
	      /* work */ null,
	      /* input type */ type,
	      /* output type */ type);	
	pop = new JIntLiteral(1);
	push = new JIntLiteral(1);
	peek = new JIntLiteral(1);
    }
    
    /**
     * Return shallow copy of this.
     */
    public Object clone() {
	SIRIdentity f = new SIRIdentity(getParent(),
					getIdent(),
					getOutputType());
	f.setInit(this.init);
	return f;
    }
    
    /**
     * Set the input type and output type to t
     * also sets the work function
     */
    public void setType(CType t) {
	this.setInputType(t);
	this.setOutputType(t);
    }
    
    /**
     * Returns whether or not this class needs a call to an init
     * function to be generated.  Special library functions like
     * Identity's do not need an init call.
     */
    public boolean needsInit() {
	return false;
    }

    /**
     * Returns whether or not this class needs a call to a work
     * function to be generated.  Special library functions like
     * Identity's do not need a work call at the
     * level of the Kopi IR (it is generated in C).
     */
    public boolean needsWork() {
	return false;
    }

    /**
     * Returns name just so can possibly show up on dot file
     */
    public String getName() {
        return "ContextContainer";
    }
}

