package streamit.frontend.nodes;

/**
 * A message-portal type.  This is a type used to send messages; it
 * appears in the portals list of
 * <code>streamit.frontend.nodes.StreamCreator</code>, and as the
 * receiver object of
 * <code>streamit.frontend.nodes.StmtSendMessage</code>.  A portal
 * corresponds to a single named stream type.  Because of the way
 * <code>FEReplacer</code> works, attempting to keep a pointer to
 * the actual referenced stream type here is useless; we only keep
 * the name.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TypePortal.java,v 1.2 2003-07-24 16:58:37 dmaze Exp $
 */
public class TypePortal extends Type
{
    private String name;
    
    /**
     * Creates a new portal type.
     *
     * @param name  name of the target filter type
     */
    public TypePortal(String name)
    {
        this.name = name;
    }
    
    /**
     * Gets the name of the target filter type.  Other code will be
     * needed to resolve this to the actual <code>StreamSpec</code>
     * object.
     *
     * @return  name of the target filter type
     */
    public String getName()
    {
        return name;
    }
}
