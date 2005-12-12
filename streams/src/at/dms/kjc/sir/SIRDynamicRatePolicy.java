package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This represents a single policy for interpreting dynamic rates in a
 * given part of the compiler.  See SIRDynamicRateManager for the big
 * picture.
 */
public abstract class SIRDynamicRatePolicy {
    /**
     * Cache of the identity policy.
     */
    private static SIRDynamicRatePolicy identity =  
	new SIRDynamicRatePolicy() {
	    public JExpression interpretRate(JExpression rate) {
		return rate;
	    }
	};
    
    /**
     * Returns an identity policy, which interprets dynamic rates as
     * they were originally written.
     */
    static SIRDynamicRatePolicy identityPolicy() {
	return identity;
    }

    /**
     * Returns a policy which interprets all dynamic rates as a given
     * constant.
     */
    static SIRDynamicRatePolicy constantPolicy(final int constant) {
	return new SIRDynamicRatePolicy() {
		public JExpression interpretRate(JExpression rate) {
		    if (rate.isDynamic()) {
			// for dynamic rates, return the constant
			return new JIntLiteral(constant);
		    } else {
			// otherwise return the original rate.
			return rate;
		    }
		}
	    };
    }
    
    /**
     * Main function of any policy, for use by the rate-determining
     * functions of the compiler.  Given an expression denoting an I/O
     * rate, this function interprets that rate to a new expression.
     *
     * This is not public because it is only called from
     * SIRDynamicRateManager.
     */
    abstract JExpression interpretRate(JExpression rate);
}
