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
 * $Id: FFTWFrequencyReplacer.java,v 1.5 2002-11-08 01:22:05 aalamb Exp $
 **/
public class FrequencyReplacer extends EmptyStreamVisitor implements Constants{
    /** the name of the function in the C library that does fast convolution via the frequency domain. **/
    public static final String FAST_CONV_EXTERNAL = "do_fast_convolution";
    /** the name of the buffer in which to place the input data. */
    public static final String INPUT_BUFFER_NAME = "timeBuffer";

    /** If the makeNewWork function is making the init work function **/
    public static final int INITWORK = 1;
    public static final int WORK = 2;
    
    /** the linear analyzier which keeps mappings from filters-->linear representations**/
    LinearAnalyzer linearityInformation;
    /** the target number of outputs to produce each firing of FIR filters. */
    int targetNumberOfOutputs;
    
    private FrequencyReplacer(LinearAnalyzer lfa, int targetSize) {
	if (lfa == null){
	    throw new IllegalArgumentException("Null linear filter analyzer!");
	}
	if (targetSize <= 0) {
	    throw new IllegalArgumentException("Target filter size must be greater than 0");
	}
	this.linearityInformation = lfa;
	this.targetNumberOfOutputs = targetSize;
    }

    /**
     * start the process of replacement on str using the Linearity information in lfa.
     * targetSize is the targeted number of outputs to produce per steady state iteration
     * for each filter that is transformed using the frequency conversion. The actual number of
     * outputs produced will always be targetSize or greater (because the FFT we are doing only
     * operates on inputs that are powers of two long.
     **/
    public static void doReplace(LinearAnalyzer lfa, SIRStream str, int targetSize) {
	LinearPrinter.println("Beginning frequency replacement...");
	// make a new replacer with the information contained in the analyzer
	FrequencyReplacer replacer = new FrequencyReplacer(lfa, targetSize);
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
	 * that computes an FIR (ef pop 1, push 1, peek N) and we want to replace it with an FFT.
	 * Note that we can equate N with the block size of the input that we are looking at.
	 */
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

	/* make fields to hold the real and imaginary partial results between executions */
	String partialFieldName = "freq_partial";
	JVariableDefinition partialField   = makeWeightField(partialFieldName);
	
	/* wrap them up, along with any fields that the filter already has. */
	JFieldDeclaration[] newFields = makeFieldDeclarations(self.getFields(),realWeightField, imagWeightField, partialField);
	
	/* make a new init function */
	JMethodDeclaration freqInit = makeNewInit(linearRep, realWeightField, imagWeightField, partialField,
						  filterSize, x);

	/* make the init work function */
	JMethodDeclaration freqInitWork = makeNewWork(INITWORK, realWeightField, imagWeightField, partialField,
						      filterSize, x, N);
	
	/* make a new work function */
	JMethodDeclaration freqWork = makeNewWork(WORK, realWeightField, imagWeightField, partialField,
						  filterSize, x, N);
	
	
	LinearPrinter.println(" done building new IR nodes for " + self);
	
	LinearPrinter.println(" creating new two stage filter...");
	// Create a new filter that contains all of the new pieces that we have built
	SIRTwoStageFilter freqFilter;
	/* Note, we need to have initPeek-initPop == peek-Pop for some scheduling reason
	 * so therefore, we set the peek rate of the work function to be N+2(x-1) even though
	 * it really only needs to be N+x-1.*/
	freqFilter = new SIRTwoStageFilter(self.getParent(),              /* parent */
					   "TwoStageFreq" + self.getIdent(),/* ident */
					   newFields,                     /* fields */
					   new JMethodDeclaration[0],     /* methods -- init, work, and initWork are special*/
					   new JIntLiteral(N+x-1 + x-1),  /* peek (w/ extra x-1 window...)*/
					   new JIntLiteral(N+x-1),        /* pop */
					   new JIntLiteral(N+x-1),        /* push */
					   freqWork,                      /* work */
					   N+x-1,                         /* initPeek */
					   N,                             /* initPop */
					   N,                             /* initPush */
					   freqInitWork,                  /* initWork */
					   self.getInputType(),           /* input type */
					   self.getOutputType());         /* output type */
	// need to explicitly set the init function
	freqFilter.setInit(freqInit);

	// now replace the current filter (self) with the frequency version
	self.getParent().replace(self, freqFilter);
	
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
    


    /* make a new field declarations array with the new fields for weights and partial results. */
    public JFieldDeclaration[] makeFieldDeclarations(JFieldDeclaration[] originals,
						     JVariableDefinition new1,
						     JVariableDefinition new2,
						     JVariableDefinition new3) {
	/* really simple -- make a new array, copy the elements from the
	 * old array and then stick in the two new ones. */
	JFieldDeclaration[] returnFields = new JFieldDeclaration[originals.length+3];
	for (int i=0; i<originals.length; i++) {
	    returnFields[i] = originals[i];
	}
	/* now, stick in the final two. */
	returnFields[originals.length]   = new JFieldDeclaration(null, new1, null, null);
	returnFields[originals.length+1] = new JFieldDeclaration(null, new2, null, null);
	returnFields[originals.length+2] = new JFieldDeclaration(null, new3, null, null);

	return returnFields;
    }



    /**
     * Make an init function which assigns allocates space for the various fields
     * (frequency weight fields are of size filterSize, and partial result fields are
     * of size x-1) and 
     * calculates the DFT of the impulse response and stores it in the weight fields.
     **/
    public JMethodDeclaration makeNewInit(LinearFilterRepresentation linearRep,
					  JVariableDefinition realWeightField,
					  JVariableDefinition imagWeightField,
					  JVariableDefinition partialField,
					  int filterSize, int x) {
	
	JBlock body = new JBlock();

	/* add in statements to allocate memory for the fields.*/
	body.addStatement(makeFieldAllocation(realWeightField.getIdent(),  filterSize));
	body.addStatement(makeFieldAllocation(imagWeightField.getIdent(),  filterSize));
	body.addStatement(makeFieldAllocation(partialField.getIdent(), x-1));


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

    /**
     * Create an array allocation expression. Allocates a one dimensional array of floats
     * for the field of name fieldName of fieldSize.
     **/
    public JStatement makeFieldAllocation(String fieldName, int fieldSize) {
	JExpression fieldExpr = new JFieldAccessExpression(null, new JThisExpression(null), fieldName);
	JExpression fieldAssign = new JAssignmentExpression(null, fieldExpr, getNewArrayExpression(fieldSize));
	return new JExpressionStatement(null, fieldAssign, null);
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


    /*
     * make both the work and the new work function.
     *
     * We make a function that copies N+x-1 elements from the input
     * tape into a local array, calls the library function with the local array
     * and the weight field to calcluate the produce of the DFT of the input and the weight fields.
     * If we are making the initWork function, then just the middle N elements of the result are pushed
     * and if we are making the work function then the x-1 partials are added to the first x-1 elements
     * of the results and we push out the first N+x-1 elements. For both types of work we
     * then save the last x-1 elements of the DFT output in
     * the partial results fields for the next execution. Note that filterSize = N + 2(x-1).
     */
    public JMethodDeclaration makeNewWork(int functionType, /* either INITWORK or WORK */
					  JVariableDefinition realWeightField,
					  JVariableDefinition imagWeightField,
					  JVariableDefinition partialField,
					  int filterSize, int x, int N) {
	// parameter check
	if ((functionType != INITWORK) && (functionType != WORK)) {
	    throw new IllegalArgumentException("function type must be either WORK or INITWORK");
	}

	/* the body of the new work function */
	JBlock body = new JBlock();

	/* make a local variable for a buffer to hold values from the input tape. */
	JVariableDefinition inputBuffer;
	inputBuffer = new JVariableDefinition(null,         /* token reference */
					      0,            /* modifiers */
					      getArrayType(), /* float* */
					      INPUT_BUFFER_NAME, /* ident */
					      null);        /* initializer*/

	/* stick in a declaration statement for the buffer into the body of the function. */
	body.addStatement(new JVariableDeclarationStatement(null, inputBuffer, null));

	/* allocate space for the input buffer */
	JLocalVariableExpression buffExpr = new JLocalVariableExpression(null, inputBuffer);
	JNewArrayExpression      newExpr = getNewArrayExpression(filterSize);
	JAssignmentExpression    assignExpr = new JAssignmentExpression(null, buffExpr, newExpr);
	body.addStatement(new JExpressionStatement(null,assignExpr,makeComment("allocate space for input buffer.")));

	/* now, copy the data from the input tape into the buffer, element by element */
	for (int i=0; i<(N+x-1); i++) {
	    JLocalVariableExpression currentBuffExpr = new JLocalVariableExpression(null, inputBuffer);
	    JArrayAccessExpression currentAccessExpr = new JArrayAccessExpression(null, currentBuffExpr, new JIntLiteral(i));
	    SIRPeekExpression      currentPeekExpr   = new SIRPeekExpression(new JIntLiteral(i), CStdType.Float);
	    JAssignmentExpression currentAssignExpr  = new JAssignmentExpression(null, currentAccessExpr, currentPeekExpr);
	    /* note that currentAssignExpr is buff[i] = peek(i) */
	    body.addStatement(new JExpressionStatement(null, currentAssignExpr, null));
	}
	
	/* stick in a call to the do_fast_convolution routine that gets linked in via the C library. */
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
	JavaStyleComment[] comment = makeComment("callout to " + FAST_CONV_EXTERNAL + " to do actual DFT, mult, IDFT. "); 
	body.addStatement(new JExpressionStatement(null,externalCall,comment));            /* comments */

	/* if we are in the normal work function, push out the first x-1
	   values from the local buffer added to the partial results */
	if (functionType == WORK) {
	    for (int i=0; i<(x-1); i++) {
		body.addStatement(makeArrayAddAndPushStatement(partialField.getIdent(), inputBuffer, i));
	    }
	}

	
	/* now, put in code that will push the appropriate values inputBuffer[x-1 to N+x-2] back on the tape */
	for (int i=(x-1); i<(N+x-1); i++) {
	    body.addStatement(makeArrayPushStatement(inputBuffer, i));
	}

	/* now, copy the last x-1 values in the input buffer into the partial results buffer. */
	for (int i=0; i<(x-1); i++) {
	    body.addStatement(makePartialCopyExpression(partialField.getIdent(), i, inputBuffer, N+x-1+i));
	}
	
	    
	/* now, free the memory that we allocated to buffer (this is a wicked hack,
	   as there is no notion of "free" in java/streamit */
	JExpression[] freeArgs = new JExpression[1];
	freeArgs[0] = new JLocalVariableExpression(null, inputBuffer);
	JMethodCallExpression freeCall = new JMethodCallExpression(null,     /* token reference */
								   null,     /* prefix */
								   "free",   /* ident */
								   freeArgs);/* args */
	comment = makeComment("call to free the buffer space");
	body.addStatement(new JExpressionStatement(null, freeCall, comment));
	
	/* stick in the appropriate number (N) of pop calls */
	for (int i=0; i<N; i++) {
	    body.addStatement(makePopStatement());
	}

	/* if this is the work function, we should also pup x-1 more items. to get a total pop count of N+x-1 */
	if (functionType == WORK) {
	    for (int i=0; i<(x-1); i++) {
		body.addStatement(makePopStatement());
	    }
	}

	/* figure out what the name of the function should be (work, or initWork) **/
	String ident = (functionType == WORK) ? "work" : "initWork";
	
	/* wrap up all the mess that we just made into a new JMethodDeclaration and return it to the caller */
	return new JMethodDeclaration(null,                  /* token reference */
				      ACC_PUBLIC,            /* modifiers */
				      CStdType.Void,         /* return type */
				      ident,                 /* identifier */
				      JFormalParameter.EMPTY,/* paramters */
				      CClassType.EMPTY,      /* exceptions */
				      body,                  /* body */
				      null,                  /* java doc */
				      null);                 /* java style comment */
    }


    /** Makes a copy expression from the local variable to the field of the form this.field[index1] = arr[index2] **/
    public JStatement makePartialCopyExpression(String field, int index1, JLocalVariable arr, int index2) {
	/* first, make the this.field[index] expression */
	JFieldAccessExpression fieldAccessExpr = new JFieldAccessExpression(null, new JThisExpression(null), field);
	JArrayAccessExpression fieldArrayAccessExpr;
	fieldArrayAccessExpr = new JArrayAccessExpression(null, fieldAccessExpr, new JIntLiteral(index1));
	/* now make the array access expression arr[index]. **/
	JLocalVariableExpression arrExpr = new JLocalVariableExpression(null, arr);
	JArrayAccessExpression arrAccessExpr = new JArrayAccessExpression(null, arrExpr, new JIntLiteral(index2));
	/* now make the assignment expression */
	JAssignmentExpression assignExpr = new JAssignmentExpression(null, fieldArrayAccessExpr, arrAccessExpr);
	/* now write it all in an expression statement */
	return new JExpressionStatement(null, assignExpr, null);
	
    }

    /** makes a popFloat() statement. **/
    public JStatement makePopStatement() {
	return new JExpressionStatement(null, new SIRPopExpression(CStdType.Float), null);
    }

    /** makes an array push statement of the following form: push(this.field[index] + arr2[index]) **/
    public JStatement makeArrayAddAndPushStatement(String field, JLocalVariable arr, int index) {
	/* first, make the this.field[index] expression */
	JFieldAccessExpression fieldAccessExpr = new JFieldAccessExpression(null, new JThisExpression(null), field);
	JArrayAccessExpression fieldArrayAccessExpr;
	fieldArrayAccessExpr = new JArrayAccessExpression(null, fieldAccessExpr, new JIntLiteral(index));
	/* now make the array access expression arr[index]. **/
	JLocalVariableExpression arrExpr = new JLocalVariableExpression(null, arr);
	JArrayAccessExpression arrAccessExpr = new JArrayAccessExpression(null, arrExpr, new JIntLiteral(index));
	/* make the add expression */
	JAddExpression addExpr = new JAddExpression(null, fieldArrayAccessExpr, arrAccessExpr);
	/* now make the push expression */
	SIRPushExpression pushExpr = new SIRPushExpression(addExpr, CStdType.Float);
	/* and return an expression statement */
	return new JExpressionStatement(null, pushExpr, null);
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




