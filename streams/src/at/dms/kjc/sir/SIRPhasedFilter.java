package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.util.Utils;
import at.dms.kjc.*;
import java.util.HashMap;

/**
 * A StreamIt phased filter.  Like SIRFilter, this has constant
 * overall I/O rates; however, the filter is divided into a set of
 * phases, which execute in some statically determined order.  Thus,
 * where the execution model for a normal filter is "wait until the
 * peek rate for the entire filter can be satisfied", or a phased
 * filter we only wait until the current phase can execute.
 */
public class SIRPhasedFilter extends SIRStream implements Cloneable 
{
    /**
     * The input and output types.  Each type is void if and only if this
     * is a source or sink, respectively.  This means that *all* phases
     * must have a 0 (peek and pop) or (push) rate.
     */
    private CType inputType, outputType;

    /**
     * Array of phases run by the filter.
     */
    private SIRWorkFunction[] initPhases, phases;

    public SIRPhasedFilter() 
    {
        this(null);
    }

    public SIRPhasedFilter(String ident)
    {
        super(null, ident, JFieldDeclaration.EMPTY(), JMethodDeclaration.EMPTY());
    }
    
    public SIRPhasedFilter(SIRContainer parent,
                           String ident,
                           JFieldDeclaration[] fields,
                           JMethodDeclaration[] methods,
                           SIRWorkFunction[] initPhases,
                           SIRWorkFunction[] phases,
                           JMethodDeclaration work,
                           CType inputType,
                           CType outputType)
    {
        super(parent, ident, fields, methods);
        this.initPhases = initPhases;
        this.phases = phases;
        this.inputType = inputType;
        this.outputType = outputType;
        if (work != null)
        {
            setWork(work);
        }
    }

    /**
     * Returns the type of this stream.
     */
    public LIRStreamType getStreamType() 
    {
        // Might want to create a new type.
        return LIRStreamType.LIR_FILTER;
    }

    /**
     * Copies the state of filter other into this.  Fields that are
     * objects will be shared instead of cloned.
     */
    public void copyState(SIRPhasedFilter other)
    {
        this.work = other.work;
        this.init = other.init;
        this.inputType = other.inputType;
        this.outputType = other.outputType;
        this.parent = other.parent;
        this.fields = other.fields;
        this.methods = other.methods;
        this.initPhases = other.initPhases;
        this.phases = other.phases;
        this.ident = other.ident;
    }
    
    /**
     * Accepts attribute visitor v at this node.
     */
    public Object accept(AttributeStreamVisitor v)
    {
        return v.visitPhasedFilter(this,
                                   fields,
                                   methods,
                                   init,
                                   work,
                                   initPhases,
                                   phases,
                                   inputType,
                                   outputType);
    }
    
    public void setInputType(CType t){
	this.inputType = t;
    }
    public CType getInputType(){
	return inputType;
    }

    public void setOutputType(CType t) {
	this.outputType = t;
    }
    public CType getOutputType() {
	return this.outputType;
    }

    public SIRWorkFunction[] getInitPhases() {
	return initPhases;
    }
    
    public void setInitPhases(SIRWorkFunction[] initPhases) {
	this.initPhases = initPhases;
    }
	
    public SIRWorkFunction[] getPhases() {
        return phases;
    }

    public void setPhases(SIRWorkFunction[] phases) {
        this.phases = phases;
    }

    public String toString() {
	return "SIRPhasedFilter name=" + getName();
    }

    public int getPushForSchedule(HashMap[] counts) {
	// not implementing this right now because I'm unclear if
	// there is a distinct execution count for each phase.  can
	// fix without too much trouble later. --bft
	Utils.fail("Don't yet support getPushForSchedule for phased filters.");
	return -1;
    }

    public int getPopForSchedule(HashMap[] counts) {
	// not implementing this right now because I'm unclear if
	// there is a distinct execution count for each phase.  can
	// fix without too much trouble later. --bft
	Utils.fail("Don't yet support getPopForSchedule for phased filters.");
	return -1;
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRPhasedFilter other = new at.dms.kjc.sir.SIRPhasedFilter();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRPhasedFilter other) {
  super.deepCloneInto(other);
  other.inputType = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.inputType, this);
  other.outputType = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.outputType, this);
  other.initPhases = (at.dms.kjc.sir.SIRWorkFunction[])at.dms.kjc.AutoCloner.cloneToplevel(this.initPhases, this);
  other.phases = (at.dms.kjc.sir.SIRWorkFunction[])at.dms.kjc.AutoCloner.cloneToplevel(this.phases, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
