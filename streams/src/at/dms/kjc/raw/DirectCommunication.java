package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;

//if 
//not 2 stage
//peek == pop
//no peek expression 
//all pops before pushe

public class DirectCommunication extends at.dms.util.Utils 
    implements Constants 
{
    public static boolean doit(SIRFilter filter) 
    {
	//runs some tests to see if we can 
	//generate code direct commmunication code
	if (filter instanceof SIRTwoStageFilter)
	    return false;
	if (filter.getPeekInt() > filter.getPopInt())
	    return false;
	if (CommunicationOutsideWork.check(filter))
	    return false;
	if (PeekFinder.findPeek(filter.getWork()))
	    return false;
	if (PushBeforePop.check(filter.getWork()))
	    return false;
	//all tests pass
	
	return true;
    }
    
}

class CommunicationOutsideWork extends SLIREmptyVisitor 
{
    private static boolean found;
    
    //returns true if we find communication statements/expressions
    //outside of the work function (i.e. in a helper function)
    public static boolean check(SIRFilter filter) 
    {
	for (int i = 0; i < filter.getMethods().length; i++) {
	    if (!filter.getMethods()[i].equals(filter.getWork())) {
		found = false;
		filter.getMethods()[i].accept(new CommunicationOutsideWork());
		if (found)
		    return true;
	    }
	}
	return false;
    }

     public void visitPeekExpression(SIRPeekExpression self,
				    CType tapeType,
				    JExpression arg) {
	 found = true;
    }

    public void visitPopExpression(SIRPopExpression self,
				   CType tapeType) {
	found = true;
    }

    public void visitPushExpression(SIRPushExpression self,
				    CType tapeType,
				    JExpression arg) {
	arg.accept(this);
    }
}


	    

class PushBeforePop extends SLIREmptyVisitor 
{
    private static boolean sawPush;
    private static boolean pushBeforePop;

    public static boolean check(JMethodDeclaration method) 
    {
	sawPush = false;
	pushBeforePop = false;
	
	method.accept(new PushBeforePop());
	return pushBeforePop;
    }

    public void visitPeekExpression(SIRPeekExpression self,
				    CType tapeType,
				    JExpression arg) {
	 Utils.fail("Should not see a peek expression");
    }

    public void visitPopExpression(SIRPopExpression self,
				   CType tapeType) {
	if (sawPush)
	    pushBeforePop = true;
    }

    public void visitPushExpression(SIRPushExpression self,
				    CType tapeType,
				    JExpression arg) {
	sawPush = true;
    }
    
    //for all loops, visit the cond and body twice to make sure that 
    //if a push statement occurs in the body and 
    //after all the pops, we will flag this as a 
    //case where a push comes before a pop
    
    
    public void visitWhileStatement(JWhileStatement self,
				    JExpression cond,
				    JStatement body) {
	cond.accept(this);
	body.accept(this);
	//second pass
	cond.accept(this);
	body.accept(this);
    }

    public void visitForStatement(JForStatement self,
				  JStatement init,
				  JExpression cond,
				  JStatement incr,
				  JStatement body) {
	if (init != null) {
	    init.accept(this);
	}
	if (cond != null) {
	    cond.accept(this);
	}
	if (incr != null) {
	    incr.accept(this);
	}
	body.accept(this);
	//second pass
	if (cond != null) {
	    cond.accept(this);
	}
	if (incr != null) {
	    incr.accept(this);
	}
	body.accept(this);
    }

    public void visitDoStatement(JDoStatement self,
				 JExpression cond,
				 JStatement body) {
	body.accept(this);
	cond.accept(this);
	//second pass
	body.accept(this);
	cond.accept(this);
    }
}


class PeekFinder extends SLIREmptyVisitor 
{
    private static boolean found;

    public static boolean findPeek(JMethodDeclaration method) 
    {
	found = false;
	method.accept(new PeekFinder());
	return found;
    }
    
     /**
      * if we find a peek expression set found to true;
     */
    public void visitPeekExpression(SIRPeekExpression self,
				    CType tapeType,
				    JExpression arg) {
	found = true;
    }
}
