package at.dms.kjc.rstream;

import at.dms.kjc.common.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Vector;
import at.dms.util.Utils;

/**
 * This class traverses the IR looking for do loops that will never execute
 * or will execute exactly once.  It then removes dead loops and removes the loop
 * header for loops that execute once.
 *
 * @author Michael Gordon
 * 
 */

public class RemoveDeadDoLoops extends SLIRReplacingVisitor implements FlatVisitor
{
    private HashMap doloops;

    /**
     * Remove dead for loops and remove the header for loops that will
     * execute exactly once.
     *
     *
     * @param node The top level flat node of the app
     * @param loops A hashmap from JForStatemet->DoLoopInformation (see 
     * IDDoLoops.java)
     */

    public static void doit(FlatNode node, HashMap loops) 
    {
	new RemoveDeadDoLoops(node, loops);
    }
    
    
    private RemoveDeadDoLoops(FlatNode node, HashMap doloops) 
    {
	this.doloops = doloops;
	node.accept(this, null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()) {
	    SIRFilter filter = (SIRFilter)node.contents;
	    //visit all the methods of the filter...
	    for (int i = 0; i < filter.getMethods().length; i++) {
		filter.getMethods()[i].accept(this);
	    }
	}
    }
    
    
    public Object visitForStatement(JForStatement self,
				    JStatement init,
				    JExpression cond,
				    JStatement incr,
				    JStatement body) {
	System.out.println("For Loop");
	
	if (StrToRStream.GENERATE_DO_LOOPS && doloops.containsKey(self)) {
	    DoLoopInformation doInfo = (DoLoopInformation)doloops.get(self);
	    
	    //find out if we should generate the do loop or not, 
	    int whatToDo = generateDoLoop(doInfo, body);
	    
	    //don't generate the do loop at all
	    if (whatToDo < 0) {
		System.out.println("Removing Do loop");
		return new JEmptyStatement(null, null);
	    }
	    //it executes once...
	    if (whatToDo == 0) {
		System.out.println("Removing Do loop (keeping body)");
		//remember to recurse into the body
		return (JStatement)body.accept(this);
		
	    }
	}

	//if we made it here, we are keeping the do loop, so just
	//check the rest of the statements that compose the for loop
	// recurse into init
	JStatement newInit = (JStatement)init.accept(this);
	if (newInit!=null && newInit!=init) {
	    self.setInit(newInit);
	}
	
	// recurse into incr
	JStatement newIncr = (JStatement)incr.accept(this);
	if (newIncr!=null && newIncr!=incr) {
	    self.setIncr(newIncr);
	}
	
	// recurse into body
	JStatement newBody = (JStatement)body.accept(this);
	if (newBody!=null && newBody!=body) {
	    self.setBody(newBody);
	}
	return self;
    }
    
    
    /** Decide if the do loop every executes, return 
	-1 if don't generate it,
	0 if it executes exactly once,
	1 generate the do loop.
    **/
    private int generateDoLoop(DoLoopInformation doInfo, JStatement body) 
    {
	//make sure the init and the incr are integers
	if (Util.passThruParens(doInfo.incr) instanceof JIntLiteral &&
	    Util.passThruParens(doInfo.init) instanceof JIntLiteral) {

	    JIntLiteral incr = (JIntLiteral)Util.passThruParens(doInfo.incr);
	    JIntLiteral init = (JIntLiteral)Util.passThruParens(doInfo.init);
	    JIntLiteral cond = null;

	    //if the cond is a int literal we are set
	    if (Util.passThruParens(doInfo.cond) instanceof JIntLiteral) {
		cond = (JIntLiteral)Util.passThruParens(doInfo.cond);
	    }
	    
		    
	    //if the cond is a binary expression, try to fold it
	    if (Util.passThruParens(doInfo.cond) instanceof JBinaryArithmeticExpression) {
		JBinaryArithmeticExpression binExp = 
		    (JBinaryArithmeticExpression)Util.passThruParens(doInfo.cond);
		if (Util.passThruParens(binExp.getLeft()) instanceof JIntLiteral && 
		    Util.passThruParens(binExp.getRight()) instanceof JIntLiteral) {
		    JIntLiteral right = (JIntLiteral)Util.passThruParens(binExp.getRight());
		    JIntLiteral left = (JIntLiteral)Util.passThruParens(binExp.getLeft());
		    
		    //perform the folding
		    if (Util.passThruParens(doInfo.cond) instanceof JAddExpression) {
			cond = new JIntLiteral(left.intValue() + right.intValue());
			doInfo.cond = cond;
		    }
		    else if (Util.passThruParens(doInfo.cond) instanceof JMinusExpression) {
			cond = new JIntLiteral(left.intValue() - right.intValue());
			doInfo.cond = cond;
		    }
		}
	    }
	    
	    //couldn't fold the cond expression, so we must generate the loop or
	    //if there are any side effects in the code or inrc or init, we must generate the loop
	    if (cond == null || HasSideEffects.hasSideEffects(doInfo.init) ||
		HasSideEffects.hasSideEffects(doInfo.cond) ||
		HasSideEffects.hasSideEffects(doInfo.incr))
		return 1;
	    
	    //System.out.println(init.intValue() + " " + 
	    //		       cond.intValue() + " " + 
	    //		       incr.intValue());

	    //this loop with never execute...
	    if ((init.intValue() > cond.intValue() && incr.intValue() > 0) ||
		 (init.intValue() < cond.intValue() && incr.intValue() < 0))
		return -1;
		 
	    //this loop will execute once, make sure the induction variable is not used 
	    //in the body, we could still handle this case, but I am just punting for now
	    if (init.intValue() == cond.intValue() &&		
		!(VariablesDefUse.getVars(body).contains(doInfo.induction)))
		return 0;
	}
	return 1;
    }
    
}
