package at.dms.kjc.sir.linear.frequency;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;


/**
 * A StupidFrequencyReplacer replaces FIR filters (those calculating convolution sums,
 * peek=N, pop=1 push=1) of sufficient length with a conversion into the frequency
 * domain, multiplication, and convert the product back into the time domain.
 *
 * (NOTE: This version of the frequency replacer uses wasteful implementation of
 * frequency conversion (eg throws out partial results between work function exections).
 *
 * In so doing, this also increases the peek, pop and push rates to take advantage of
 * the frequency transformation
 * 
 * $Id: StupidFrequencyReplacer.java,v 1.5 2003-03-30 21:51:44 thies Exp $
 **/
public class StupidFrequencyReplacer extends FrequencyReplacer {
    /** the name of the function in the C library that does fast convolution via the frequency domain. **/
    public static final String FAST_CONV_EXTERNAL = "do_fast_convolution_std";
    /** the name of the buffer in which to place the input data. */
    public static final String INPUT_BUFFER_NAME = "timeBuffer";
    
    /** the linear analyzier which keeps mappings from filters-->linear representations**/
    LinearAnalyzer linearityInformation;
    /** the target number of outputs to produce each firing of FIR filters. */
    int targetNumberOfOutputs;
    
    StupidFrequencyReplacer(LinearAnalyzer lfa, int targetSize) {
	if (lfa == null){
	    throw new IllegalArgumentException("Null linear filter analyzer!");
	}
	if (targetSize <= 0) {
	    throw new IllegalArgumentException("Target filter size must be greater than 0");
	}
	this.linearityInformation = lfa;
	this.targetNumberOfOutputs = targetSize;
    }

    public void preVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}
    public void preVisitPipeline(SIRPipeline self, SIRPipelineIter iter){}
    public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter){}
    public void visitFilter(SIRFilter self, SIRFilterIter iter){makeReplacement(self);}

    /**
     * Does the actual work of replacing something that computes a convolution
     * sum with something that does a FFT, multiply, and then IFFT.
     */
    public boolean makeReplacement(SIRStream self) {
	/* if we don't have a linear form for this stream, we are done. */
	if(!this.linearityInformation.hasLinearRepresentation(self)) {
	    return false;
	}

	LinearFilterRepresentation linearRep = this.linearityInformation.getLinearRepresentation(self);
	/* if there is not an FIR filter, we are done. */
	if (!linearRep.isFIR()) {
	    return false;
	}	
	
	/* now is when we get to the fun part, we have a linear representation
	 * that computes an FIR (ef pop 1, push 1, peek N) and we want to replace it with an FFT. */
	int x = linearRep.getPeekCount();
	int N = calculateN(this.targetNumberOfOutputs,x);
	int filterSize = N+2*(x-1);
	LinearPrinter.println(" creating frequency filter. (N=" + N +
			      ",x=" + x + ",size=" + filterSize + ")");
	
	/* make fields to hold the real and imaginary parts of the weights. */
	String realWeightName = "freq_weights_r";
	String imagWeightName = "freq_weights_i";
	JVariableDefinition realWeightField    = makeWeightField(realWeightName);
	JVariableDefinition imagWeightField    = makeWeightField(imagWeightName);
	
	/* wrap them up, along with any fields that the filter already has. */
	JFieldDeclaration[] newFields = makeFieldDeclarations(self.getFields(), realWeightField, imagWeightField);
	
	/* make a new init function */
	JMethodDeclaration freqInit = makeNewInit(linearRep, realWeightField, imagWeightField, filterSize);

	/* make a new work function */
	JMethodDeclaration freqWork = makeNewWork(realWeightField, imagWeightField, filterSize, x, N);
	
	
	LinearPrinter.println(" done building new IR nodes for " + self);

	// casting goodness to compile. not an issue as this code is not live anymore
	SIRFilter castSelf = (SIRFilter)self; 
	
	/* replace all of the pieces that we just built. */
	castSelf.setPeek(x+N-1);
	castSelf.setPop(N);
	castSelf.setPush(N);
	castSelf.setWork(freqWork);
	castSelf.setInit(freqInit);
	castSelf.setFields(newFields);
	castSelf.setIdent("Frequency" + self.getIdent());
	
	LinearPrinter.println(" done replacing.");
	
	// return true since we replaced something
	return true;
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

	/* calculate the weights of the fields based on the filter
	 * coefficients that are found in the linear representation. */	
	float[] frequency_response_r = new float[filterSize];
	float[] frequency_response_i = new float[filterSize];
	float[] time_response_r      = getRealArray(linearRep, filterSize);
	float[] time_response_i      = getImagArray(linearRep, filterSize);
	LinearFFT.fft_float(filterSize, false, /* false means that we are not doing a reverse transform */
			    time_response_r, /* real input */
			    time_response_i, /* imag input */
			    frequency_response_r,     /* filter's frequency response, real (output) */
			    frequency_response_i);    /* filter's frequency response, imag (output) */
				    
	/* assign the weights that we just calculated (in the compiler) to the weights array in the filter. */
	for (int i=0; i<filterSize; i++) {
	    /* create array assignments for both the real and imaginary coefficient arrays. */
	    body.addStatement(makeArrayAssignment(realWeightField, /* field */
						  i,                          /* index */
						  frequency_response_r[i]));  /* value */
	    body.addStatement(makeArrayAssignment(imagWeightField, /* field */
						  i,                          /* index */
						  frequency_response_i[i]));  /* value */
	}

	
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

    /** Creates an assignment expression of the form: this.f[index]=value; **/
    public JStatement makeArrayAssignment(JLocalVariable field, int index, float value) {
	/* make the field access expression (eg this.field)*/
	JFieldAccessExpression fldAccessExpr;
	fldAccessExpr = new JFieldAccessExpression(null, /* token reference */
						   new JThisExpression(null), /* base */
						   field.getIdent()); /* field name */
	/* make the array access expression (eg this.field[index]*/
	JArrayAccessExpression arrAccessExpr;
	arrAccessExpr = new JArrayAccessExpression(null,                    /* token reference */
						   fldAccessExpr,           /* prefix */
						   new JIntLiteral(index)); /* accessor */
	/* the literal value to assign */
	JFloatLiteral literalValue = new JFloatLiteral(null, value);
	/* now, make the assignment expression */
	JAssignmentExpression assignExpr;
	assignExpr = new JAssignmentExpression(null,          /* token reference */
					       arrAccessExpr, /* lhs */
					       literalValue); /* rhs */
	/* return an expression statement */
	return new JExpressionStatement(null, assignExpr, new JavaStyleComment[0]);
    }


    /* make the new work function */
    public JMethodDeclaration makeNewWork(JVariableDefinition realWeightField,
					  JVariableDefinition imagWeightField,
					  int filterSize, int x, int N) {
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
	for (int i=0; i<(N+x-1); i++) {
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
								       FAST_CONV_EXTERNAL, /* ident */
								       externalArgs);      /* args */
	JavaStyleComment[] comment = makeComment("callout to " + FAST_CONV_EXTERNAL + " to do actual work. "); 
	body.addStatement(new JExpressionStatement(null,         /* token reference */
						   externalCall, /* expression */
						   comment));       /* comments */
	

	/* now, put in code that will push the appropriate values inputBuffer[x-1 to N+x-2] back on the tape */
	for (int i=(x-1); i<(N+x-1); i++) {
	    body.addStatement(makeArrayPushStatement(inputBuffer, i));
	}

	/* now, free the memory that we allocated to buffer (this is a wicked hack,
	   as there is no notion of "free" in java/streamit */
	JExpression[] freeArgs = new JExpression[1];
	freeArgs[0] = new JLocalVariableExpression(null, inputBuffer);
	JMethodCallExpression freeCall = new JMethodCallExpression(null,     /* token reference */
								   "free",   /* ident */
								   freeArgs);/* args */
	comment = makeComment("call to free the buffer space");
	body.addStatement(new JExpressionStatement(null, freeCall, comment));
	
	/* stick in the appropriate number (N) of pop calls */
	for (int i=0; i<N; i++) {
	    body.addStatement(new JExpressionStatement(null, new SIRPopExpression(CStdType.Float), null));
	}
	
	/* wrap up all the mess that we just made into a new JMethodDeclaration and return it to the caller */
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


    /* makes an array push statement of the following form: push(arr[index]) */
    public JStatement makeArrayPushStatement(JLocalVariable arr, int index) {
	/* first make the array access expression arr[index]. **/
	JLocalVariableExpression arrExpr = new JLocalVariableExpression(null, arr);
	JArrayAccessExpression arrAccessExpr = new JArrayAccessExpression(null, arrExpr, new JIntLiteral(index));
	/* now make the push expression */
	SIRPushExpression pushExpr = new SIRPushExpression(arrAccessExpr, CStdType.Float);
	/* and return an expression statement */
	return new JExpressionStatement(null, pushExpr, null);
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


    /** returns an array of floating point numbers that correspond to the real part of the FIR filter's weights  **/
    public float[] getRealArray(LinearFilterRepresentation filterRep, int size) {return getArray(filterRep, size, 0);} 
    /** returns an array of floating point numbers that correspond to the imaginary part of the FIR filter's weights **/
    public float[] getImagArray(LinearFilterRepresentation filterRep, int size) {return getArray(filterRep, size, 1);}
    /**
     * the method that does the actual work for getRealArray and getImagArray.
     * part == 0, means get real part, part == 1 means get imaginary part.
     * if size is greater than the size of the filter rep, the rest of the array is left as 0
     **/
    public float[] getArray(LinearFilterRepresentation filterRep, int size, int part) {
	if (!filterRep.isFIR()) {
	    throw new RuntimeException("non fir filter passed to getArray()");
	}
	/* use the matrix that is hidden inside the filter representation */
	FilterMatrix source = filterRep.getA();
	if (source.getRows() > size) {
	    throw new RuntimeException("freq response size is too small for this filter's response.");
	}
	/* allocate the return array */
	float[] arr = new float[size];
	/* pull out the parts of the matrix, one by one */
	for (int i=0; i<source.getRows(); i++) {
	    ComplexNumber currentElement = source.getElement(i,0);
	    /* assign different piece depending on part parameter */
	    arr[i] = (part == 0) ? (float)currentElement.getReal() : (float)currentElement.getImaginary();
	}
	return arr;
    }


    /**
     * calculates the appropriate size FFT to perform. It is passed a target N, the number
     * of outputs to produce, and x, the length
     * of the impuse response of the filter, and it returns the actual N, the
     * number of output points that will be produced by one execution of the
     * filter.
     **/
    public int calculateN(int targetN, int x) {
	// we know N + 2(x-1) = 2^r
	// so we calculate r as floor(lg(N+2(x-1))) +1 where lg is log base 2
	
	// and then N = 2^r - 2(x-1)
	int arg = targetN + 2*(x-1);
	int r = (int)Math.floor(Math.log(arg)/Math.log(2)) + 1;
	// now, calculate N
	int N = (int)Math.pow(2,r) - 2*(x-1);
	return N;
    }







}




