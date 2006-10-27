
package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.PositionedError;
import at.dms.kjc.sir.lowering.Propagator;
import at.dms.kjc.sir.lowering.LoweringConstants;
import at.dms.compiler.TokenReference;
import at.dms.util.InconsistencyException;

import java.util.*;

/**
 * This represents a stream portal used for messaging
 */
public class SIRPortal extends JLiteral /*JExpression*/ {

    protected static LinkedList<SIRPortal> portals = new LinkedList<SIRPortal>();

    protected LinkedList<SIRStream> receivers;
    protected LinkedList<SIRPortalSender> senders;

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
        receivers = new LinkedList<SIRStream>();
        senders = new LinkedList<SIRPortalSender>();
        this.type = type; 
        if (addToList) portals.add(this);    
    }

    /*
     * Finds send message statements in a stream, registering the
     * senders with their associated portals.  Returns whether or not
     * any message statements were found.
     */
    public static boolean findMessageStatements(SIRStream str) {
        final FindMessageStatements finder = new FindMessageStatements();
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void preVisitStream(SIRStream self, SIRIterator iter) {
                    finder.setSender(self);
                    JMethodDeclaration[] methods = self.getMethods();
                    for (int i=0; i<methods.length; i++) {
                        methods[i].accept(finder);
                    }
                }
            });
        return finder.foundMessageStatement();
    }

    /*
     * Returns array of all portals found
     */
    public static SIRPortal[] getPortals() {
        SIRPortal[] array = new SIRPortal[portals.size()];
        return portals.toArray(array);
    }

    /*
     * Returns array of all portals with a specific sender
     */
    public static SIRPortal[] getPortalsWithSender(SIRStream sender) {
        LinkedList<SIRPortal> list = new LinkedList<SIRPortal>();
        for (int t = 0; t < portals.size(); t++) {
            SIRPortal portal = portals.get(t);
            if (portal.hasSender(sender)) list.add(portal);
        }
        SIRPortal[] array = new SIRPortal[list.size()];
        return list.toArray(array);
    }

    /*
     * Returns array of all portals with a specific receiver
     */
    public static SIRPortal[] getPortalsWithReceiver(SIRStream receiver) {
        LinkedList<SIRPortal> list = new LinkedList<SIRPortal>();
        for (int t = 0; t < portals.size(); t++) {
            SIRPortal portal = portals.get(t);
            if (portal.hasReceiver(receiver)) list.add(portal);
        }
        SIRPortal[] array = new SIRPortal[list.size()];
        return list.toArray(array);
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
            if (senders.get(t).getStream().equals(sender)) return true;
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
        return receivers.toArray(array);
    }

    public SIRPortalSender[] getSenders() {
        SIRPortalSender[] array = new SIRPortalSender[senders.size()];
        return senders.toArray(array);
    }

    static class FindMessageStatements extends SLIRReplacingVisitor {
        /**
         * Current stream we're registering as the sender.
         */
        private SIRStream sender;
        /**
         * Whether or not we found any message statements.
         */
        private boolean foundMessageStatement = false;
        
        public void setSender(SIRStream sender) {
            this.sender = sender;
        }

        public boolean foundMessageStatement() {
            return foundMessageStatement;
        }

        public Object visitMessageStatement(SIRMessageStatement self, 
                                            JExpression portal, 
                                            java.lang.String iname, 
                                            java.lang.String ident, 
                                            JExpression[] args, 
                                            SIRLatency latency) {
            foundMessageStatement = true;
            if (self.getPortal() instanceof SIRPortal) {
                SIRPortalSender ps = new SIRPortalSender(sender, self.getLatency());
                ((SIRPortal)self.getPortal()).addSender(ps);
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

    public boolean isConstant() 
    {
        return true;
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
     * @param   p       the visitor
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
     * @param   p       the visitor
     */
    public Object accept(AttributeVisitor p) {
        if (p instanceof SLIRAttributeVisitor) {
            return ((SLIRAttributeVisitor)p).visitPortal(this);
        } else {
            return this;
        }
    }

    /**
     * Accepts the specified visitor
     * @param p the visitor
     * @param o object containing extra data to be passed to visitor
     * @return object containing data generated by visitor 
     */
    @Override
    public <S,T> S accept(ExpressionVisitor<S,T> p, T o) {
        return p.visitPortal(this,o);
    }



    /**
     * Generates JVM bytecode to evaluate this expression.
     *
     * @param   code        the bytecode sequence
     * @param   discardValue    discard the result of the evaluation ?
     */
    public void genCode(CodeSequence code, boolean discardValue) {
        if (!discardValue) {
            setLineNumber(code);
            code.plantNoArgInstruction(opc_aconst_null);
        }
    }
    
    public String convertToString() {
        // does not make sense for an sir portal
        return "[SIRPortal]";
    }

    //############################
    // end of JLiteral methods

}
