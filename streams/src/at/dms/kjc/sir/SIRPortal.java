
package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.compiler.PositionedError;
import at.dms.kjc.sir.lowering.Propagator;
import at.dms.kjc.sir.lowering.LoweringConstants;
import at.dms.compiler.TokenReference;
import at.dms.util.InconsistencyException;

import java.util.*;

/**
 * This represents a stream portal used for messaging
 */
public class SIRPortal extends JExpression {

    protected static LinkedList portals = new LinkedList();

    protected LinkedList receivers;
    protected LinkedList senders;

    protected CType type;

    public SIRPortal(CType type) {
	super(null); // JLiteral(TokenReference where)
	construct(type, true);
    }

    private SIRPortal(CType type, boolean addToList) {
	super(null); // JLiteral(TokenReference where)
	construct(type, addToList);
    }

    private void construct(CType type, boolean addToList) {
	receivers = new LinkedList();
	senders = new LinkedList();
	this.type = type; 
	if (addToList) portals.add(this);    
    }

    /*
     * Finds send message statements in stream structure
     */

    public static void findMessageStatements(SIRStream str) {
    
	SIRPortal tmp = new SIRPortal(null, false);
	tmp.traverse(str);
    }

    /*
     * Returns array of all portals found
     */

    public static SIRPortal[] getPortals() {
	SIRPortal[] array = new SIRPortal[portals.size()];
	return (SIRPortal[])portals.toArray(array);
    }

    /*
     * Returns array of all portals with a specific sender
     */

    public static SIRPortal[] getPortalsWithSender(SIRStream sender) {
	LinkedList list = new LinkedList();
	for (int t = 0; t < portals.size(); t++) {
	    SIRPortal portal = (SIRPortal)portals.get(t);
	    if (portal.hasSender(sender)) list.add(portal);
	}
	SIRPortal[] array = new SIRPortal[list.size()];
	return (SIRPortal[])list.toArray(array);
    }

    /*
     * Returns array of all portals with a specific receiver
     */

    public static SIRPortal[] getPortalsWithReceiver(SIRStream receiver) {
	LinkedList list = new LinkedList();
	for (int t = 0; t < portals.size(); t++) {
	    SIRPortal portal = (SIRPortal)portals.get(t);
	    if (portal.hasReceiver(receiver)) list.add(portal);
	}
	SIRPortal[] array = new SIRPortal[list.size()];
	return (SIRPortal[])list.toArray(array);
    }

    public CType getPortalType() {
	return type;
    }

    public void addReceiver(SIRStream stream) {
	if (!receivers.contains(stream)) receivers.add(stream);
    }

    public void addSender(SIRPortalSender sender) {
	if (!senders.contains(sender)) senders.add(sender);
    }

    public boolean hasSender(SIRStream sender) {
	for (int t = 0; t < senders.size(); t++) {
	    if (((SIRPortalSender)senders.get(t)).getStream().equals(sender)) return true;
	}
	return false;
    }

    public boolean hasReceiver(SIRStream receiver) {
	for (int t = 0; t < receivers.size(); t++) {
	    if (receivers.get(t).equals(receiver)) return true;
	}
	return false;
    }

    public SIRStream[] getReceivers() {
	SIRStream[] array = new SIRStream[receivers.size()];
	return (SIRStream[])receivers.toArray(array);
    }

    public SIRPortalSender[] getSenders() {
	SIRPortalSender[] array = new SIRPortalSender[senders.size()];
	return (SIRPortalSender[])senders.toArray(array);
    }

    private void traverse(SIRStream str)
    {
        // First, visit children (if any).
        if (str instanceof SIRFeedbackLoop)
        {
            SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
            traverse(fl.getBody());
            traverse(fl.getLoop());
        }
        if (str instanceof SIRPipeline)
        {
            SIRPipeline pl = (SIRPipeline)str;
            Iterator iter = pl.getChildren().iterator();
            while (iter.hasNext())
            {
                SIRStream child = (SIRStream)iter.next();
                traverse(child);
            }
        }
        if (str instanceof SIRSplitJoin)
        {
            SIRSplitJoin sj = (SIRSplitJoin)str;
            Iterator iter = sj.getParallelStreams().iterator();
            while (iter.hasNext())
            {
                SIRStream child = (SIRStream)iter.next();
                traverse(child);
            }
        }
        
        if (str instanceof SIRFilter || str instanceof SIRPhasedFilter)
        {
	    if (str.needsWork()) {
		FindMessageStatements find = new FindMessageStatements(str);
		str.getWork().accept(find);
	    }
        }
    }

    class FindMessageStatements extends SLIRReplacingVisitor {

	SIRStream stream;

	public FindMessageStatements(SIRStream stream) {
	    this.stream = stream;
	}

	public Object visitMessageStatement(SIRMessageStatement self, 
					    JExpression portal, 
					    java.lang.String iname, 
					    java.lang.String ident, 
					    JExpression[] args, 
					    SIRLatency latency) {
	    if (self.getPortal() instanceof SIRPortal) {
		SIRPortalSender sender = new SIRPortalSender(stream, self.getLatency());
		((SIRPortal)self.getPortal()).addSender(sender);
	    }
	    return self;
	}	
    }

    //############################
    // JLiteral extended methods

    /**
     * Throws an exception (NOT SUPPORTED YET)
     */
    public JExpression analyse(CExpressionContext context) throws PositionedError {
	at.dms.util.Utils.fail("Analysis of custom nodes not supported yet.");
	return this;
    }

    public boolean isDefault() { 
	return false; 
    }

    public JExpression convertType(CType dest, CExpressionContext context) {
	throw new InconsistencyException("cannot convert Potral type");	
    }

    /**
     * Compute the type of this expression (called after parsing)
     * @return the type of this expression
     */
    public CType getType() {
	return CStdType.Null;
    }

    /**
     * Accepts the specified visitor
     * @param	p		the visitor
     */
    public void accept(KjcVisitor p) {
        if (p instanceof SLIRVisitor) {
            ((SLIRVisitor)p).visitPortal(this);
        } else {
            // otherwise, do nothing
        }
    }
    
    /**
     * Accepts the specified attribute visitor
     * @param	p		the visitor
     */
    public Object accept(AttributeVisitor p) {
        if (p instanceof SLIRAttributeVisitor) {
            return ((SLIRAttributeVisitor)p).visitPortal(this);
        } else {
            return this;
        }
    }


    /**
     * Generates JVM bytecode to evaluate this expression.
     *
     * @param	code		the bytecode sequence
     * @param	discardValue	discard the result of the evaluation ?
     */
    public void genCode(CodeSequence code, boolean discardValue) {
	if (!discardValue) {
	    setLineNumber(code);
	    code.plantNoArgInstruction(opc_aconst_null);
	}
    }
    
    //############################
    // end of JLiteral methods

}
