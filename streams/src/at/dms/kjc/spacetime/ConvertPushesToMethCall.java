/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.ListIterator;

import at.dms.kjc.CType;
import at.dms.kjc.JExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.SLIRReplacingVisitor;
import at.dms.kjc.sir.SIRPushExpression;
import at.dms.kjc.slicegraph.FilterInfo;


/**
 * This class will convert all of the pushes into function calls.  It will
 * generate different function calls depending on whether the filter writes
 * its output to the gdn or the static network.
 * <p>
 * It is used by all subclasses of  
 * {@link at.dms.kjc.spacetime.RawExecutionCode} to convert push statements.
 * <p>
 * The method is then recognized by later passes and converted to the
 * correct generated code.
 * 
 * @author mgordon
 *
 */
public class ConvertPushesToMethCall extends SLIRReplacingVisitor {

    /** true if we are sending over the gdn*/
    private boolean dynamic;
   
    /**
     * Convert all of the pushes into function calles.  It will
     * generate different function calls depending on whether the filter writes
     * its output to the gdn or the static network.
     * 
     * @param filterInfo
     * @param gdnOutput True if we are using the gdn.
     */
    public static void doit(FilterInfo filterInfo, boolean gdnOutput) {
        
        ConvertPushesToMethCall convert = 
            new ConvertPushesToMethCall(gdnOutput);
        
        JMethodDeclaration[] methods = filterInfo.filter.getMethods();

        if(methods!=null)
            for (int i = 0; i < methods.length; i++) {
                //iterate over the statements and call the ConvertCommunication
                //class to convert peek, pop
                for (ListIterator it = methods[i].getStatementIterator();
                     it.hasNext(); ){
                    ((JStatement)it.next()).accept(convert);
                }
            }
    }
    
    /**
     * Create a new converted for either the gdn or the static network.
     * 
     * @param dynamic if true create a converter for the gdn.
     */
    private ConvertPushesToMethCall(boolean dynamic) {
        this.dynamic = dynamic;
    }
    
    /**
     * Visits a push expression and replaces the push expression with a method
     * call to the appropriate method depending on whether we are using the gdn or 
     * static network.
     */
    public Object visitPushExpression(SIRPushExpression self,
                                      CType tapeType,
                                      JExpression arg) {
        JExpression newExp = (JExpression)arg.accept(this);
        
        String methodIdent = (dynamic ? RawExecutionCode.gdnSendMethod :
            RawExecutionCode.staticSendMethod);
        
        JMethodCallExpression methCall = 
            new JMethodCallExpression(methodIdent, new JExpression[]{newExp});
        
        methCall.setTapeType(tapeType);
        
        return methCall;
    }   
}
