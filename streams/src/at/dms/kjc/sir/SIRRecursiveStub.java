package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.*;
import at.dms.util.Utils;
import java.util.*;

/**
 * This class represents a stub for something that was found to be
 * recursive in the front-end, and needs to be expanded within the IR.
 * All instances should be eliminated in the expansion of the stream
 * graph, and it should never be instantiated during normal IR manipulation.
 */
public class SIRRecursiveStub extends SIRStream implements Cloneable {
    /**
     * The name of the class that this should be expanded into.
     */
    private String className;
    /**
     * A snapshot (clone) of Kopi2SIR when this class was encountered.
     */
    private Kopi2SIR kopi2SIR;
    /**
     * The expanded version of this.  Initially null, then memoized to
     * prevent duplicate expansions.
     */
    private SIRStream expanded;

    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    protected SIRRecursiveStub() {
	super();
    }
    
    public SIRRecursiveStub(String className, Kopi2SIR kopi2SIR) {
	super(null, "RecursiveStub_for_" + className, 
	      JFieldDeclaration.EMPTY(), JMethodDeclaration.EMPTY());
	this.className = className;
	this.kopi2SIR = kopi2SIR;
	this.expanded = null;
    }

    /**
     * Returns the expanded version of this stub, which is obtained by
     * parsing the original class definition one level deeper.
     */
    public SIRStream expand() {
	if (expanded==null) {
	    expanded = (SIRStream)kopi2SIR.searchForOp(className);
	}
	return expanded;
    }

    /**
     * Shouldn't be calling this.
     */
    public CType getOutputType() {
	Utils.fail("Can't get output type of recursive stub.");
	return null;
    }

    /**
     * Shouldn't be calling this.
     */
    public CType getInputType() {
	Utils.fail("Can't get input type of recursive stub.");
	return null;
    }

    /**
     * Shouldn't be calling this.
     */
    public int getPushForSchedule(HashMap[] counts)
    {
        Utils.fail("Can't get push for schedule of recursive stub.");
	return -1;
    }

    /**
     * Shouldn't be calling this.
     */
    public int getPopForSchedule(HashMap[] counts)
    {
        Utils.fail("Can't get pop for schedule of recursive stub.");
	return -1;
    }

    /**
     * Shouldn't be calling this.
     */
    public LIRStreamType getStreamType() {
	Utils.fail("Can't get stream type of recursive stub.");
	return null;
    }

    /**
     * Shouldn't be calling this.
     */
    public Object accept(AttributeStreamVisitor v) {
	Utils.fail("Can't visit RecursiveStub.");
	return null;
    }
}
