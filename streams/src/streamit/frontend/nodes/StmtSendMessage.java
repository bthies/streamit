package streamit.frontend.nodes;

import java.util.Collections;
import java.util.List;

/**
 * A statement that causes a message to be sent to a portal.  This looks
 * like a function call, but it exists as a statement and has the
 * receiving portal of the message as an additional parameter.  In
 * StreamIt, this causes the message and its associated parameter list
 * to be sent to all filters registered with the portal.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtSendMessage.java,v 1.1 2003-07-07 18:59:50 dmaze Exp $
 */
public class StmtSendMessage extends Statement
{
    private Expression receiver;
    private String name;
    private List params;
    private Expression minLatency, maxLatency;
    
    /**
     * Creates a message-sending statement.
     *
     * @param context  file and line number this object corresponds to
     * @param receiver expression naming the portal receiving the message
     * @param name     name of the message to send
     * @param params   list of <code>Expression</code> containing the
     *                 message parameters
     * @param minLatency expression giving the minimum number of received
     *                 items before the message is delivered, or
     *                 <code>null</code>
     * @param maxLatency expression giving the maximum number of received
     *                 items before the message is delivered, or
     *                 <code>null</code>
     */
    public StmtSendMessage(FEContext context, Expression receiver,
                           String name, List params,
                           Expression minLatency, Expression maxLatency)
    {
        super(context);
        this.receiver = receiver;
        this.name = name;
        this.params = Collections.unmodifiableList(params);
        this.minLatency = minLatency;
        this.maxLatency = maxLatency;
    }

    /**
     * Get the portal receiving the message.
     *
     * @returns expression naming the portal receiving the message
     */
    public Expression getReceiver()
    {
        return receiver;
    }

    /**
     * Get the name of the message.
     *
     * @returns the name of the message being sent
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Get the parameter list of the message.
     *
     * @returns list of <code>Expression</code> containing the parameters
     */
    public List getParams()
    {
        return params;
    }
    
    /**
     * Get the minimum latency of the message.  This is the number of
     * items that can be received or sent by this filter before the
     * receiver actually receives the message.  If <code>null</code>,
     * there is no minimum latency.
     *
     * @returns expression giving the minimum message latency
     */
    public Expression getMinLatency()
    {
        return minLatency;
    }

    /**
     * Get the maximum latency of the message.  This is the number of
     * items that can be received or sent by this filter before the
     * receiver actually receives the message.  If <code>null</code>,
     * there is no maximum latency.
     *
     * @returns expression giving the maximum message latency
     */
    public Expression getMaxLatency()
    {
        return maxLatency;
    }

    /**
     * Accepts a front-end visitor.  Calls
     * <code>streamit.frontend.nodes.FEVisitor.visitStmtSendMessage</code>
     * on the visitor.
     *
     * @param v  visitor to accept
     * @returns  defined by the visitor object
     */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtSendMessage(this);
    }
}
