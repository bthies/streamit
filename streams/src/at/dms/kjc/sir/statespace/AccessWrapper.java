package at.dms.kjc.sir.statespace;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.statespace.*;
import at.dms.kjc.iterator.*;


/**
 * This class represents an access expression for a field or array.
 * The field access expressions
 * provided by KOPI don't seem to do the right thing for equals -- eg they
 * implement strict object equality and I want them to use value equality.
 * (and I haven't tested the behavior of ArrayAccessExpressions, but I assume that
 * they also don't do the right thing for equals).
 * (eg two wrappers that
 * represent the same field should be equal).<br>
 *
 * As an interesting note on implementation in KOPI, it seems as though there are
 * two cases that we will accept as being valid "prefix" part of a JFieldAccessExpression.
 * The first is a JThisExpression if the field is a local field.
 * The second is a JLocalVariableExpression if the field is from a class
 * that is being used as a structure.<br>
 *
 * If the prefix is not one of the above things, then we give up and don't
 * wrap it.
 *
 * Also, we can wrap array accesses, but it gets tricky when combinations of
 * arrays and fields are used....<br>
 *
 * <H1>NOTE: I am not sure that the whole access wrapper thing actually works
 * This was too much of a reimplementation of ConstProp.
 * </H1>
 *
 * $Id: AccessWrapper.java,v 1.1 2004-02-09 17:55:01 thies Exp $ 
 **/

abstract class AccessWrapper {
    /** 
     * Used for generating a hash code. Should be the same for all
     * objects which represent the same value in the IR.
     **/
    String ident;

    /**
     * Create an Access wrapper with the specified name. Names don't need to be unique, but
     * they need to be the same for all wrappers that wrap the same
     * object (same being computed by value, not boject equality).
     **/
    public AccessWrapper(String name) {
	if (name == null) {throw new IllegalArgumentException("Null name passed to AccessWrapper constructor");}
	this.ident = name;
    }

    /** check identities. **/
    public boolean equals(Object o) {
	if (o == null) {return false;}
	if (!(o instanceof AccessWrapper)) {return false;}
	// cast for convenience
	AccessWrapper other = (AccessWrapper)o;
	if (other.ident.equals(this.ident)) {
	    return true;
	} else {
	    return false;
	}
    }

    /** Return the hashcode from the string identity. **/
    public int hashCode() {
	return this.ident.hashCode();
    }
    /** returns the identity string of this access wrapper. **/
    public String getIdent() {return this.ident;}

}
	    
    
	
