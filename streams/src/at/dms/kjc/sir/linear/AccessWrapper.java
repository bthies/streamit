package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;


/**
 * This class represents an access expression to a field or array.
 * The field access expressions
 * provided by KOPI don't seem to do the right thing for equals.
 * (and I haven't tested the behavior of ArrayAccessExpressions, but I assume that
 * they also don't do the right thing for equals).
 * Basically, JFieldAccessExpressions are compared using object equality
 * and I want them to be compared using value equality (eg two wrappers that
 * represent the same field should be equal).<p>
 *
 * As an interesting note on implementation in KOPI, it seems as though there are
 * two cases that we will accept as being valid "prefix" part of a JFieldAccessExpression.
 * The first is a JThisExpression if the field is a local field.
 * The second is a JLocalVariableExpression if the field is from a class
 * that is being used as a structure.<p>
 *
 * If the prefix is not one of the above things, then we give up and don't
 * wrap it.
 *
 * Also, we can wrap array accesses, but it gets tricky when combinations of arrays and fields are used....
 *
 * $Id: AccessWrapper.java,v 1.1 2002-09-04 19:05:59 aalamb Exp $
 **/

class AccessWrapper {
    /** space to hold the field name **/
    String fieldIdentifier;

    /**
     * Create a new access wrapper for the specified field name.
     **/
    AccessWrapper(String ident) {
	this.fieldIdentifier = ident;
    }

    // the this and local var versions are subclasses
    
    /**
     * Factory method for creating FeildAccessWrappers. Returns null
     * if the field can not be wrapper (see rules in main class description)
     * otherwise returns a wrapper for this field.
     **/
    public static AccessWrapper wrapFieldAccess(JFieldAccessExpression expr) {
	if (expr == null) {throw new RuntimeException("null arg passed to wrapFieldAccess");}
	// get the prefix of the field access (eg the structure/object that it belongs to)
	JExpression prefix = expr.getPrefix();

	// dispatch on type (bad OO, so sue me).
	if (prefix instanceof JThisExpression) {
	    return new ThisAccessWrapper(expr.getIdent());
	} else if (prefix instanceof JLocalVariableExpression) {
	    return new LocalVariableAccessWrapper(expr.getIdent(),
						       ((JLocalVariableExpression)prefix).getVariable());
	} else {
	    System.err.println("Warning: can't wrap a FieldAccessExpression with prefix : " + prefix);
	    return null;
	}
    }

    /**
     * Returns true if the specified object represents the same
     * field access expression as this.
     **/
    public boolean equals(Object o) {
	if (o == null) {return false;}
	AccessWrapper other = (AccessWrapper)o;
	if (other.fieldIdentifier.equals(this.fieldIdentifier)) {
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * returns the hashcode from the identity string.
     * simple way to preserve the semantics of equals --
     * if two objects are equal, then their hashCode()
     * methods return the same value.
     **/
    public int hashCode() {
	return this.fieldIdentifier.hashCode();
    }
}

/**
 * Represents a local field (eg of the form this.fieldname).
 **/
class ThisAccessWrapper extends AccessWrapper {
    public ThisAccessWrapper(String ident) {
	super(ident);
    }
    public String toString() {
	return ("<this." +this.fieldIdentifier + ">");
    }
}

/**
 * Represents a field that is accessed via a local variable.
 **/
class LocalVariableAccessWrapper extends AccessWrapper {
    JLocalVariable localVariable;
    
    public LocalVariableAccessWrapper(String ident,
					   JLocalVariable var) {
	super(ident);
	this.localVariable = var;
    }

    /** update the equals method to also check that the vars are the same. **/
    public boolean equals(Object o) {
	// if the types of object and the field names don't match
	// then we are done.
	if (!super.equals(o)) {
	    return false;
	}

	// check for the correct type
	if (!(o instanceof LocalVariableAccessWrapper)) {
	    return false;
	}
	// casting magic
	LocalVariableAccessWrapper other = (LocalVariableAccessWrapper)o;
	// remember the super class already checked the field name for us,
	// all we have to do is to check the variable.
	if (other.localVariable.getIdent().equals(this.localVariable.getIdent())) {
	    return true;
	} else {
	    return false;
	}
    }
    public String toString() {
	return ("<" + this.localVariable + "." +this.fieldIdentifier + ">");
    }

}
	    


