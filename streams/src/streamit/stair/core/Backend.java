package streamit.stair.core;

import java.util.ListIterator;

public interface Backend
{
    /** Return the name of the backend.  Used for debugging only. */
    public String getName();
    
    /**
     * Get an <code>Opcode</code> object with a particular name.
     * <code>Opcode</code> objects should generally be singletons;
     * the backend object is probably a good place to define the set of
     * every <code>Opcode</code> object that will ever exist.
     *
     * @param name  Name of the opcode to return
     * @returns     The unique opcode object named <code>name</code>
     */
    public Opcode getOpcode(String name);
    
    /**
     * Insert an instruction to spill an operand to the stack.
     * The instruction that contains the operand to be spilled is
     * immediately before <code>pos</code>.  After doing the
     * insertion, the iterator should point to after the inserted
     * instruction(s).
     *
     * @param pos   Location to insert instructions
     * @param op    Operand to spill
     */
    public void insertSpill(ListIterator pos, Operand op);
    
    /**
     * Insert an instruction to load a spilled operand from the stack.
     * The instruction that contains the operand to be loaded is
     * immediately before <code>pos</code>.  After doing the
     * insertion, the iterator should point to after the inserted
     * instruction(s).
     *
     * @param pos   Location to insert instructions
     * @param op    Operand to unspill
     */
    public void insertUnspill(ListIterator pos, Operand op);
    
    /**
     * Insert an unconditional branch to <code>target</code>.  After
     * doing the insertion, the iterator should point to after the
     * inserted instruction(s).  This function should not rebuild
     * the control-flow graph, even if it is necessary.
     *
     * @param pos    Location to insert instructions
     * @param target Target of the branch
     */
    public void insertUnconditionalBranch(ListIterator pos, String target);

    /**
     * Insert a conditional branch to <code>target</code> if
     * <code>cond</code> is true.  After doing the insertion, the
     * iterator should point to after the inserted instruction(s).
     * This function should not rebuild the control-flow graph, even
     * if it is necessary.
     *
     * @param pos    Location to insert instructions
     * @param cond   Condition to branch on
     * @param target Target of the branch
     */
    public void insertConditionalBranch(ListIterator pos,
                                        Operand cond,
                                        String target);
}
