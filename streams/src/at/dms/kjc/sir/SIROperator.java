package at.dms.kjc.sir;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.lowering.LoweringConstants;

import java.util.*;
import java.io.*;

/**
 * This represents an operator in the stream graph.
 */
public abstract class SIROperator implements Finalizable, Serializable, DeepCloneable {
    /**
     * The stream structure containing this, or NULL if this is the
     * toplevel stream.
     */
    private SIRContainer parent;
    
    // Revolting hack: there is some processing (flatgraph.StaticStreamGraph.setTopLevelSIR for instance)
    // where object data structures containing SIROperators are replaced.  We need to keep data about the
    // SIROperators, e.g. whether they are on the boundary between StaticStreamGraph's and the safest place
    // to keep that infomation seems to be in the SIROperators themselves, assuming that they are mutated
    // and rearranged, but not recreated during processing where the data is needed.

    // anyway, here goes.  Attributes are not clonable: if the
    // object is cloned then attributes are shared by all clones.
    private Map<String,Object> attributes = new HashMap<String,Object>();
    /** a property list implementation :^( */
    public Map<String,Object> getAttributes() { return attributes; }
    /** a property list implementation :^( */
    public void clearAttribute(String key) {attributes.remove(key); }
    /** a property list implementation :^( */
    public void setAttribute(String name, Object attribute) {attributes.put(name,attribute);}
    /** a property list implementation :^( */
    public void clearAllAttributes() {attributes = new HashMap<String,Object>();}
    
    // tell clone generator to not attempt to clone the attributes field
    public static final String[] DO_NOT_CLONE_THESE_FIELDS = {"attributes"};
    
    /**
     * Constructs and operator with parent <parent>.
     */
    protected SIROperator(SIRContainer parent) {
        this.setParent(parent);
    }

    protected SIROperator() {
       this(null);
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(SIRContainer parent) {
        this.parent = parent;
    }
    /**
     * @return the parent
     */
    public SIRContainer getParent() {
        return parent;
    }
    /**
     * Returns list of all parents.  The first element of the list is
     * the immediate parent of this, and the last element is the final
     * non-null ancestor of this.
     */
    public SIRContainer[] getParents() {
        LinkedList<SIRContainer> result = new LinkedList<SIRContainer>();
        SIRContainer parent = this.getParent();
        // make list of parents
        while (parent!=null) {
            result.add(parent);
            parent = parent.getParent();
        }
        return result.toArray(new SIRContainer[result.size()]);
    }

    /**
     * Get lists starting with <b>this</b> followed by its
     * ancestors in order of lookup through the parent relation.
     * @return reflexive closure of parent relation, in order.
     */
    public List<SIROperator> getAncestors() {
        LinkedList<SIROperator> result = new LinkedList<SIROperator>();
        SIROperator parent = this;
        while (parent!=null) {
            result.add(parent);
            parent = parent.getParent();
        }
        return result;
    }
    
    /**
     * TO BE REMOVED once immutable IR is in place.
     *
     * Returns an expression that accesses the structure of the parent
     * of this, assuming that the toplevel structure of the last
     * parent is named as in LoweringConstants.getDataField()
     */
    public JExpression getParentStructureAccess() {
        // get parents of <str>
        SIRStream parents[] = getParents();

        // construct result expression
        JExpression result = LoweringConstants.getDataField();
    
        // go through parents from top to bottom, building up the
        // field access expression.
        for (int i=parents.length-2; i>=0; i--) {
            // get field name for child context
            String childName = parents[i].getRelativeName();
            // build up cascaded field reference
            result = new JFieldAccessExpression(/* tokref */
                                                null,
                                                /* prefix is previous ref*/
                                                result,
                                                /* ident */
                                                childName);
        }
    
        // return result
        return result;
    }

    /**
     * TO BE REMOVED once immutable stuff is in place.
     */
    public String getRelativeName() {
        if (getParent()==null) {
            return null;
        } else {
            if (getParent() instanceof SIRFeedbackLoop) {
                if (this==((SIRFeedbackLoop)getParent()).getLoop()) {
                    return "loop";
                } else {
                    return "body";
                }
            } else {
                assert getParent().indexOf((SIRStream)this)!=-1:
                    "Stream's parent doesn't contain it.  Stream is " +
                    this + ", parent is " + getParent();
                return "child_" + getParent().indexOf((SIRStream)this);
            }
        }
    }
    
    public abstract Object accept(AttributeStreamVisitor v);

    // ----------------------------------------------------------------------
    // CLONING STUFF
    // ----------------------------------------------------------------------

    private Object serializationHandle;
    
    private void writeObject(ObjectOutputStream oos) throws IOException {
        this.serializationHandle = ObjectDeepCloner.getHandle(getParent());
    
        if (((Integer)serializationHandle).intValue()>=0) {
            // if we got a handle, erase our parent for the write object call
            SIRContainer temp = this.getParent();
            this.setParent(null);
            oos.defaultWriteObject();
            this.setParent(temp);
        } else {
            // otherwise just write the parent
            oos.defaultWriteObject();
        }
    }
    
    protected Object readResolve() throws Exception {
        Object o = ObjectDeepCloner.getInstance(serializationHandle, this);
        if (o!=this) {
            // if we had a handle, reset parent before returning
            this.setParent((SIRContainer)o);
        }
        return this;
    }

    // ----------------------------------------------------------------------

    /**
     * Returns an identifier for this which is NOT unique.  This will
     * return the same string for a given type of filter that was
     * added to the stream graph.
     */
    public abstract String getIdent();

    /**
     * Returns a UNIQUE name for this.  That is, if a given stream
     * operator was added in multiple positions of the stream graph,
     * then this will return a different name for each instantiation.
     */
    public String getName() {
        return getIdent() + "_" + Namer.getUniqueNumber(this);
    }

    /**
     * Returns the name of this, truncated to 'n' characters.  If the
     * name overflows, the last three characters will be replaced by ...
     */
    public String getShortIdent(int n) {
        String ident = getIdent();
        if (ident.length()>n) {
            if (n>3) {
                return ident.substring(0,n-3) + "...";
            } else {
                return ident.substring(0,3);
            }
        } else {
            return ident;
        }
    }

    /**
     * Returns a UNIQUE integer for this.  That is, if a given stream
     * operator was added in multiple positions of the stream graph,
     * then this will return a different name for each instantiation.
     */
    public int getNumber() {
        return Namer.getUniqueNumber(this);
    }


    public int hashCode() {
        return Namer.getUniqueNumber(this);
    }

    /**
     * This is only for the efficiency of the Namer (class included
     * below)... need a way to maintain mapping of the hashcodes
     * themselves.  Do this using the memory-based hashcode.
     */
    private int origHashCode() {
        return super.hashCode();
    }

    /**
     * This should be called in every mutator.
     */
    public void assertMutable() {
        assert !IterFactory.isFinalized(this):
            "A mutability check failed.";
    }

    /**
     * Just abstracting the naming methods and fields into their own
     * unit... no other reason for this class.
     */
    private static class Namer {
        /**
         * Mapping from original hash codes of SIROperators to
         * Integers that represent the unique identifier for that
         * object.  Use a mapping that is maintained in a
         * demand-driven way instead of a field to simplify
         * cloning/serialization issues.
         */
        private static HashMap<Integer, Integer> opToNumber = new HashMap<Integer, Integer>();
        /**
         * The last number assigned to an SIROperator.
         */
        private static int MAX_NUMBER = -1;
    
        /**
         * Return a unique number for <op>, that is deterministic between
         * multiple runs of the program (does not depend on memory
         * allocation).
         */
        static int getUniqueNumber(SIROperator op) {
            Integer key = new Integer(op.origHashCode());
            if (opToNumber.containsKey(key)) {
                return opToNumber.get(key).intValue();
            } else {
                // otherwise, register a number for it
                int result = ++MAX_NUMBER;
                opToNumber.put(key, new Integer(result));
                return result;
            }
        }
    }

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.sir.SIROperator other) {
        other.setParent(this.getParent());
        other.serializationHandle = this.serializationHandle;
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
