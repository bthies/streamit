package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.*;
import at.dms.util.Utils;
import java.util.*;
import streamit.frontend.FEIRToSIR;

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
     * The front-end syntax converter that created this class.
     */
    private FEIRToSIR feir2sir;
    
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
        this.feir2sir = null;
	this.expanded = null;
    }

    public SIRRecursiveStub(String className, FEIRToSIR feir2sir) 
    {
        super(null, "RecursiveStub_for_" + className,
              JFieldDeclaration.EMPTY(), JMethodDeclaration.EMPTY());
        this.className = className;
        this.kopi2SIR = null;
        this.feir2sir = feir2sir;
        this.expanded = null;
    }

    /**
     * Returns the expanded version of this stub, which is obtained by
     * parsing the original class definition one level deeper.
     */
    public SIRStream expand() {
	if (expanded==null) {
            if (kopi2SIR != null)
                expanded = (SIRStream)kopi2SIR.searchForOp(className);
            else
                expanded = feir2sir.findStream(className);
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

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRRecursiveStub other = new at.dms.kjc.sir.SIRRecursiveStub();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRRecursiveStub other) {
  super.deepCloneInto(other);
  other.className = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.className, other);
  other.kopi2SIR = (at.dms.kjc.Kopi2SIR)at.dms.kjc.AutoCloner.cloneToplevel(this.kopi2SIR, other);
  other.feir2sir = (streamit.frontend.FEIRToSIR)at.dms.kjc.AutoCloner.cloneToplevel(this.feir2sir, other);
  other.expanded = (at.dms.kjc.sir.SIRStream)at.dms.kjc.AutoCloner.cloneToplevel(this.expanded, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
