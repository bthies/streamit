package streamit.stair.core;

/**
 * An operand to a machine instruction.  This is the parent class of all
 * operand types.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: Operand.java,v 1.1 2003-02-24 21:45:11 dmaze Exp $
 */
public abstract class Operand
{
    /**
     * Return the type of the operand.
     *
     * @return  type of this operand
     */
    public abstract Type getType();
}
