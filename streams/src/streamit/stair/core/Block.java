package streamit.stair.core;

import java.util.List;

/**
 * Part of a control-flow graph containing an ordered sequence of
 * instructions.  This may be used to implement a basic block, or for
 * a flattened block containing an entire function's code, or
 * something in between.  Blocks are connected to each other with
 * {@link ControlEdge}s.  They also have sets of operands with
 * associated SSA information that are live on entry and exit of the
 * block.
 *
 * <p> This class also includes static methods for updating regions of
 * control-flow graphs.  These include methods for converting between
 * flat blocks and basic blocks and methods for updating per-block
 * information such as variable liveness.  These methods go here since
 * <code>BlockContainer</code> is an interface, since various
 * higher-level constructs might want to be block containers.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: Block.java,v 1.1 2003-02-24 21:45:11 dmaze Exp $
 */
public class Block
{
    private List predecessors, successors;
    private List liveEntry, liveExit;
    private List body;
    
    /**
     * Get the list of predecessor edges.  This list is mutable,
     * but should only be changed from within the associated
     * {@link ControlEdge} object.  The list contains only
     * <code>ControlEdge</code>s.
     *
     * @return  The list of predecessor edges to this block
     */
    public List getPredecessors()
    {
        return predecessors;
    }
    
    /**
     * Get the list of successor edges.  This list is mutable, but
     * should only be changed from within the associated {@link
     * ControlEdge} object.  The list contains only
     * <code>ControlEdge</code>s.
     *
     * @return  The list of successor edges to this block
     */
    public List getSuccessors()
    {
        return successors;
    }
    
    /**
     * Get the list of variables that are live on entry to this block.
     * This is returned as a list of <code>Operand</code>.  This list
     * is immutable.  This list is only guaranteed to be valid after a
     * call to {@link #buildLiveness}, and then only if the
     * control-flow graph has not changed since then.
     *
     * @return  an immutable list of operands representing variables
     *          live on entry to the block
     */
    public List getLiveOnEntry()
    {
        return Collections.unmodifiableList(liveEntry);
    }

    /**
     * Get the list of variables that are live on exit from this block.
     * This is returned as a list of <code>Operand</code>.  This list
     * is immutable.  This list is only guaranteed to be valid after a
     * call to {@link #buildLiveness}, and then only if the
     * control-flow graph has not changed since then.
     *
     * @return  an immutable list of operands representing variables
     *          live on exit from the block
     */
    public List getLiveOnExit()
    {
        return Collections.unmodifiableList(liveExit);
    }

    /**
     * Get the list of instructions in this block.  This is a mutable
     * ordered list of {@link Instruction}.  A given instruction
     * should exist in only one block; call {@link #updateParents} to
     * update the parent-block pointers of member instructions.  In
     * the absence of scheduling information to the contrary,
     * instructions execute serially in the order in the list.
     *
     * @return  a list of instructions that are the contents of this block
     */

    /**
     * Convert a region of a graph into a single flattened block.
     * This alters each of the instructions in container and its
     * children to point to the new block as their parent.  It may
     * also alter the instruction stream by inserting branch and
     * label instructions as necessary.
     *
     * @param container  CFG region to flatten
     * @returns          A single block containing all of the code
     *                   in <code>container</code>
     */
    public static Block flatten(BlockContainer container)
    {
    }
    
    /**
     * Convert a single block into a set of basic blocks.  Each block
     * contains no labels except for its first instruction, and no
     * branches except for its last instruction.  Thus, if the first
     * instruction in the block is executed, every instruction is
     * executed.  Instructions in this block are altered such that
     * their parent is a new block in the container.
     */
    public BlockContainer unflatten()
    {
    }

    /**
     * Update liveness information for a region of a control-flow
     * graph.  This update includes updating live-on-entry and
     * live-on-exit information as well as SSA annotations.  It will
     * also alter the instruction stream to insert phi nodes for SSA
     * where necessary, deleting all old phi nodes first.
     *
     * @param region  Region of the CFG to update liveness for
     */
    public static void buildLiveness(BlockContainer region)
    {
    }

    /**
     * Update ownership of contained instructions.  This iterates
     * through the list of instructions in the block and updates
     * each of them so that they point to the current block as
     * their parent.
     *
     * <p> This method could conceivably also check that the current
     * parent does not have the instruction in its instruction list.
     * This is not always appropriate; for example, this can be called
     * as part of <code>flatten</code> to change the parents of
     * instructions moved from one block to another, and it is
     * expected that the instructions will still point at their old
     * parent.  However, this method could also be used to give an
     * initial parent to added instructions, and in this case every
     * other instruction should have this block as a parent.
     */
    public void updateParents()
    {
    }
}
