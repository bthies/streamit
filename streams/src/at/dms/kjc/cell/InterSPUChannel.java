package at.dms.kjc.cell;

import at.dms.kjc.CClassType;
import at.dms.kjc.JBlock;
import at.dms.kjc.JExpression;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JReturnStatement;
import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.slicegraph.Edge;

public class InterSPUChannel extends Channel {

    public InterSPUChannel(Edge e) {
        super(e);
    }
    
    @Override
    public JMethodDeclaration popMethod() {
        JBlock body = new JBlock();
        JMethodDeclaration retval = new JMethodDeclaration(
                null,
                /*at.dms.kjc.Constants.ACC_PUBLIC | at.dms.kjc.Constants.ACC_STATIC |*/ at.dms.kjc.Constants.ACC_INLINE,
                theEdge.getType(),
                popMethodName(),
                new JFormalParameter[0],
                CClassType.EMPTY,
                body, null, null);
        body.addStatement(
        new JReturnStatement(null,new JMethodCallExpression("pop",new JExpression[0]),null));
        return retval;
    }
}
