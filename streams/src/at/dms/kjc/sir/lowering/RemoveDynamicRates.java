package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;

/**
 * NOTE: This class is deprecated in favor of SIRDynamicRateManager.
 * (However, this class still might be useful if we want to replace
 * all dynamic rates with something in the IR.)
 *
 * This class performs an UNSAFE TRANSFORMATION in which all dynamic
 * rates are replaced with the constant 1.  This is an easy way to
 * push dynamic rates through backends such as the cluster backend,
 * where the rate information is not used for anything critical but
 * needs to be resolved to some constant.
 */
public class RemoveDynamicRates extends EmptyStreamVisitor {

    private RemoveDynamicRates() {}

    public static void doit(SIRStream str) {
        IterFactory.createFactory().createIter(str).accept(new RemoveDynamicRates());
    }
     
    public void preVisitStream(SIRStream self, SIRIterator iter) {
        SLIRRemoveDynamicRates replacer = new SLIRRemoveDynamicRates();
        JMethodDeclaration[] methods = self.getMethods();
        for (int i=0; i<methods.length; i++) {
            // replace push, pop, peek values in all methods.  This
            // should cover work, initWork, helpers, utils, etc.
            methods[i].setPush((JExpression)methods[i].getPush().accept(replacer));
            methods[i].setPop((JExpression)methods[i].getPop().accept(replacer));
            methods[i].setPeek((JExpression)methods[i].getPeek().accept(replacer));
        }
    }


    /**
     * Visit SLIR and replace all dynamic rates with the constant 1.
     */
    class SLIRRemoveDynamicRates extends SLIRReplacingVisitor {
        public Object visitRangeExpression(SIRRangeExpression self) {
            //TODO: eventually change to self.max or 1000 if max dynamic 
            // for now: to push MPEG through cluster need size 
            // of apps/benchmarks/mpeg2/input/momessage.m2v in bits rounded up
            // to a multiple of 32.
            return new JIntLiteral(/*625032*/1000000);
        }
    }
}
