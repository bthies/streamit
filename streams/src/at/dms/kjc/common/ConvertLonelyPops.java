/**
 * 
 */
package at.dms.kjc.common;

import java.util.Iterator;
import java.util.HashSet;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFloatLiteral;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JLiteral;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.SLIRReplacingVisitor;
import at.dms.kjc.spacedynamic.RawExecutionCode;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;

/**
 * This class will convert each pop expression that is not nested
 * in another expression into an assignment of the pop expression to a 
 * compiler created variable.  This is needed because for raw we convert pops 
 * into register reads, and gcc optimizes out the register read if we don't 
 * use it for anything.
 * 
 * @author mgordon
 *
 */
public class ConvertLonelyPops implements Constants {
    /** The name of the var we create */
    public static final String VARNAME = "__lonely_receive__";
     
    public ConvertLonelyPops() {
      
    }
      
    /**
     * Visit the stream graph.
     * 
     * @param str The current stream.
     */
    public void convertGraph(SIRStream str) {
        if (str instanceof SIRFeedbackLoop) {
            SIRFeedbackLoop fl = (SIRFeedbackLoop) str;
            convertGraph(fl.getBody());
            convertGraph(fl.getLoop());
        }
        if (str instanceof SIRPipeline) {
            SIRPipeline pl = (SIRPipeline) str;
            Iterator iter = pl.getChildren().iterator();
            while (iter.hasNext()) {
                SIRStream child = (SIRStream) iter.next();
                convertGraph(child);
            }
        }
        if (str instanceof SIRSplitJoin) {
            SIRSplitJoin sj = (SIRSplitJoin) str;
            Iterator<SIRStream> iter = sj.getParallelStreams().iterator();
            while (iter.hasNext()) {
                SIRStream child = iter.next();
                convertGraph(child);
            }
        }
        if (str instanceof SIRFilter) {
            convert((SIRFilter)str);
        }
    }
    
    /**
     * Convert all lonely pops of the filter.
     * 
     * @param filter The filter.
     */
    public void convert(SIRFilter filter) {
        CType type = filter.getInputType();
        //don't do anything for no numeric types...
        if (!type.isNumeric())
            return;
        
        for (int i = 0; i < filter.getMethods().length; i++) {
            JMethodDeclaration meth = filter.getMethods()[i];
            //did we replace anything...
            final boolean[] replaced = {false};
            final JVariableDefinition varDef = 
                new JVariableDefinition(null, ACC_VOLATILE, type, VARNAME, 
                                        type.isFloatingPoint() ? (JLiteral) new JFloatLiteral(0) :
                                        (JLiteral)new JIntLiteral(0));
            //replace the lonely receives
            meth.getBody().accept(new SLIRReplacingVisitor() {
                    public Object visitExpressionStatement(JExpressionStatement self,
                                                           JExpression expr) {

                        //this this statement consists only of a method call and 
                        //it is a receive and it has zero args, the create a dummy assignment
                        if (expr instanceof SIRPopExpression) {
                            self.setExpression
                                (new JAssignmentExpression(new JLocalVariableExpression(varDef), 
                                                           expr));
                            replaced[0] = true;
                        }
            
                        return self;
                    }
                });
        
            //if we created an assignment expression, create a var def for the 
            //dummy variable
            if (replaced[0]) {
                meth.getBody().
                    addStatementFirst(new JVariableDeclarationStatement(null, varDef, null));
            }
        }
    }
}
