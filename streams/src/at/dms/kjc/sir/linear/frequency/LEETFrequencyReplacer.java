package at.dms.kjc.sir.linear.frequency;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;


/**
 * A LEETFrequencyReplacer replaces FIR filters (those calculating convolution sums,
 * peek=N, pop=1 push=1) of sufficient length with a conversion into the frequency
 * domain, multiplication, and convert the product back into the time domain.
 *
 * In so doing, this also increases the peek, pop and push rates to take advantage of
 * the frequency transformation.
 * 
 * $Id: LEETFrequencyReplacer.java,v 1.7 2003-02-10 18:31:36 aalamb Exp $
 **/
public class LEETFrequencyReplacer extends FrequencyReplacer{
    /** the name of the function in the C library that converts a buffer of real data from the time
     * domain to the frequency domain (replacing the value in the buffer with the half complex array
     * representation of the frequency response. **/
    public static final String TO_FREQ_EXTERNAL   = "convert_to_freq";
    /** the name of the function in the C library that converts a buffer of half complex array in the
     * frequency domain to a buffer of real data in the time domain. **/
    public static final String FROM_FREQ_EXTERNAL   = "convert_from_freq";
    /** the name of the function in the C library that scales an array by 1/size. **/
    public static final String SCALE_EXTERNAL = "scale_by_size";
    /** the name of the function in the C library that multiplies two complex arrays together. **/
    public static final String MULTIPLY_EXTERNAL  = "do_halfcomplex_multiply";

    /** the name of the field to treat as the input buffer. */
    public static final String INPUT_BUFFER_NAME = "inputBuffer";
    /** the field to use as the output buffer for the filter. **/
    public static final String OUTPUT_BUFFER_NAME = "outputBuffer";
    /** the field to use as the output buffer for the filter. **/
    public static final String TEMP_BUFFER_NAME = "tempBuffer";
    /** the prefix for the fields that we will store the transform of the filters in. **/
    public static final String FILTER_WEIGHTS_PREFIX = "freqWeightField";
    /** the prefix for the fields that will store the partial results between filter executions. **/
    public static final String PARTIAL_BUFFER_PREFIX ="freqPartial";
    

    /** Constants specifying if we are making the work or the init work function **/
    public static final int INITWORK = 1;
    public static final int WORK = 2;

    /** The minimum size FIR we will replace. 90 came from empirical measurements. **/
    //public static final int minFIRSize = 90;
    public static final int minFIRSize = 2;
    /** We multiply the FIR size to get the target FFT size if it is not specified. **/
    public static final int fftSizeFactor = 2;
    
    
    /** the linear analyzier which keeps mappings from filters-->linear representations**/
    LinearAnalyzer linearityInformation;
    
    LEETFrequencyReplacer(LinearAnalyzer lfa) {
	if (lfa == null){
	    throw new IllegalArgumentException("Null linear filter analyzer!");
	}
	this.linearityInformation = lfa;
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
	LinearPrinter.println(" processing " + self.getIdent());
	/* if we don't have a linear form for this stream, we are done. */
	if(!this.linearityInformation.hasLinearRepresentation(self)) {
	    LinearPrinter.println("  aborting -- not linear");
	    return;
	}

	LinearFilterRepresentation linearRep = this.linearityInformation.getLinearRepresentation(self);
	/* if this filter doesn't have a pop of one, we abort. **/
	if (linearRep.getPopCount() != 1) {
	    LinearPrinter.println("  aborting -- filter is not FIR (pop = " +
				  linearRep.getPopCount() + ")"); 
	    return;
	}	

	/* if there is not a real valued FIR (all coefficients are real), we are done. */
	if (!linearRep.isPurelyReal()) {
	    LinearPrinter.println("  aborting -- filter has non real coefficients."); 
	    return;
	}

	/** if this filter has a constant component (eg if it has a non zero "b") we abort.
	    (Note, there is nothing saying that we can't perform the transform in this
	    case, only that currently we don't have any examples of programs that use it
	    so I am not going to bother implenting it. -- AAL**/
	if (linearRep.hasConstantComponent()) {
	    LinearPrinter.println("  aborting -- filter has non zero constant components.\n" +
				  "ANDREW -- YOU ARE BEING LAZY. THIS IS NOT A HARD THING TO IMPLEMENT " +
				  "SO DO IT!");
	    return;
	}
	
	/** if doing clever replacement, don't do small FIRs. **/
	if (linearRep.getPeekCount() < minFIRSize) {
 	    LinearPrinter.println("  aborting -- fir size too small: " +
				  linearRep.getPeekCount() + ". needs to be at least " +
				  minFIRSize);
	    return;
	}

	/** set the target FFT size appropriately if it hasn't already been set */
	int targetNumberOfOutputs = fftSizeFactor * linearRep.getPeekCount();
	LinearPrinter.println("  target output size: " + targetNumberOfOutputs);
	
	/* now is when we get to the fun part, we have a linear representation
	 * that computes an FIR (ef pop 1, push 1, peek x) and we want to replace it with an FFT.
	 * Note that N is the block size of the input that we are looking at. */
	int x = linearRep.getPeekCount();
	int N = calculateN(targetNumberOfOutputs,x);
	int filterSize = N+2*(x-1); // this is the size of the overall filter
	LinearPrinter.println("  creating frequency filter.\n" +
			      "   N+(x-1)=" + (N+x-1) + " (outputs per steady state)\n" + 
			      "   x=" + x + " (peek, original filter size)\n" +
			      "   filterSize=" + filterSize + " (FFT size)");
	

	/////////////// Create the new fields for this filter.
	int numWeightFields = linearRep.getPushCount(); // Eg the number of columns in the matrix
	JFieldDeclaration[] newFields = self.getFields();
	
	/* make the fields to hold the transform of the weights. Note that the
	   weights are stored in "half complex array" format which is the output format
	   generated by FFTW. The first weight field corresponds to the weight field of
	   the right most column, the next weight field corresponds to the weight field
	   of the next right most column, etc. */
	JVariableDefinition[] weightFields = new JVariableDefinition[numWeightFields];
	for (int i=0; i<numWeightFields; i++) {
	    weightFields[i] = makeWeightField(FILTER_WEIGHTS_PREFIX + "_" + i);
	    newFields = appendFieldDeclaration(newFields, weightFields[i]);
	}
	
	/* make the fields to hold the partial results between executions. These
	   fields are also stored in half complex array format, and each corresponds
	   to the same column as does the weight fields. */
	JVariableDefinition[] partialFields = new JVariableDefinition[numWeightFields];
	for (int i=0; i<numWeightFields; i++) {
	    partialFields[i] = makeWeightField(PARTIAL_BUFFER_PREFIX + "_" + i);
	    newFields = appendFieldDeclaration(newFields, partialFields[i]);
	}

	/* make the input and temp buffer fields. */
	JVariableDefinition inputBufferField  = makeWeightField(INPUT_BUFFER_NAME);
	newFields = appendFieldDeclaration(newFields, inputBufferField);
	JVariableDefinition tempBufferField  = makeWeightField(TEMP_BUFFER_NAME);
	newFields = appendFieldDeclaration(newFields, tempBufferField);
 
	/** make enough output buffers to hold the appropriate outputs. **/
	JVariableDefinition outputBufferFields[] = new JVariableDefinition[numWeightFields];
	for (int i=0; i<numWeightFields; i++) {
	    outputBufferFields[i] = makeWeightField(OUTPUT_BUFFER_NAME + "_" + i);
	    newFields = appendFieldDeclaration(newFields, outputBufferFields[i]);
	}
	
	
	
	/* make a new init function */
	JMethodDeclaration freqInit = makeNewInit(linearRep,
						  weightFields, partialFields,
						  inputBufferField, tempBufferField,
						  outputBufferFields,
						  filterSize, x);
	
	/* make the init work function */
	JMethodDeclaration freqInitWork = makeNewWork(INITWORK,
						      weightFields, partialFields,
						      inputBufferField, tempBufferField,
						      outputBufferFields,
						      filterSize, x, N);
	
	/* make a new work function */
	JMethodDeclaration freqWork = makeNewWork(WORK,
						  weightFields, partialFields,
						  inputBufferField, tempBufferField,
						  outputBufferFields,
						  filterSize, x, N);
	
	
	LinearPrinter.println("  done building new IR nodes for " + self.getIdent());
	
	LinearPrinter.println("  creating new two stage filter...");
	// Create a new filter that contains all of the new pieces that we have built
	SIRTwoStageFilter freqFilter;
	/* Note, we need to have initPeek-initPop == peek-Pop for some scheduling reason
	 * so therefore, we set the peek rate of the work function to be N+2(x-1) even though
	 * it really only needs to be N+x-1.*/
	freqFilter = new SIRTwoStageFilter(self.getParent(),              /* parent */
					   "TwoStageFreq"+self.getIdent(),/* ident */
					   newFields,                     /* fields */
					   new JMethodDeclaration[0],     /* methods Note:
									     -- init, work, and
									     initWork are special. */
					   new JIntLiteral(N+x-1),        /* peek (w/ extra x-1 window...)*/
					   new JIntLiteral(N+x-1),        /* pop */
					   new JIntLiteral((N+x-1)*numWeightFields), /* push (note that the
											push count needs to
											be scaled by the
											number of outputs
											(because each FIR
											filter outputs N+x-1 */
					   freqWork,                      /* work */
					   N+x-1,                         /* initPeek */
					   N+x-1,                         /* initPop */
					   N+x-1,                         /* initPush */
					   freqInitWork,                  /* initWork */
					   self.getInputType(),           /* input type */
					   self.getOutputType());         /* output type */
	// need to explicitly set the init function
	freqFilter.setInit(freqInit);
	
	// now replace the current filter (self) with the frequency version
	self.getParent().replace(self, freqFilter);
	
	LinearPrinter.println("  done replacing.");
	
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
    


    /* returns an array that is a field declaration for (newField)
     * appended to the original field declarations. **/
    public JFieldDeclaration[] appendFieldDeclaration(JFieldDeclaration[] originals,
						      JVariableDefinition newField) {
						     
	/* really simple -- make a new array, copy the elements from the
	 * old array and then stick in the new one. */
	JFieldDeclaration[] returnFields = new JFieldDeclaration[originals.length+1];
	for (int i=0; i<originals.length; i++) {
	    returnFields[i] = originals[i];
	}
	/* now, stick in a new field declaration. */
	returnFields[originals.length]   = new JFieldDeclaration(null, newField, null, null);
	return returnFields;
    }

    /**
     * Make an init function which assigns allocates space for the various fields
     * (frequency weight fields are of size filterSize, and partial result fields are
     * of size x-1) and 
     * calculates the DFT of the impulse response and stores it in the weight fields.
     **/
    public JMethodDeclaration makeNewInit(LinearFilterRepresentation linearRep,
					  JVariableDefinition[] weightFields,
					  JVariableDefinition[] partialFields,
					  JVariableDefinition inputBufferField,
					  JVariableDefinition tempBufferField,
					  JVariableDefinition[] outputBufferFields,
					  int filterSize, int x) {
	
	JBlock body = new JBlock();

	// the number of FIR filters that are embodied by this filter
	int numFIRs = weightFields.length;
	
	/** add in statements to allocate space for the weight fields, partial results
	    fields, input buffer field, and output buffer field.
	    The weight fields are all of the filter size (because we will store the frequency
	    representation in half complex array form (the output of FFTW). **/
	for (int i=0; i<numFIRs; i++) {
	    body.addStatement(makeFieldAllocation(weightFields[i].getIdent(), filterSize,
						  "field to store the frequency response (" +
						  i + ") of the filter."));
	}
	for (int i=0; i<numFIRs; i++) {
	    body.addStatement(makeFieldAllocation(partialFields[i].getIdent(), x-1,
						  "field to store the partial results (" +
						  i + ") across filter firings."));
	}
	
	/** allocate space for the input data on each execution. **/
	body.addStatement(makeFieldAllocation(inputBufferField.getIdent(), filterSize,
					      "field to store the input each execution."));
	/** allocate space for the temp data on each execution (eg Y[k]). **/
	body.addStatement(makeFieldAllocation(tempBufferField.getIdent(), filterSize,
					      "field to store the temp product on each execution."));

	/** Allocate space for each of the output buffers. **/
	for (int i=0; i<numFIRs; i++) {
	    body.addStatement(makeFieldAllocation(outputBufferFields[i].getIdent(), filterSize,
						  "field to store the output of each execution (" +
						  i + ")."));
	}
	    
	/** copy the values of the time responses into the weight field (afterwards we will
	    run convert_to_freq on the buffer to convert the real values to their (complex)
	    frequency form.) **/
	for (int currentFIR=0; currentFIR<numFIRs; currentFIR++) {
	    /* Get the weights of the current column that we are looking at into a float array. */
	    float[] time_response_r = getRealArray(linearRep, numFIRs - 1 - currentFIR, filterSize);
	    // now, generate statements to assign the weights one by one to the weight field.
	    for (int i=0; i<filterSize; i++) {
		body.addStatement(makeArrayAssignment(weightFields[currentFIR],/* field */
						      i,                       /* index */
						      time_response_r[i]));    /* value */
	    }
	    /** add in code to convert the weight field to the frequency domain. **/
	    body.addStatement(makeTimeToFrequencyConversion(weightFields[currentFIR], filterSize));
	    /** add in code to scale the weight field by 1/N (because FFTW doesn't do it. */
	    body.addStatement(makeFrequencyScale(weightFields[currentFIR], filterSize));
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

    /** An assignment to convert a field to frequency, via an external callout. **/
    public JStatement makeTimeToFrequencyConversion(JVariableDefinition fieldToConvert,
						    int filterSize) {
	// now, add in a callout to convert_to_freq to transform the real weights into
	// their complex form.
	// prep the args
	JExpression[] externalArgs = new JExpression[2];
	externalArgs[0] = new JFieldAccessExpression(null, new JThisExpression(null),
						     fieldToConvert.getIdent());
	externalArgs[1] = new JIntLiteral(filterSize);
	JExpression externalCall = new JMethodCallExpression(null,               /* token reference*/
							     null,               /* prefix */
							     TO_FREQ_EXTERNAL,   /* ident */
							     externalArgs);      /* args */
	JavaStyleComment[] comment = makeComment("callout to " + TO_FREQ_EXTERNAL +
						 " to convert from time to freq. "); 
	return new JExpressionStatement(null,externalCall,comment);           
    }

    /** An assignment to scale each element in an array by 1/size via a callout. **/
    public JStatement makeFrequencyScale(JVariableDefinition fieldToScale,
					 int filterSize) {
	JExpression[] externalArgs = new JExpression[2];
	externalArgs[0] = new JFieldAccessExpression(null, new JThisExpression(null),
						     fieldToScale.getIdent());
	externalArgs[1] = new JIntLiteral(filterSize);
	JExpression externalCall = new JMethodCallExpression(null,           /* token reference*/
							     null,           /* prefix */
							     SCALE_EXTERNAL, /* ident */
							     externalArgs);  /* args */
	JavaStyleComment[] comment = makeComment("callout to " + SCALE_EXTERNAL +
						 " (scales buffer by 1/" + filterSize + ")."); 
	return new JExpressionStatement(null,externalCall,comment);           
    }



    
    /**
     * Create an array allocation expression. Allocates a one dimensional array of floats
     * for the field of name fieldName of fieldSize.
     **/
    public JStatement makeFieldAllocation(String fieldName, int fieldSize, String commentString) {
	JExpression fieldExpr = new JFieldAccessExpression(null, new JThisExpression(null), fieldName);
	JExpression fieldAssign = new JAssignmentExpression(null, fieldExpr, getNewArrayExpression(fieldSize));
	JavaStyleComment[] comment = makeComment(commentString); 
	return new JExpressionStatement(null, fieldAssign, comment);
    }

    
    /** Creates an assignment expression of the form: this.f[index]=value; **/
    public JStatement makeArrayAssignment(JLocalVariable field, int index, float value) {
	return makeArrayAssignment(field, index, new JFloatLiteral(null, value));
    }
    
    /** Creates an assignment expression of the form: this.f[index]=rhs; **/
    public JStatement makeArrayAssignment(JLocalVariable field, int index, JExpression assignedValue) {
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
	/* now, make the assignment expression */
	JAssignmentExpression assignExpr;
	assignExpr = new JAssignmentExpression(null,          /* token reference */
					       arrAccessExpr, /* lhs */
					       assignedValue); /* rhs */
	/* return an expression statement */
	return new JExpressionStatement(null, assignExpr, new JavaStyleComment[0]);
    }

    /** Creates an assignment expression of the form: f[index]=value; **/
    public JStatement makeLocalArrayAssignment(JLocalVariable var, int index, float value) {
	/* make the local variable expression to access var */
	JLocalVariableExpression varExpr = new JLocalVariableExpression(null, var);
	/* make the field access expression (eg this.field)*/
	JArrayAccessExpression arrAccessExpr;
	arrAccessExpr = new JArrayAccessExpression(null,                    /* token reference */
						   varExpr,                 /* prefix */
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
     * We make a function that copies N elements from the input
     * tape into a local array, calls the library function with the local array
     * and the weight field to calcluate the produce of the DFT of the input and the weight fields.
     * If we are making the initWork function, then just the middle N elements of the result are pushed
     * and if we are making the work function then the x-1 partials are added to the first x-1 elements
     * of the results and we push out the first N+x-1 elements. For both types of work we
     * then save the last x-1 elements of the DFT output in
     * the partial results fields for the next execution. Note that filterSize = N + 2(x-1).
     */
    public JMethodDeclaration makeNewWork(int functionType, /* either INITWORK or WORK */
					  JVariableDefinition[] weightFields,
					  JVariableDefinition[] partialFields,
					  JVariableDefinition inputBufferField,
					  JVariableDefinition tempBufferField,
					  JVariableDefinition[] outputBufferFields,
					  int filterSize, int x, int N) {
	// parameter check
	if ((functionType != INITWORK) && (functionType != WORK)) {
	    throw new IllegalArgumentException("function type must be either WORK or INITWORK");
	}

	/* the body of the new work function */
	JBlock body = new JBlock();


	/* first, copy the data from the input tape into the input buffer, element by element */
	for (int i=0; i<(N+x-1); i++) {
	    SIRPeekExpression currentPeekExpr   = new SIRPeekExpression(new JIntLiteral(i), CStdType.Float);
	    /* note that currentAssignExpr is buff[i] = peek(i) */
	    body.addStatement(makeArrayAssignment(inputBufferField, i, currentPeekExpr));
	}
	
	/* add statements to set the rest of the input buffer (eg pad with zeros). */
	for (int i=(N+x-1); i<(N+2*(x-1)); i++) {
	    body.addStatement(makeArrayAssignment(inputBufferField, i, 0.0f));
	}

	// now, convert the input buffer to frequency (we only have to do this for each set of inputs once)
	body.addStatement(makeTimeToFrequencyConversion(inputBufferField, filterSize));
		
	// now, this is where things get complicated by the fact that we could have
	// multiple FIRs going on in a single filter, with their outputs multiplexed
	// together.
	int numFIRs = weightFields.length;
	
	// make a callout to the complex multiplication routine for each set of
	// filter weights that we have. The function prototype is:
	// static void do_halfcomplex_multiply(float *Y, float *X, float *H, int size);
	for (int i=0; i<numFIRs; i++) {
	    JExpression[] externalArgs = new JExpression[4];
	    externalArgs[0] = new JFieldAccessExpression(null, new JThisExpression(null),
							 tempBufferField.getIdent());
	    externalArgs[1] = new JFieldAccessExpression(null, new JThisExpression(null),
							 inputBufferField.getIdent());
	    externalArgs[2] = new JFieldAccessExpression(null, new JThisExpression(null),
							 weightFields[i].getIdent());
	    externalArgs[3] = new JIntLiteral(filterSize);
	    JExpression externalCall = new JMethodCallExpression(null,           /* token reference */
								 null,               /* prefix */
								 MULTIPLY_EXTERNAL, /* ident */
								 externalArgs);      /* args */
	    JavaStyleComment[] externalComment = makeComment("callout to " + MULTIPLY_EXTERNAL +
							     " to calculate Y[k] = X[k].*H[k](" +
							     i + ")."); 
	    body.addStatement(new JExpressionStatement(null,externalCall,externalComment)); /* comments */
	    
	    // make a callout to the inverse transform routine to convert from Y[k]
	    // to y[n] for each of the FIR filters represented in this linear form
	    // prototype: void convert_from_freq(float* input_buff, float* output_buff, int size)
	    externalArgs = new JExpression[3];
	    externalArgs[0] = new JFieldAccessExpression(null, new JThisExpression(null),
							 tempBufferField.getIdent());
	    externalArgs[1] = new JFieldAccessExpression(null, new JThisExpression(null),
							 outputBufferFields[i].getIdent());
	    externalArgs[2] = new JIntLiteral(filterSize);
	    externalCall = new JMethodCallExpression(null,           /* token reference */
						     null,               /* prefix */
						     FROM_FREQ_EXTERNAL, /* ident */
						     externalArgs);      /* args */
	    externalComment = makeComment("callout to " + FROM_FREQ_EXTERNAL +
					  " to calculate y[n] = IFFT(Y[k]) (" +
					  i + ")"); 
	    body.addStatement(new JExpressionStatement(null,externalCall,externalComment));
	}
	
	
	/* if we are in the normal work function, push out the first x-1
	   values from the local buffer added to the partial results for each
	   * of the output buffers, interleaved.*/
	if (functionType == WORK) {
	    for (int currentOutput=0; currentOutput<(x-1); currentOutput++) {
		// for each of the output buffers
		for (int currentFIR=0; currentFIR<numFIRs; currentFIR++) {
		    body.addStatement(makeArrayAddAndPushStatement(partialFields[currentFIR],
								   outputBufferFields[currentFIR],
								   currentOutput));
		}
	    }
	}
	
	
	//now, put in code that will push the appropriate N values of inputBuffer[x-1 to N+(x-1)]
	//back on the tape
	for (int currentOutput=(x-1); currentOutput<(N+x-1); currentOutput++) {
	    for (int currentFIR = 0; currentFIR < numFIRs; currentFIR++) {
		body.addStatement(makeArrayPushStatement(outputBufferFields[currentFIR],
							 currentOutput));
	    }
	}
	
	/* now, copy the last x-1 values in the input buffer into the partial results buffer. */
	for (int currentOutput=0; currentOutput < x-1; currentOutput++) {
	    for (int currentFIR = 0; currentFIR < numFIRs; currentFIR++) {
		body.addStatement(makePartialCopyExpression(partialFields[currentFIR],
							    currentOutput,
							    outputBufferFields[currentFIR],
							    (N+x-1)+currentOutput));
	    }
	}
	
	
	/* stick in the appropriate number (N+x-1) of pop calls */
	for (int i=0; i<(N+x-1); i++) {
	    body.addStatement(makePopStatement());
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


    /**
     * Makes a copy expression from one array field to another array
     * field of the form this.field1[index1] = this.field2[index2].
     **/
    public JStatement makePartialCopyExpression(JLocalVariable field1, int index1,
						JLocalVariable field2, int index2) {
	/** make this.field1[index1] expression. **/
	JExpression fieldAccessExpr1 = makeArrayFieldAccessExpr(field1, index1);
	/** make this.field2[index2] expression. **/
	JExpression fieldAccessExpr2 = makeArrayFieldAccessExpr(field2, index2);
	
	/* now make the assignment expression */
	JExpression assignExpr = new JAssignmentExpression(null, fieldAccessExpr1, fieldAccessExpr2);
	/* now write it all in an expression statement */
	return new JExpressionStatement(null, assignExpr, null);
	
    }

    /** makes a popFloat() statement. **/
    public JStatement makePopStatement() {
	return new JExpressionStatement(null, new SIRPopExpression(CStdType.Float), null);
    }

    /** makes an array push statement of the following form: push(this.arr1[index] + this.arr2[index]) **/
    public JStatement makeArrayAddAndPushStatement(JLocalVariable arr1, JLocalVariable arr2, int index) {

	/* first, make the this.arr1[index] expression. */
	JExpression fieldArrayAccessExpr1 = makeArrayFieldAccessExpr(arr1, index);
	
	/* first, make the this.arr2[index] expression. */
	JExpression fieldArrayAccessExpr2 = makeArrayFieldAccessExpr(arr2, index);
	
	/* make the add expression */
	JAddExpression addExpr = new JAddExpression(null, fieldArrayAccessExpr1, fieldArrayAccessExpr2);
	/* now make the push expression */
	SIRPushExpression pushExpr = new SIRPushExpression(addExpr, CStdType.Float);
	/* and return an expression statement */
	return new JExpressionStatement(null, pushExpr, null);
    }
       
    

    /* makes an array push statement of the following form: push(this.arr[index]) */
    public JStatement makeArrayPushStatement(JLocalVariable arr, int index) {
	/* first make the array access expression this.arr[index]. **/
	JExpression arrAccessExpr = makeArrayFieldAccessExpr(arr, index);
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

    /* makes a field array access expression of the form this.arrField[index] */
    public JExpression makeArrayFieldAccessExpr(JLocalVariable arrField, int index) {
	/* first, make the this.arr1[index] expression */
	JExpression fieldAccessExpr;
	fieldAccessExpr = new JFieldAccessExpression(null, new JThisExpression(null), arrField.getIdent());
	JExpression fieldArrayAccessExpr;
	fieldArrayAccessExpr = new JArrayAccessExpression(null, fieldAccessExpr, new JIntLiteral(index));
	
	return fieldArrayAccessExpr;
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


    /**
     * returns an array of floating point numbers that correspond
     * to the real part of the FIR filter's weights. Size is the size
     * of the array to allocate. This might need to be bigger than the
     * actual number of elements in the FIR response because we will
     * be taking and FFT of the data.
     *
     * Col represents which column of data we want to get the coefficients
     * from. This is the column index in the actual filter's matrix rep.
     **/
    public float[] getRealArray(LinearFilterRepresentation filterRep, int col, int size) {
	/* use the matrix that is hidden inside the filter representation */
	FilterMatrix source = filterRep.getA();
	if (source.getRows() > size) {
	    throw new RuntimeException("freq response size is too small for this filter's response.");
	}
	/* allocate the return array */
	float[] arr = new float[size];

	/* pull out the parts of the matrix, one by one */
	for (int i=0; i<source.getRows(); i++) {
	    ComplexNumber currentElement = source.getElement(i,col);
	    /* assign different piece depending on part parameter */
	    arr[i] = (float)currentElement.getReal();
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




