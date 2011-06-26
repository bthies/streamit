package at.dms.kjc.sir;

import at.dms.compiler.PositionedError;
import at.dms.kjc.AttributeVisitor;
import at.dms.kjc.CExpressionContext;
import at.dms.kjc.CStdType;
import at.dms.kjc.CType;
import at.dms.kjc.CodeSequence;
import at.dms.kjc.ExpressionVisitor;
import at.dms.kjc.JExpression;
import at.dms.kjc.KjcVisitor;
import at.dms.kjc.SLIRAttributeVisitor;
import at.dms.kjc.SLIRVisitor;

public class SIRIterationExpression extends JExpression{

        @Override
        public CType getType() {
            return CStdType.Integer;
        }

        @Override
        public void setType(CType type) {
        	return;
        }
        
        /**
         * Throws an exception (NOT SUPPORTED YET)
         */
        @Override
        public JExpression analyse(CExpressionContext context)
                        throws PositionedError {
	        at.dms.util.Utils.fail("Analysis of custom nodes not supported yet.");
	        return this;
        }

		@Override
		public void accept(KjcVisitor p) {
	        if (p instanceof SLIRVisitor) {
	            ((SLIRVisitor)p).visitIterationExpression(this); 
	        } else {
	            // otherwise, do nothing
	        }
		}
        
        @Override
        public Object accept(AttributeVisitor p) {
            if (p instanceof SLIRAttributeVisitor) {
                return ((SLIRAttributeVisitor)p).visitIterationExpression(this);
            } else {
                return this;
            }
        }

        @Override
        public <S, T> S accept(ExpressionVisitor<S, T> p, T d) {
        	return p.visitIter(this,d);
        }


        /**
         * Generates JVM bytecode to evaluate this expression.  NOT SUPPORTED YET.
         *
         * @param   code        the bytecode sequence
         * @param   discardValue    discard the result of the evaluation ?
         */
        @Override
        public void genCode(CodeSequence code, boolean discardValue) {
            at.dms.util.Utils.fail("Visitors to custom nodes not supported yet.");
        }

        /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

        /** Returns a deep clone of this object. */
        public Object deepClone() {
            at.dms.kjc.sir.SIRIterationExpression other = new at.dms.kjc.sir.SIRIterationExpression();
            at.dms.kjc.AutoCloner.register(this, other);
            deepCloneInto(other);
            return other;
        }

        /** Clones all fields of this into <pre>other</pre> */
        protected void deepCloneInto(at.dms.kjc.sir.SIRIterationExpression other) {
            super.deepCloneInto(other);
        }

        /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

}
