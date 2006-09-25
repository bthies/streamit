/**
 * 
 */
package at.dms.kjc.common;

import java.util.Iterator;

import at.dms.kjc.CStdType;
import at.dms.kjc.CType;
import at.dms.kjc.JExpression;
import at.dms.kjc.SLIRReplacingVisitor;
import at.dms.kjc.sir.SIRFeedbackLoop;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRPipeline;
import at.dms.kjc.sir.SIRPushExpression;
import at.dms.kjc.sir.SIRSplitJoin;
import at.dms.kjc.sir.SIRStream;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;

/**
 * This class will search all push expressions and if a push expression 
 * has a pop expression as a arg, the arg will be first assigned to a 
 * temp var and then push'ed.
 * 
 * @author mgordon
 *
 */
public class SeparatePushPop extends SLIRReplacingVisitor {
    /** the name of the temp var that we use for the arg of the push */
    public static String TEMPVAR = "__temp_var__";
    
    /**
     * if a push expression has a pop expression as a arg, the arg 
     * will be first assigned to a temp var and then push'ed.
     * 
     * @param str
     */
    public static void doit(SIRStream str) {
        new SeparatePushPop().separatePushPop(str);
    }
    
    /**
     * Visit the str structure...
     * 
     * @param str
     */
    private void separatePushPop(SIRStream str) {
        if (str instanceof SIRFeedbackLoop) {
            SIRFeedbackLoop fl = (SIRFeedbackLoop) str;
            separatePushPop(fl.getBody());
            separatePushPop(fl.getLoop());
        }
        if (str instanceof SIRPipeline) {
            SIRPipeline pl = (SIRPipeline) str;
            Iterator iter = pl.getChildren().iterator();
            while (iter.hasNext()) {
                SIRStream child = (SIRStream) iter.next();
                separatePushPop(child);
            }
        }
        if (str instanceof SIRSplitJoin) {
            SIRSplitJoin sj = (SIRSplitJoin) str;
            Iterator<SIRStream> iter = sj.getParallelStreams().iterator();
            while (iter.hasNext()) {
                SIRStream child = iter.next();
                separatePushPop(child);
            }
        }
        if (str instanceof SIRFilter) {
            for (int i = 0; i < str.getMethods().length; i++) {
                str.getMethods()[i].accept(this);
            }
        }
    }
    
    /**
     * If push expression has a pop in the arg, then assign the arg
     * to a temp and then push the temp. 
     */
    public Object visitExpressionStatement(JExpressionStatement self,
            JExpression expr) {
        if (expr instanceof SIRPushExpression) {
            SIRPushExpression push = (SIRPushExpression)expr;
            if (containsPop(push.getArg())) {
                JBlock block = new JBlock();
                JVariableDefinition temp = 
                    new JVariableDefinition(null, 
                            0, 
                            push.getTapeType(),
                            TEMPVAR,
                            null);
                block.addStatement(new JVariableDeclarationStatement(temp));
                block.addStatement(
                        new JExpressionStatement(
                                new JAssignmentExpression(
                                        new JLocalVariableExpression(temp), 
                                        push.getArg())));
                block.addStatement(
                        new JExpressionStatement(
                                new SIRPushExpression(new JLocalVariableExpression(temp),
                                        push.getTapeType())));
                return  block;
            }
        } 
        return self;
    }

    /**
     * @param expr
     * @return True if <expr> contains a pop expression. 
     */
    public static boolean containsPop(JExpression expr) {
        final boolean foundPop[] = {false};
        
        expr.accept(new SLIREmptyVisitor() {
            public void visitPopExpression(SIRPopExpression self, CType tapeType) {
                foundPop[0] = true;
            }
        });
        return foundPop[0];
    }
 
}
