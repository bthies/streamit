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
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.util.*;

public class SIRPrinter extends IRPrinter implements StreamVisitor {
    
    /* visit a structure */

    /* I don't think a structure can show up in the stream graph
     * anymore, so commenting this out.  --bft 
     *
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
    */

    /**
     * Build an IRPrinter for a particular file.
     *
     * @arg filename  Name of the file to write IR to
     */
    public SIRPrinter(String filename) {
	super(filename);
    }
    
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {
	blockStart("Filter");
	attrStart("Parent");
	if (iter.getParent() == null)
	    printData("null");
	attrEnd();
	JFieldDeclaration[] fields = self.getFields();
	for (int i = 0; i < fields.length; i++)
	    fields[i].accept(this);
	JMethodDeclaration[] methods = self.getMethods();
	for (int i = 0; i < methods.length; i++)
	    methods[i].accept(this);
	attrPrint("peek", self.getPeek());
	attrPrint("pop", self.getPop());
	attrPrint("push", self.getPush());
	if (self.getInputType() != null)
	    attrPrint("InputType", self.getInputType().toString());
	if (self.getOutputType() != null)
	    attrPrint("OutputType", self.getOutputType().toString());
	blockEnd();
    }
	    
    /* visit a filter */
    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
	blockStart("PhasedFilter");
	attrStart("Parent");
	if (iter.getParent() == null)
	    printData("null");
	attrEnd();
	JFieldDeclaration[] fields = self.getFields();
	for (int i = 0; i < fields.length; i++)
	    fields[i].accept(this);
	JMethodDeclaration[] methods = self.getMethods();
	for (int i = 0; i < methods.length; i++)
	    methods[i].accept(this);
        SIRWorkFunction[] phases = self.getPhases();
        if (phases != null) {
            blockStart("phases");
            for (int i = 0; i < phases.length; i++)
            {
                attrPrint("peek", phases[i].getPeek());
                attrPrint("pop", phases[i].getPop());
                attrPrint("push", phases[i].getPush());
                phases[i].getWork().accept(this);
            }
            blockEnd();
        }
	if (self.getInputType() != null)
	    attrPrint("InputType", self.getInputType().toString());
	if (self.getOutputType() != null)
	    attrPrint("OutputType", self.getOutputType().toString());
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
    public void visitSplitter(SIRSplitter self) {
	blockStart("Splitter");
	attrPrint("Type", getSplitString(self.getType()));
	attrStart("Weights");
	JExpression[] weights = self.getInternalWeights();
	if (weights != null)
	    for (int i = 0; i < weights.length; i++)
		weights[i].accept(this);
	attrEnd();
	blockEnd();
    }
    
    /* visit a joiner */
    public void visitJoiner(SIRJoiner self) {
	blockStart("Joiner");
	attrPrint("Type", getJoinString(self.getType()));
	attrStart("Weights");
	JExpression[] weights = self.getInternalWeights();
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
				 SIRPipelineIter iter) {

	blockStart("Pipeline");
	attrStart("Parent");
	if (iter.getParent() == null)
	    printData("null");
	attrEnd();
	JFieldDeclaration[] fields = self.getFields();
	for (int i = 0; i < fields.length; i++)
	    fields[i].accept(this);
	JMethodDeclaration[] methods = self.getMethods();
	for (int i = 0; i < methods.length; i++)
	    methods[i].accept(this);
	blockEnd();
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter) {
	blockStart("SplitJoin");
	attrStart("Parent");
	if (iter.getParent() == null)
	    printData("null");
	attrEnd();

	JFieldDeclaration[] fields = self.getFields();
	for (int i = 0; i < fields.length; i++)
	    fields[i].accept(this);
	JMethodDeclaration[] methods = self.getMethods();
	for (int i = 0; i < methods.length; i++)
	    methods[i].accept(this);
	blockEnd();
	visitSplitter(self.getSplitter());
	visitJoiner(self.getJoiner());
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {
	blockStart("FeedbackLoop");
	attrStart("Parent");
	if (iter.getParent() == null)
	    printData("null");
	attrEnd();
	attrStart("delay");
	self.getDelay().accept(this);
	attrEnd();
	attrStart("InitPath");
	self.getInitPath().accept(this);
	attrEnd();
	JFieldDeclaration[] fields = self.getFields();
	for (int i = 0; i < fields.length; i++)
	    fields[i].accept(this);
	JMethodDeclaration[] methods = self.getMethods();
	for (int i = 0; i < methods.length; i++)
	    methods[i].accept(this);
	blockEnd();
	visitJoiner(self.getJoiner());
	visitSplitter(self.getSplitter());
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
