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
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;


/**
 * This class finds the array dimensions for a given variable
 */
public class ArrayDim extends SLIREmptyVisitor implements StreamVisitor{
    //the dimension of the array when found
    private JExpression[] dims;
    
    String variable;
    
    public static String[] findDim(SIRFilter filter, String var)
    {
	ArrayDim ad = new ArrayDim(var);
	IterFactory.createIter(filter).accept(ad);
	
	
	if (ad.dims == null)
	    return null;
	
	String[] ret = new String[ad.dims.length];
	

	for (int i = 0; i < ad.dims.length; i++) {
	    FlatIRToC ftoc = new FlatIRToC();
	    ad.dims[i].accept(ftoc);
	    ret[i] = ftoc.getString();
	}
	
	return ret;
    }
    
    private ArrayDim(String var) 
    {
	variable = var;
    }
    
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {
	
	String ident = "";
	
	if (left instanceof JFieldAccessExpression) 
	    ident = ((JFieldAccessExpression)left).getIdent();
	else if (left instanceof JLocalVariableExpression) 
	    ident = ((JLocalVariableExpression)left).getVariable().getIdent();
	else 
	    return;
	
	//check the name
	if (!variable.equals(ident))
	    return;
	
	if (right instanceof JNewArrayExpression) {
	    dims = ((JNewArrayExpression)right).getDims();
	    return;
	}
    }
    
	
    /**
     * This is called before all visits to a stream structure (Filter,
     * Pipeline, SplitJoin, FeedbackLoop)
     */
    public void visitStream(SIRStream self,
			    SIRIterator iter) {
    }

    /**
     * PLAIN-VISITS 
     */
	    
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {

	//Visit fields declared in the filter class
	JFieldDeclaration[] fields = self.getFields();
	for (int i = 0; i < fields.length; i++)
	   fields[i].accept(this);

	//visit methods
	JMethodDeclaration[] methods = self.getMethods();
	for (int i =0; i < methods.length; i++)
	    methods[i].accept(this);
    }
  
    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRPipelineIter iter) {
	visitStream(self, iter);
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter) {
	visitStream(self, iter);
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {
	visitStream(self, iter);
    }

    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRPipelineIter iter) {
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRSplitJoinIter iter) {
    }

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRFeedbackLoopIter iter) {
    }
}
