package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.sir.lowering.LoweringConstants;
import at.dms.kjc.*;
import at.dms.util.*;

/**
 * This represents a StreaMIT filter that just reads 1 item and sends it along.
 */
public class SIRIdentity extends SIRPredefinedFilter implements Cloneable {

    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    private SIRIdentity() {
	super();
    }
    
    public SIRIdentity(CType type) {
	super(null,
	      "Identity",
	      /* fields */ JFieldDeclaration.EMPTY(), 
	      /* methods */ JMethodDeclaration.EMPTY(),
	      new JIntLiteral(1), new JIntLiteral(1), new JIntLiteral(1),
	      /* input type */ type,
	      /* output type */ type);
	setType(type);
    }

    /**
     * Set the input type and output type to t
     * also sets the work function and init function
     */
    public void setType(CType t) {
	this.setInputType(t);
	this.setOutputType(t);

	// work function
	JStatement work1body[] = new JStatement[1];
	work1body[0] =  
	    new JExpressionStatement
	    (null, new SIRPushExpression
	     (new SIRPopExpression(), t),
	     null);
	
	JBlock work1block = new JBlock(/* tokref   */ null,
				/* body     */ work1body,
				/* comments */ null);
	JMethodDeclaration workfn =  new JMethodDeclaration( /* tokref     */ null,
					  /* modifiers  */ at.dms.kjc.
					  Constants.ACC_PUBLIC,
					  /* returntype */ CStdType.Void,
					  /* identifier */ "work",
					  /* parameters */ JFormalParameter.EMPTY,
					  /* exceptions */ CClassType.EMPTY,
					  /* body       */ work1block,
					  /* javadoc    */ null,
					  /* comments   */ null);
	setWork(workfn);

	// init function
	JBlock initblock = new JBlock(/* tokref   */ null,
				/* body     */ new JStatement[0],
				/* comments */ null);
	setInit(SIRStream.makeEmptyInit());
    }
}

