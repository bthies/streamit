package streamit.stair.core;

import java.util.List;
import java.util.Map;

/**
 * A single instruction on a target machine.  An instruction
 * fundamentally has an opcode, a list of sources, and a list of
 * destinations; labels and control-transfer instructions each have a
 * textual label associated with them as well.  Instructions also have
 * pointers to their containing <code>Block</code>s.
 *
 * <p> Instructions also have additional support for features that may
 * not be necessary on all architectures.  An instruction has a
 * single-<code>Operand</code> predicate for use on predicated
 * architectures; if the predicate is <code>null</code>, the
 * instruction always executes.  A <code>Scheduling</code> object may
 * be attached to each instruction.  Finally, an instruction can have
 * arbitrarily many string-keyed annotations.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: Instruction.java,v 1.1 2003-02-24 21:45:11 dmaze Exp $
 */
public class Instruction
{
    private Block block;
    private String label;
    private Opcode opcode;
    private List sources, dests;
    private Operand pred;
    private Scheduling sched;
    private Map annotations;

    /** Get the containing block of this instruction. */
    public Block getBlock() 
    {
        return block;
    }
    
    /**
     * Set the containing block of this instruction.  This function
     * should only be called from the <code>Block</code> code, for
     * consistency.
     */
    public void setBlock(Block block)
    {
        this.block = block;
    }
    
    /**
     * Get the label of this instruction.  If the instruction is a
     * label instruction, this is the sum of the state of the
     * instruction.  If the instruction is a control-transfer
     * instruction (jump or branch), the label contains the location
     * the branch goes to.
     */
    public String getLabel()
    {
        return label;
    }
    
    /** Set the label of this instruction. */
    public void setLabel(String label)
    {
        this.label = label;
    }

    /** Get the opcode of this instruction. */
    public Opcode getOpcode()
    {
        return opcode;
    }
    
    /**
     * Set the opcode of this instruction.  Note that this may have
     * the effect of changing the valid instruction format, and of
     * changing the target machine of the instruction.
     */
    public void setOpcode(Opcode opcode)
    {
        this.opcode = opcode;
    }
    
    /**
     * Get a <code>List</code> of source operands for this
     * instruction.  This list may be examined or mutated to get or
     * change the list of sources.  Every object in this list must be
     * an <code>Operand</code>.
     */
    public List getSources()
    {
        return sources;
    }
    
    /**
     * Get a <code>List</code> of destination operands for this
     * instruction.  This list may be examined or mutated to get or
     * change the list of destinations.  Every object in this list
     * must be an <code>Operand</code>.  This list should include
     * every operand that is given in the assembly format for the
     * instruction and is modified; for example, an instruction that
     * adds a constant to a register should include that register as
     * both a source and a destination, even if it is only included
     * once in the assembly.
     */
    public List getDests()
    {
        return dests;
    }

    /** Get the predicate operand for this instruction. */
    public Operand getPred()
    {
        return pred;
    }
    
    /**
     * Set the predicate operand for this instruction.  If
     * <code>null</code>, the instruction always executes.  Otherwise,
     * the instruction executes only if the predicate is true at
     * run-time.  The details of "execute" here are dependent on the
     * target architecture, but typically an instruction with a false
     * predicate will issue but not commit.
     */
    public void setPred(Operand pred)
    {
        this.pred = pred;
    }

    /** Get the scheduling information for this instruction. */
    public Scheduling getSched()
    {
        return sched;
    }
    
    /** Set the scheduling information for this instruction. */
    public void setSched(Scheduling sched)
    {
        this.sched = sched;
    }
    
    /** Get an annotation for this instruction.  Returns
     * <code>null</code> if the named annotation is not present. */
    public Object getAnnotation(String key)
    {
        return annotations.get(key);
    }

    /** Set or remove an annotation for this instruction.  If the
     * value is <code>null</code>, the annotation is removed. */
    public void setAnnotation(String key, Object value)
    {
        if (value == null)
            annotations.remove(key);
        else
            annotations.put(key, value);
    }
    
}
