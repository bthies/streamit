
package at.dms.kjc.cluster;

import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;

class IncreaseFilterMult implements StreamVisitor {

    private int mult;

    public IncreaseFilterMult(int mult) {
	this.mult = mult;
    }

    static public void inc(SIRStream str, int mult) {
	IterFactory.createFactory().createIter(str).accept(new IncreaseFilterMult(mult));
    }
    
    public void visitFilter(SIRFilter filter,
		     SIRFilterIter iter) { 
	
	System.out.print("IncMult visiting: "+filter.getName());
	
	JMethodDeclaration work = filter.getWork();

	System.out.print(" work: "+work.getName());


	//
	// adding a work2 method
	//
	
	JBlock block = new JBlock(null, new JStatement[0], null);

	JVariableDefinition counter = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    "i",
				    null);
	

	JExpression initExpr =
	    new JAssignmentExpression(null,
				      new JLocalVariableExpression(null,
								   counter),
				      new JIntLiteral(0));

	JStatement init = new JExpressionStatement(null, initExpr, null);

	JExpression incrExpr = 
	    new JPostfixExpression(null, 
				   Constants.OPE_POSTINC, 
				   new JLocalVariableExpression(null,
								   counter));

	JStatement incr = 
	    new JExpressionStatement(null, incrExpr, null);

	JExpression cond = 
	    new JRelationalExpression(null,
				      Constants.OPE_LT,
				      new JLocalVariableExpression(null,counter),
				      new JIntLiteral(mult));
	
	JMethodCallExpression callExpr = 
	    new JMethodCallExpression(null, 
				      new JThisExpression(null), 
				      work.getName()+"__2", 
				      new JExpression[0]);

	JStatement call = new JExpressionStatement(null, callExpr, null);

	//JBlock do_block = new JBlock(null, new JStatement[0], null);
	//do_block.addStatement(call);
	//do_block.addStatement(incr);
	

	block.addStatement(
             new JVariableDeclarationStatement(null, counter, null));
	block.addStatement(init);

	JBlock for_block = new JBlock(null, new JStatement[0], null);
	for_block.addStatement(call);

	block.addStatement(new JForStatement(null, init, cond, incr, for_block,
					     null));


	//block.addStatement(
        //     new JDoStatement(null, cond, do_block, null));


	//JBlock body = new JBlock(null, new JStatement[0], null);
	//body.addStatement(new JExpressionStatement(null, new JMethodCallExpression(null, work.getName(), new JExpression[0]), null));

	JBlock work_body = work.getBody();

	JMethodDeclaration old_work = 
	    new JMethodDeclaration(null, 
				   at.dms.kjc.Constants.ACC_PUBLIC,
				   CStdType.Void,
				   work.getName()+"__2",
				   JFormalParameter.EMPTY,
				   CClassType.EMPTY,
				   work_body,
				   null,
				   null);

	filter.addMethod(old_work); 



	JMethodDeclaration new_work = 
	    new JMethodDeclaration(null, 
				   at.dms.kjc.Constants.ACC_PUBLIC,
				   CStdType.Void,
				   work.getName(),
				   JFormalParameter.EMPTY,
				   CClassType.EMPTY,
				   block,
				   null,
				   null);

	filter.setWork(new_work); 

	//
	// increasing peek, pop, push rates
	//
	
	int pop = filter.getPopInt();
	int push = filter.getPushInt();
	int peek = filter.getPeekInt();
	int extra = 0;

	if (peek > pop) extra = peek - pop;

	filter.setPop(pop * mult);
	filter.setPush(push * mult);
	filter.setPeek(pop * mult + extra);

	System.out.println(" new work: "+filter.getWork().getName());
    }

    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
        // This is a stub; it'll get filled in once we figure out how phased
        // filters should actually work.
    }

    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {}
    
    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {}
    
    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}
    
    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {}
   
    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {}

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}

}
