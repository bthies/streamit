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
     * Returns a policy which estimates an average for dynamic rates.
     * This _always_ returns a JIntLiteral for a SIRRangeExpression.
     * (but only returns a JIntLiteral for a JintLiteral or SIRRangeExpression).
     * 
     * For a SIRRangeExpression the estimate is the first of the following that works:
     * (1) If the average is a JIntLiteral return the average.
     * (2) If both max and min are JIntLiterals, return average of
     *     min and sqrt(max)  -- in case max set very large.
     * (3) If min is a JIntLiteral but max is not, return min * 2, but not < 1.
     * (4) If max is a JIntLiteral but min is not, return sqrt(max).
     * (5) Return 1.
     */
    static SIRDynamicRatePolicy estimatePolicy() {
        return new SIRDynamicRatePolicy() {
                public JExpression interpretRate(JExpression rate) {
                    if (rate.isDynamic()) {
                        SIRRangeExpression r = (SIRRangeExpression)rate;
                        if (r.getAve() instanceof JIntLiteral) {
                            return r.getAve();
                        }
                        JExpression min = r.getMin();
                        JExpression max = r.getMax();
                        int maxValSqrt = 0;
                        if (max instanceof JIntLiteral) {
                            int i = ((JIntLiteral)max).intValue();
                            maxValSqrt = (int) Math.sqrt(i);
                        }
                        if (min instanceof JIntLiteral) {
                            if (max instanceof JIntLiteral) {
                                return new JIntLiteral(Math.min( 
                                        (((JIntLiteral)min).intValue() + maxValSqrt) / 2,
                                        1));
                            } else {
                                return new JIntLiteral(Math.min(((JIntLiteral)min).intValue() * 2,
                                        1));
                            }
                        }
                        if (max instanceof JIntLiteral) {
                            return new JIntLiteral(Math.min(maxValSqrt, 1));
                        }
                    }
                    return new JIntLiteral(1);  // no info.
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
