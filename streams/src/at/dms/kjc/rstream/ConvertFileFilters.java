package at.dms.kjc.rstream;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * This class visits the stream graph and converts all 
 * SIRFileReaders and SIRFileWriters to normal (non-predefined)
 * filters with explicit calls to fopen in the init and fscanf or
 * fprintf in the work function.  This is done because the partitioner
 * does not currently support fusion of predefinied filters.
 *
 * @author Michael Gordon
 * 
 */
public class ConvertFileFilters extends EmptyAttributeStreamVisitor 
{
    /**
     * The main entry-point to this class, visit all filters in 
     * str and convert as above.
     * @param str the toplevel of the stream graph
     */
    public static void doit(SIRStream str) 
    {
	str.accept(new ConvertFileFilters());
    
    } 
    
    /**
     * Visit each stream in a pipeline and reset the pipeline
     * to be the children returned by the attribute visitor.
     *
     * @param self the pipeline
     * @param fields the fields of the pipeline
     * @param methods the methods of the pipeline     
     * @param init the init function of the pipeline
     *
     * @return The new pipeline.
     * 
     */
    public Object visitPipeline(SIRPipeline self,
				JFieldDeclaration[] fields,
				JMethodDeclaration[] methods,
				JMethodDeclaration init) {
	/** visit each child. **/
	LinkedList newChildren = new LinkedList();
	Iterator childIter = self.getChildren().iterator();
	while(childIter.hasNext()) {
	    SIROperator currentChild = (SIROperator)childIter.next();
	    newChildren.add(currentChild.accept(this));
	}
	self.setChildren(newChildren);
	return self;
    }

    /**
     * Visit a splitjoin and reset the parallel children to be
     * the children returned by the attribute visitor.
     */
    public Object visitSplitJoin(SIRSplitJoin self,
			  JFieldDeclaration[] fields,
			  JMethodDeclaration[] methods,
			  JMethodDeclaration init,
			  SIRSplitter splitter,
			  SIRJoiner joiner) {
	LinkedList newChildren = new LinkedList();
	// visit splitter
	self.getSplitter().accept(this);
	// visit children
	Iterator childIter = self.getChildren().iterator();
	while(childIter.hasNext()) {
	    SIROperator currentChild = (SIROperator)childIter.next();
	    newChildren.add(currentChild.accept(this));
	}
	// visit joiner 
	self.getJoiner().accept(this);
	self.setParallelStreams(newChildren);

	return self;
    }

    /**
     * Visit a feedbackloop and reset the loop and body to be the
     * loop and body as returned by the attribute stream visitor
     */
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
			     JFieldDeclaration[] fields,
			     JMethodDeclaration[] methods,
			     JMethodDeclaration init,
			     JMethodDeclaration initPath) {
	// visit the body
	self.setBody((SIRStream)self.getBody().accept(this));
	// visit the loop
	self.setLoop((SIRStream)self.getLoop().accept(this));
	return self;
    }

    /**
     * Visit a filter, if the filter is an SIRFile Reader or Writer
     * replace it with a FileReader or FileWriter, respectively.
     */
    public Object visitFilter(SIRFilter self,
			      JFieldDeclaration[] fields,
			      JMethodDeclaration[] methods,
			      JMethodDeclaration init,
			      JMethodDeclaration work,
			      CType inputType, CType outputType) {
	super.visitFilter(self, fields, methods, init, work, inputType, outputType);

	
	if (self instanceof SIRFileReader) {
	    System.out.println("Replacing file reader " + self);
	    return new FileReader((SIRFileReader)self);
	}
	else if (self instanceof SIRFileWriter) {
	    //do something here...
	    System.out.println("Replacing file writer " + self);	    
	    return null;
	}
	else {
	    return self;
	}
    }
    
}
