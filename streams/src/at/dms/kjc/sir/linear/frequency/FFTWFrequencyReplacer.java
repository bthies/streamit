package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;


/**
 * A FrequencyReplacer replaces FIR filters (those calculating convolution sums,
 * peek=N, pop=1 push=1) of sufficient length with a conversion into the frequency
 * domain, multiplication, and convert the product back into the time domain.
 *
 * In so doing, this also increases the peek, pop and push rates to take advantage of
 * the frequency transformation
 * 
 * $Id: FFTWFrequencyReplacer.java,v 1.2 2002-10-28 22:32:31 aalamb Exp $
 **/
public class FrequencyReplacer extends EmptyStreamVisitor implements Constants{
    /** the name of the function in the C library that does fast convolution via the frequency domain. **/
    public static final String FAST_CONV_EXTERNAL = "do_fast_convolution";
    /** the name of the buffer in which to place the input data. */
    public static final String INPUT_BUFFER_NAME = "freqBuffer";
    
    /** the linear analyzier which keeps mappings from filters-->linear representations**/
    LinearAnalyzer linearityInformation;
    
    private FrequencyReplacer(LinearAnalyzer lfa) {
	if (lfa == null){
	    throw new IllegalArgumentException("Null linear filter analyzer!");
	}

	this.linearityInformation = lfa;
    }

    /** start the process of replacement on str using the Linearity information in lfa. **/
    public static void doReplace(LinearAnalyzer lfa, SIRStream str) {
	LinearPrinter.println("Beginning frequency replacement...");
	// make a new replacer with the information contained in the analyzer
	FrequencyReplacer replacer = new FrequencyReplacer(lfa);
	// pump the replacer through the stream graph.
	IterFactory.createIter(str).accept(replacer);
    }

    public void preVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}
    public void preVisitPipeline(SIRPipeline self, SIRPipelineIter iter){}
    public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter){}
    public void visitFilter(SIRFilter self, SIRFilterIter iter){makeReplacement(self);}

    /**
     * Does the actual work of replacing something that computes a convolution
     * sum with something that does a FFT, multiply, and then IFFT.
     */
    private void makeReplacement(SIRFilter self) {
	/* if we don't have a linear form for this stream, we are done. */
	if(!this.linearityInformation.hasLinearRepresentation(self)) {
	    return;
	}

	LinearFilterRepresentation linearRep = this.linearityInformation.getLinearRepresentation(self);
	/* if there is not an FIR filter, we are done. */
	if (!linearRep.isFIR()) {
	    return;
	}	
	
	/* now is when we get to the fun part, we have a linear representation
	 * that computes an FIR (ef pop 1, push 1, peek N) and we want to replace it with an FFT. */

	int filterSize = linearRep.getPeekCount();
	
	/* make fields to hold the real and imaginary parts of the weights. */
	String realWeightName = "freq_weights_r";
	String imagWeightName = "freq_weights_i";
	JVariableDefinition realWeightField    = makeWeightField(realWeightName);
	JVariableDefinition imagWeightField    = makeWeightField(imagWeightName);
	
	/* wrap them up, along with any fields that the filter already has. */
	JFieldDeclaration[] newFields = makeFieldDeclarations(self.getFields(), realWeightField, imagWeightField);
	
	/* make a new work function */
	JMethodDeclaration freqWork = makeNewWork(realWeightField, imagWeightField, filterSize);
	
	/* make a new init function */
	JMethodDeclaration freqInit = makeNewInit(linearRep, realWeightField, imagWeightField, filterSize);
	
	LinearPrinter.println(" done building new IR nodes for " + self);
	
	/* replace all of the pieces that we just built. */
	self.setWork(freqWork);
	self.setInit(freqInit);
	self.setFields(newFields);
	self.setIdent("Frequency" + self.getIdent());
	
	LinearPrinter.println(" done replacing.");
	
    }
    
    /**
     * create the field that we are going to put the filter's freqenucy response in.
     * the field is an array of floats, and we will have one array for the real part of the
     * repsonse and one array for the imaginary part of the response.
     */
    public JVariableDefinition makeWeightField(String name) {
	return new JVariableDefinition(null, /* token reference */
				       ACC_FINAL, /* modifiers */
				       getArrayType(), /* type */
				       name, /* identity */
				       null); /* initializer */
    }
    


    /* make a new field declarations array with the new fields for weights. */
    public JFieldDeclaration[] makeFieldDeclarations(JFieldDeclaration[] originals,
						     JVariableDefinition new1,
						     JVariableDefinition new2) {
	/* really simple -- make a new array, copy the elements from the
	 * old array and then stick in the two new ones. */
	JFieldDeclaration[] returnFields = new JFieldDeclaration[originals.length+2];
	for (int i=0; i<originals.length; i++) {
	    returnFields[i] = originals[i];
	}
	/* now, stick in the final two. */
	returnFields[originals.length]   = new JFieldDeclaration(null, new1, null, null);
	returnFields[originals.length+1] = new JFieldDeclaration(null, new2, null, null);

	return returnFields;
    }



    /* make the new init function which assigns the frequency weights to their respective fields. */
    public JMethodDeclaration makeNewInit(LinearFilterRepresentation linearRep,
					  JVariableDefinition realWeightField,
					  JVariableDefinition imagWeightField,
					  int filterSize) {
	
	JBlock body = new JBlock();

	/* add in statements to allocate memory for the fields.*/
	JExpression realField = new JFieldAccessExpression(null, new JThisExpression(null), realWeightField.getIdent());
	JExpression imagField = new JFieldAccessExpression(null, new JThisExpression(null), imagWeightField.getIdent());
	JExpression realAssign = new JAssignmentExpression(null, realField, getNewArrayExpression(filterSize)); 
	JExpression imagAssign = new JAssignmentExpression(null, imagField, getNewArrayExpression(filterSize));
	body.addStatement(new JExpressionStatement(null, realAssign, null));
	body.addStatement(new JExpressionStatement(null, imagAssign, null)); 

	/* calculate the weights of the fields based on the filter coefficients that are found in the linear representation. */

	/* assign the weights that we just calculated (in the compiler) to the weights array in the filter. */


	/** assemble the new pieces that we made and stick them into a new method. */
	return new JMethodDeclaration(null,                 /* token reference */
				      ACC_PUBLIC,           /* modifiers */
				      CStdType.Void,        /* return type */
				      "init",               /* identifier */
				      JFormalParameter.EMPTY,/* paramters */
				      CClassType.EMPTY,     /* exceptions */
				      body,                 /* body */
				      null,                 /* java doc */
				      null);                /* java style comment */
    }


    /* make the new work function */
    public JMethodDeclaration makeNewWork(JVariableDefinition realWeightField,
					  JVariableDefinition imagWeightField,
					  int filterSize) {
	/* the body of the new work function */
	JBlock body = new JBlock();

	/* make a local variable as a buffer to hold the information from the input tape. */
	JVariableDefinition inputBuffer;
	inputBuffer = new JVariableDefinition(null,         /* token reference */
					      0,            /* modifiers */
					      getArrayType(), /* float* */
					      INPUT_BUFFER_NAME, /* ident */
					      null);        /* initializer*/

	/* stick in a declaration statement for the buffer into the body of the function. */
	body.addStatement(new JVariableDeclarationStatement(null, /* token reference */
							    inputBuffer, /* var definition */
							    null)); /* comments */

	/* allocate space for the input buffer */
	JLocalVariableExpression buffExpr = new JLocalVariableExpression(null, inputBuffer);
	JNewArrayExpression      newExpr = getNewArrayExpression(filterSize);
	JAssignmentExpression    assignExpr = new JAssignmentExpression(null, buffExpr, newExpr);
	body.addStatement(new JExpressionStatement(null, assignExpr,
						   makeComment("allocate space for input buffer.")));

	/* now, copy the data from the input tape into the buffer, element by element */
	for (int i=0; i<filterSize; i++) {
	    JLocalVariableExpression currentBuffExpr = new JLocalVariableExpression(null, inputBuffer);
	    JArrayAccessExpression currentAccessExpr = new JArrayAccessExpression(null, currentBuffExpr, new JIntLiteral(i));
	    SIRPeekExpression      currentPeekExpr   = new SIRPeekExpression(new JIntLiteral(i), CStdType.Float);
	    JAssignmentExpression currentAssignExpr  = new JAssignmentExpression(null, currentAccessExpr, currentPeekExpr);
	    /* note that currentAssignExpr is buff[i] = peek(i) */
	    body.addStatement(new JExpressionStatement(null, currentAssignExpr, null));
	}
	
	/* stick in a call to the do_fast_convolution routine that gets linked in to the C library. */
	// prep the args
	JExpression[] externalArgs = new JExpression[4];
	externalArgs[0] = new JLocalVariableExpression(null, inputBuffer);
	externalArgs[1] = new JFieldAccessExpression(null, new JThisExpression(null), realWeightField.getIdent());
	externalArgs[2] = new JFieldAccessExpression(null, new JThisExpression(null), imagWeightField.getIdent());
	externalArgs[3] = new JIntLiteral(filterSize);
	JMethodCallExpression externalCall = new JMethodCallExpression(null,               /* token reference */
								       null,               /* prefix */
								       FAST_CONV_EXTERNAL, /* ident */
								       externalArgs);      /* args */
	JavaStyleComment[] comment = makeComment("callout to " + FAST_CONV_EXTERNAL + " to do actual work. "); 
	body.addStatement(new JExpressionStatement(null,         /* token reference */
						   externalCall, /* expression */
						   comment));       /* comments */
	
	return new JMethodDeclaration(null,                 /* token reference */
				      ACC_PUBLIC,           /* modifiers */
				      CStdType.Void,        /* return type */
				      "work",               /* identifier */
				      JFormalParameter.EMPTY,/* paramters */
				      CClassType.EMPTY,     /* exceptions */
				      body,                 /* body */
				      null,                 /* java doc */
				      null);                /* java style comment */
    }


	/** returns the type to use for buffers -- in this case float[] */
	public CType getArrayType() {
	    return new CArrayType(CStdType.Float, 1);
	}

	/** make a JNewArrayStatement that allocates size elements of a float array */
	public JNewArrayExpression getNewArrayExpression(int size) {
	    /* make the size array. */
	    JExpression[] arrayDims = new JExpression[1];
	    arrayDims[0] = new JIntLiteral(size);
	    return new JNewArrayExpression(null,           /* token reference */
					   getArrayType(), /* type */
					   arrayDims,      /* size */
					   null);          /* initializer */
	}


    /* make an array of one java comments from a string. */
    public JavaStyleComment[] makeComment(String c) {
	JavaStyleComment[] container = new JavaStyleComment[1];
	container[0] = new JavaStyleComment(c,
					    true,   /* isLineComment */
					    false,  /* space before */
					    false); /* space after */
	return container;
    }














}




