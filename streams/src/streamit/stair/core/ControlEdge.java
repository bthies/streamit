package streamit.stair.core;

/**
 * A directed edge between two blocks in a control-flow graph.  This
 * class, along with {@link Block}, describes a control-flow graph.
 * <code>ControlEdge</code> represents the edges between blocks.  As
 * such, it has the preceding and following blocks, along with a flag
 * indicating whether or not the program will ever follow the edge.
 * This class also contains a pointer to the instruction in the
 * preceding block, if the exit is not at the end of the block, and
 * the condition under which the edge will be followed for a
 * conditional branch.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ControlEdge.java,v 1.1 2003-02-24 21:45:11 dmaze Exp $
 */
public class ControlEdge
{
    private Block predecessor, successor;
    private boolean impossible;
    private Instruction instruction;
    private Operand condition;
    
    /** Return the block that comes before the edge. */
    public Block getPredecessor()
    {
        return predecessor;
    }
    
    /** Return the block that comes after the edge. */
    public Block getSuccessor()
    {
        return successor;
    }
    
    /** Returns true if normal control flow might follow this edge. */
    public boolean isNormal()
    {
        return !impossible;
    }
    
    /**
     * Returns true if normal control flow will never follow this
     * edge.  Impossible edges are generally inserted in graphs only
     * with respect to infinite loops: since every node in the graph
     * needs to be connected to the exit node, there needs to be an
     * impossible edge from the head of such a loop to the exit node
     * of the graph.
     */
    public boolean isImpossible()
    {
        return impossible;
    }
    
    /**
     * Returns the instruction immediately preceding the edge.  If
     * <code>null</code>, the edge follows the last instruction of the
     * preceding block.  This will generally be used with blocks
     * larger than basic blocks; it distinguishes which of multiple
     * exits from a block the edge corresponds to.
     */
    public Instruction getInstruction()
    {
        return instruction;
    }
    
    /**
     * Returns the condition under which this edge is followed.  If <code>null</code>, the edge is always followed.  If the instruction is non-null, the condition is evaluated after the instruction executes.
     */
    public Operand getCondition()
    {
        return condition;
    }
}
