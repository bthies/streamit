
package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;

public class FindTapeOps extends SLIREmptyVisitor {

    public static boolean findTapeOps(JStatement body) {
    	FindTapeOps test = new FindTapeOps();
	body.accept(test);
	//System.out.println("FindTapeOps.findTapeOps result = "+test.tape_op);
	return test.tape_op;
    }


    // private fields and methods

    private boolean tape_op;

    public FindTapeOps() { tape_op = false; }
    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
	tape_op = true;
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
	tape_op = true;
    }    

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {
	tape_op = true;
    }
}
