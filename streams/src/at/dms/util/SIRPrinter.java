//===========================================================================
//
//   FILE: SIRPrinter.java:
//   
//   Author: Michael Gordon
//   Date: Tue Oct  2 19:49:17 2001
//
//   Function:  print the sir
//
//===========================================================================


package at.dms.util;

import java.io.*;
import java.util.List;
import at.dms.kjc.SLIRVisitor;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.util.*;

public class SIRPrinter extends IRPrinter implements StreamVisitor {
    
    /* visit a structure */
    public void visitStructure(SIRStructure self,
                               SIRStream parent,
                               JFieldDeclaration[] fields) {
        blockStart("Structure");
        attrStart("Parent");
        if (parent == null)
            printData("null");
        attrEnd();
        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);
        blockEnd();
    }
    
    /* visit a filter */
    public void visitFilter(SIRFilter self,
		     SIRStream parent,
		     JFieldDeclaration[] fields,
		     JMethodDeclaration[] methods,
		     JMethodDeclaration init,
		     JMethodDeclaration work,
		     CType inputType, CType outputType){
	
	blockStart("Filter");
	attrStart("Parent");
	if (parent == null)
	    printData("null");
	attrEnd();
	for (int i = 0; i < fields.length; i++)
	    fields[i].accept(this);
	for (int i = 0; i < methods.length; i++)
	    methods[i].accept(this);
	attrPrint("peek", self.getPeek());
	attrPrint("pop", self.getPop());
	attrPrint("push", self.getPush());
	if (inputType != null)
	    attrPrint("InputType", inputType.toString());
	if (outputType != null)
	    attrPrint("OutputType", outputType.toString());
	blockEnd();
    }
	    
    String getSplitString(SIRSplitType s) {
	if (s == SIRSplitType.ROUND_ROBIN)
	    return "Round-Robin";
	if (s == SIRSplitType.DUPLICATE)
	    return "Duplicate";
	if (s == SIRSplitType.NULL)
	    return "Null";
	if (s == SIRSplitType.WEIGHTED_RR)
	    return "Weighted Round-Robin";
	else return "Unknown";
    }


    String getJoinString(SIRJoinType s) {
	if (s == SIRJoinType.ROUND_ROBIN)
	    return "Round-Robin";
	if (s == SIRJoinType.COMBINE)
	    return "Combine";
	if (s == SIRJoinType.NULL)
	    return "Null";
	if (s == SIRJoinType.WEIGHTED_RR)
	    return "Weighted Round-Robin";
	else return "Unknown";
    }


    /* visit a splitter */
    public void visitSplitter(SIRSplitter self,
		       SIRStream parent,
		       SIRSplitType type,
		       JExpression[] weights){
	
	blockStart("Splitter");
	attrPrint("Type", getSplitString(type));
	attrStart("Weights");
	if (weights != null)
	    for (int i = 0; i < weights.length; i++)
		weights[i].accept(this);
	attrEnd();
	blockEnd();
    }
    
    /* visit a joiner */
    public void visitJoiner(SIRJoiner self,
		     SIRStream parent,
		     SIRJoinType type,
		     JExpression[] weights){
	blockStart("Joiner");
	attrPrint("Type", getJoinString(type));
	attrStart("Weights");
	if (weights != null)
	    for (int i = 0; i < weights.length; i++)
		weights[i].accept(this);
	attrEnd();
	blockEnd();
    }

    
    protected void printData(int data) {
	try
	    {
		p.write(data);
	    }
        catch (IOException e)
	    {
		System.err.println(e);
		System.exit(1);
	    }
    }
 

    protected void attrPrint(String name, int i) {
	attrStart(name);
	printData(" ");
	printData(i);
	attrEnd();
    }

  
    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
			  SIRStream parent,
			  JFieldDeclaration[] fields,
			  JMethodDeclaration[] methods,
			  JMethodDeclaration init){
	blockStart("Pipeline");
	attrStart("Parent");
	if (parent == null)
	    printData("null");
	attrEnd();
	for (int i = 0; i < fields.length; i++)
	    fields[i].accept(this);
	for (int i = 0; i < methods.length; i++)
	    methods[i].accept(this);
	blockEnd();
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
			   SIRStream parent,
			   JFieldDeclaration[] fields,
			   JMethodDeclaration[] methods,
			   JMethodDeclaration init){
	blockStart("SplitJoin");
	attrStart("Parent");
	if (parent == null)
	    printData("null");
	attrEnd();

	for (int i = 0; i < fields.length; i++)
	    fields[i].accept(this);
	for (int i = 0; i < methods.length; i++)
	    methods[i].accept(this);
	blockEnd();
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
			      SIRStream parent,
			      JFieldDeclaration[] fields,
			      JMethodDeclaration[] methods,
			      JMethodDeclaration init,
			      int delay,
			      JMethodDeclaration initPath){
	blockStart("FeedbackLoop");
	attrStart("Parent");
	if (parent == null)
	    printData("null");
	attrEnd();
	attrPrint("delay", (new Integer(delay)).toString());
	attrStart("InitPath");
	initPath.accept(this);
	attrEnd();
	for (int i = 0; i < fields.length; i++)
	    fields[i].accept(this);
	for (int i = 0; i < methods.length; i++)
	    methods[i].accept(this);
	blockEnd();
    }

    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
			   SIRStream parent,
			   JFieldDeclaration[] fields,
			   JMethodDeclaration[] methods,
			   JMethodDeclaration init){}

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
			    SIRStream parent,
			    JFieldDeclaration[] fields,
			    JMethodDeclaration[] methods,
			    JMethodDeclaration init){}
    
    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
			       SIRStream parent,
			       JFieldDeclaration[] fields,
			       JMethodDeclaration[] methods,
			       JMethodDeclaration init,
			       int delay,
			       JMethodDeclaration initPath){}
}
