package at.dms.kjc.sir;

import at.dms.kjc.*;
import java.util.Stack;

/**
 * This class governs how dynamic rates are interpreted at a given
 * point in the compiler.  The idea is that compiler passes sometimes
 * want to view dynamic rates in different ways -- for example,
 * treating a dynamic rate as a fixed constant, as an upper bound, or
 * as an exceptional case.  
 *
 * This class basically encapsulates a dynamic variable, which
 * indicates the current policy for interpreting dynamic rates
 * everywhere in the compiler.  The methods should be called (for
 * example) from the main backend passes to specify how all dynamic
 * rates should be interpreted for a given portion of the compiler.
 *
 * To make the calling convention more natural, the class exposes a
 * stack paradigm where a stack of policies is maintained.  The
 * top-most element of the stack is always used for interpreting
 * rates.  One can change the policy by pushing on a new policy, or by
 * popping off the old policy.
 */
public class SIRDynamicRateManager {
    /**
     * Stack of policies being employed.  The top-most element of the
     * stack is the current policy.
     */
    private static Stack<SIRDynamicRatePolicy> policies = new Stack<SIRDynamicRatePolicy>();
    // default policy is identity policy
    static {
        pushIdentityPolicy();
    }
    
    /**
     * For use by the rate-determining functions of the compiler.
     * Given an expression denoting an I/O rate, this function uses
     * the current policy to interpret that rate.
     */
    public static JExpression interpretRate(JExpression rate) {
        return policies.peek().interpretRate(rate);
    }

    /**
     * Sets the current policy to an identity policy (which returns
     * all rates unaltered).  Pushes this policy onto the stack.
     */
    public static void pushIdentityPolicy() {
        policies.push(SIRDynamicRatePolicy.identityPolicy());
    }

    /**
     * Sets the current policy to a constant policy (returns a given
     * <constant> for all dynamic rates).  Pushes this policy onto the
     * stack.
     */
    public static void pushConstantPolicy(int constant) {
        policies.push(SIRDynamicRatePolicy.constantPolicy(constant));
    }

    /**
     * Pops a policy off the stack.
     */
    public static void popPolicy() {
        policies.pop();
        // fail early -- should never pop default policy off the stack
        if (policies.empty()) {
            throw new RuntimeException("Error: popped more SIRDynamicRatePolicy's than pushed.");
        }
    }
}
