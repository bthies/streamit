package at.dms.kjc.sir.linear.frequency;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;


/**
 * Replaces linear filters of sufficient length with a conversion into the frequency
 * domain, multiplication, and convert the product back into the time domain.
 * In so doing, this also increases the peek, pop and push rates to take advantage of
 * the frequency transformation.<br>
 * 
 * $Id: LEETFrequencyReplacer.java,v 1.21 2003-05-30 14:51:54 aalamb Exp $
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
    

    /** Constant specifying we are making the work function. **/
    public static final int INITWORK = 1;
    /** Constant specifying we are making the init work. **/
    public static final int WORK = 2;

    /** The minimum size FIR (eg peek count) we will replace. There is no way that
     * a size one FIR will benefit from this transformation. **/
    public static final int minFIRSize = 2;
    
    /** We multiply the FIR size to get the target FFT size. "Good tradeoff between
     * execution time and storage space requirements. **/
    public static final int fftSizeFactor = 2;

    /** name of the loop variable to use **/
    public static final String LOOPVARNAME = "__freqIndex__";
    
    /** the linear analyzier which keeps mappings from filters-->linear representations. **/
    LinearAnalyzer linearityInformation;
    
    LEETFrequencyReplacer(LinearAnalyzer lfa) {
	if (lfa == null){
	    throw new IllegalArgumentException("Null linear filter analyzer!");
	}
	this.linearityInformation = lfa;
    }

    /**
     * Returns whether or not we can replace <str> with this replacer.
     * This ensures that our assumptions are met.
     */
    public static boolean canReplace(SIRStream str, LinearAnalyzer lfa) {
	/* if we don't have a linear form for this stream, we are done. */
	if(!lfa.hasLinearRepresentation(str)) {
	    LinearPrinter.println("  aborting -- not linear");
	    return false;
	}

	LinearFilterRepresentation linearRep = lfa.getLinearRepresentation(str);
	// for now, don't try converting to frequency inside any
	// feedback loop.  This is a conservative way to avoid making
	// scheduling feedbackloops impossible.
	//
	SIRContainer parent = str.getParent();
	while (parent!=null) {
	    if (parent instanceof SIRFeedbackLoop) {
		return false;
	    }
	    parent = parent.getParent();
	}

	/* if there is not a real valued FIR (all coefficients are real), we are done. */
	if (!linearRep.isPurelyReal()) {
	    LinearPrinter.println("  aborting -- filter has non real coefficients."); 
	    return false;
	}

	/** if this filter has a constant component (eg if it has a non zero "b") we abort.
	    (Note, there is nothing saying that we can't perform the transform in this
	    case, only that currently we don't have any examples of programs that use it
	    so I am not going to bother implenting it. The method would be to add the appropriate
	    elements of b into the output after returning to the time domain and before
	    we actually push the output values. -- AAL)**/
	if (linearRep.hasConstantComponent()) {
	    LinearPrinter.println("  aborting -- filter has non zero constant components.\n" +
				  "ANDREW -- YOU ARE BEING LAZY. THIS IS NOT A HARD THING TO IMPLEMENT " +
				  "SO DO IT!");
	    return false;
	}
	
	/** don't do small FIRs. **/
	if (linearRep.getPeekCount() < minFIRSize) {
 	    LinearPrinter.println("  aborting -- fir size too small: " +
				  linearRep.getPeekCount() + ". needs to be at least " +
				  minFIRSize);
	    return false;
	}

	// otherwise we can replace
	return true;
    }

    /**
     * Does the actual work of replacing something that computes a convolution
     * sum with something that does a FFT, multiply, and then IFFT.
     */
    public boolean makeReplacement(SIRStream self) {
	LinearPrinter.println(" processing " + self.getIdent());

	// make sure we can replace "self" with a frequency version.
	if (!canReplace(self, this.linearityInformation)) {
	    return false;
	}

	LinearFilterRepresentation linearRep = this.linearityInformation.getLinearRepresentation(self);
	/* now is when we get to the fun part, we have a linear representation
	 * and we want to replace it with an frequency version using the FFT.*/
	int x = linearRep.getPeekCount(); // x-1 is the overlap amount
	int N = calculateN(x);            // N is the non-overlap part
	int filterSize = N+2*(x-1);       // this is the overall peek of the new filter
	LinearPrinter.println("  creating frequency filter.\n" +
			      "   N+(x-1)=" + (N+x-1) + " (outputs per steady state)\n" + 
			      "   x=" + x + " (peek, original filter size)\n" +
			      "   filterSize=" + filterSize + " (FFT size)");
	

	/////////////// Create the new fields for this filter.
	int numWeightFields = linearRep.getPushCount(); // Eg the number of columns in the matrix
	JFieldDeclaration[] newFields = self.getFields();
	
	/* make the fields to hold the transform of the weights. Note that the
	   weights are stored in "half complex array" or hermetian array format. This
	   is the output format generated by FFTW. The first weight field corresponds to the weight field of
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
 
	/** make enough output buffers to hold the appropriate outputs. We can't reuse
	 * the output buffer fields because we need to interleave the values at the end. **/
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
	
	/* make a new init work function */
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
	SIRStream replacement;
	replacement = new SIRTwoStageFilter(self.getParent(),              /* parent */
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
											 filter outputs
											 N+x-1 */
					    freqWork,                      /* work */
					    N+x-1,                         /* initPeek */
					    N+x-1,                         /* initPop */
					    N*numWeightFields,             /* initPush */
					    freqInitWork,                  /* initWork */
					    self.getInputType(),           /* input type */
					    self.getOutputType());         /* output type */
	// need to explicitly set the init function (not part of the constructor)
	replacement.setInit(freqInit);
	
	// if we have a pop rate greater than one, then put a decimator next to <replacement>
	if (linearRep.getPopCount()>1) {
	    SIRPipeline wrapper = new SIRPipeline(self.getParent(), replacement.getIdent()+"_dec_wrap");
	    wrapper.setInit(SIRStream.makeEmptyInit());
	    wrapper.add(replacement);
	    wrapper.add(makeDecimatorForFrequencyNode(linearRep.getPushCount(),
						      linearRep.getPopCount(), replacement.getOutputType()));
	    // now remember this new wrapper as our replacement instead of the original
	    replacement = wrapper;
	}

	// do unrolling on the new filter.  (Unrolling is part of
	// filter lowering; will get field propagation and stuff, node
	// optimization, etc., as added bonus.)
	Flattener.lowerFilterContents(replacement, false);
	
	// now replace the current filter (self) with the frequency version
	self.getParent().replace(self, replacement);
	
	LinearPrinter.println("  done replacing.");
	
	// return true since we replaced something
	return true;
    }

    /**
     * Returns a filter that has this behavior:<br>
     * <pre>
     *   for (int i=0; i&lt;freqPush; i++) { push(pop()) }
     *   for (int i=0; i&lt;freqPush; i++) { for (int j=0;j&lt;freqPop-1; j++) { pop() } }
     * </pre> <br>
     *
     * Will return a filter with a null parent.  This is intended to
     * follow a frequency node that has a pop rate more than one; the
     * freqPush and freqPop refers to the push and pop rates of the
     * frequency node.
     */
    public static SIRFilter makeDecimatorForFrequencyNode(int freqPush, int freqPop, CType type) {
	// make work function
	// work function
	JStatement body[] = { Utils.makeForLoop(new JExpressionStatement(null, new SIRPushExpression(new SIRPopExpression(type), type), null), freqPush),
			      new JExpressionStatement(null, new SIRPopExpression(type, freqPush*(freqPop-1)), null) };
	
	JMethodDeclaration work =  new JMethodDeclaration( /* tokref     */ null,
							   /* modifiers  */ at.dms.kjc.
							   Constants.ACC_PUBLIC,
							   /* returntype */ CStdType.Void,
							   /* identifier */ "work",
							   /* parameters */ JFormalParameter.EMPTY,
							   /* exceptions */ CClassType.EMPTY,
							   /* body       */ new JBlock(null, body, null),
							   /* javadoc    */ null,
							   /* comments   */ null);

	// make filter
	SIRFilter result = new SIRFilter(null,
					 "__Decimator",
					 JFieldDeclaration.EMPTY(),
					 JMethodDeclaration.EMPTY(),
					 // peek, pop, push
					 new JIntLiteral(freqPop*freqPush),
					 new JIntLiteral(freqPop*freqPush),
					 new JIntLiteral(freqPush),
					 work,
					 type,
					 type);
	result.setInit(SIRStream.makeEmptyInit());
	return result;
    }

    /**
     * Create the field that we put the filter's frequency response in.
     * The field is an array of floats, and we will have one array for the real part of the
     * repsonse and one array for the imaginary part of the response.
     */
    public JVariableDefinition makeWeightField(String name) {
	return new JVariableDefinition(null, /* token reference */
				       ACC_FINAL, /* modifiers */
				       getArrayType(), /* type */
				       name, /* identity */
				       null); /* initializer */
    }
    


    /**
     * Make the init function. The init function allocates space for the various fields
     * (frequency weight fields are of size filterSize, and partial result fields are
     * of size x-1) and 
     * calculates the FFT of the impulse response and stores it in the weight fields.
     **/
    public JMethodDeclaration makeNewInit(LinearFilterRepresentation linearRep,
					  JVariableDefinition[] weightFields,
					  JVariableDefinition[] partialFields,
					  JVariableDefinition inputBufferField,
					  JVariableDefinition tempBufferField,
					  JVariableDefinition[] outputBufferFields,
					  int filterSize, int x) {
	
	JBlock body = new JBlock();

	// the number of FIR filters that are embodied by this filter (ie push count)
	int numFIRs = weightFields.length;
	
	/** add statements to allocate space for the weight fields, partial results
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
							     SCALE_EXTERNAL, /* ident */
							     externalArgs);  /* args */
	JavaStyleComment[] comment = makeComment("callout to " + SCALE_EXTERNAL +
						 " (scales buffer by 1/" + filterSize + ")."); 
	return new JExpressionStatement(null,externalCall,comment);           
    }



    

    
    /** Creates an assignment expression of the form: this.f[index]=value; **/
    public JStatement makeArrayAssignment(JLocalVariable field, int index, float value) {
	return makeArrayAssignment(field, new JIntLiteral(index), new JFloatLiteral(null, value),null);
    }


    /** Creates an assignment expression of the form: this.f[index]=rhs; **/
    public JStatement makeArrayAssignment(JLocalVariable field, JVariableDefinition index,
					  JExpression assignedValue, String comment) {
	return makeArrayAssignment(field, makeLocalVarExpression(index), assignedValue, comment);
    }
    
    /** Creates an assignment expression of the form: this.f[index]=rhs; **/
    public JStatement makeArrayAssignment(JLocalVariable field, JExpression index,
					  JExpression assignedValue, String comment) {
	/* make the field access expression (eg this.field)*/
	JFieldAccessExpression fldAccessExpr;
	fldAccessExpr = new JFieldAccessExpression(null, /* token reference */
						   new JThisExpression(null), /* base */
						   field.getIdent()); /* field name */
	/* make the array access expression (eg this.field[index]*/
	JArrayAccessExpression arrAccessExpr;
	arrAccessExpr = new JArrayAccessExpression(null,                    /* token reference */
						   fldAccessExpr,           /* prefix */
						   index,
						   field.getType()); /* accessor */
	/* now, make the assignment expression */
	JAssignmentExpression assignExpr;
	assignExpr = new JAssignmentExpression(null,          /* token reference */
					       arrAccessExpr, /* lhs */
					       assignedValue); /* rhs */
	/* return an expression statement */
	JavaStyleComment[] jscomment = new JavaStyleComment[0];
	if (comment != null) {jscomment = makeComment(comment);}
	return new JExpressionStatement(null, assignExpr, jscomment);
    }


    

    /**
     * Make both the initWork and the work function (because they are not very different).<br>
     *
     * We make a function that copies N+2(x-1) elements from the input
     * tape into a local array, calls the library function with the local array
     * and the weight field to calcluate the FFT of the input and the weight fields.
     * If we are making the initWork function, then just the middle N elements of the result are pushed
     * and if we are making the work function then the x-1 partials are added to the first x-1 elements
     * of the results and we push out the first N+x-1 elements. For both types of work we
     * then save the last x-1 elements of the DFT output in
     * the partial results fields for the next execution. Note that filterSize = N + 2(x-1).<br>
     *
     * If the GENFORLOOPS flag is set, then we will actually generate for loops in the
     * work function rather than generating unrolled code.<br>
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

	// loop index.
	JVariableDefinition loopVar = new JVariableDefinition(null,0,CStdType.Integer,
							      LOOPVARNAME, null);
	body.addStatement(new JVariableDeclarationStatement(null, loopVar,
							    makeComment("loop variable")));

	/* copy input to buffer */
	// for (i=0; i<(N+x-1); i++) { inputBuffer[i]=peek(i);}
	SIRPeekExpression currentPeekExpr = new SIRPeekExpression(makeLocalVarExpression(loopVar),
								  CStdType.Float);
	JStatement forBody = makeArrayAssignment(inputBufferField, loopVar,
						 currentPeekExpr, "copy input to buffer");
	body.addStatement(makeConstantForLoop(loopVar, 0, N+x-1, forBody));

	/* fill the rest of the input buffer with zeros */
	// for (i=(N+x-1); i<(N+2*(x-1)); i++) {inputBuffer[i] = 0;}
	forBody = makeArrayAssignment(inputBufferField, loopVar, new JFloatLiteral(0),"pad with zeros"); 
	body.addStatement(makeConstantForLoop(loopVar, (N+x-1), N+2*(x-1), forBody));
    	
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

	JBlock pushForBody = new JBlock();
	if (functionType == WORK) {
	    for (int currentFIR=0; currentFIR<numFIRs; currentFIR++) {
		JStatement currentPushStatement;
		currentPushStatement = makeArrayAddAndPushStatement(partialFields[currentFIR],
								    outputBufferFields[currentFIR],
								    loopVar);
		pushForBody.addStatement(currentPushStatement);
	    }
	    // generate a for loop which contains a statement like the following
	    // for each of the fields:
	    // for (i=0; i<x-1; i++) {
	    //  push(outputBuffer_1[i]);
	    //  ......
	    //  push(outputBuffer_x[i]); }
	    body.addStatement(makeConstantForLoop(loopVar, 0, x-1, pushForBody));
	}
	
	
	/* now, put in code that will push the appropriate N values of inputBuffer[x-1 to N+(x-1)]. */
	pushForBody = new JBlock();
	for (int currentFIR=0; currentFIR<numFIRs; currentFIR++) {
	    JStatement currentPushStatement;
	    currentPushStatement = makeArrayPushStatement(outputBufferFields[currentFIR],
							  loopVar);
	    pushForBody.addStatement(currentPushStatement);
	}
	// for(i=x-i; i<(N+x-1); i++) 
	//  push(outputBuffer_1[i]);
	//  ......
	//  push(outputBuffer_x[i]); }
	body.addStatement(makeConstantForLoop(loopVar, x-1, N+x-1, pushForBody));

	/* now, copy the last x-1 values in the input buffer into the partial results buffer. */
	pushForBody = new JBlock();
	for (int currentFIR=0; currentFIR<numFIRs; currentFIR++) {
	    JExpression indexOffsetExpr = new JAddExpression(null,
							     makeLocalVarExpression(loopVar),
							     new JIntLiteral(N+x-1));
	    JStatement copyStatement;
	    copyStatement = makePartialCopyExpression(partialFields[currentFIR],
						      makeLocalVarExpression(loopVar),
						      outputBufferFields[currentFIR],
						      indexOffsetExpr);
	    // copy statement: partial_i[loopVar] = partail_i[loopVar+(N+x-1)] */
	    pushForBody.addStatement(copyStatement);
	}
	// for(i=0; i<x-1; i++) 
	//  partial_1[i] = outputBuffer_1[i+N+(x-1)]
	//  ......
	//  partial_N[i] = outputBuffer_N[i+N+(x-1)]
	body.addStatement(makeConstantForLoop(loopVar, 0, x-1, pushForBody));

	
	/* stick in the appropriate number (N+x-1) of pop calls */
	makePopStatements(body, N+x-1);
	
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
    public JStatement makePartialCopyExpression(JLocalVariable field1, JExpression index1,
						JLocalVariable field2, JExpression index2) {
	/** make this.field1[index1] expression. **/
	JExpression fieldAccessExpr1 = makeArrayFieldAccessExpr(field1, index1);
	/** make this.field2[index2] expression. **/
	JExpression fieldAccessExpr2 = makeArrayFieldAccessExpr(field2, index2);
	
	/* now make the assignment expression */
	JExpression assignExpr = new JAssignmentExpression(null, fieldAccessExpr1, fieldAccessExpr2);
	/* now write it all in an expression statement */
	return new JExpressionStatement(null, assignExpr, null);
	
    }

    /** adds n popFloat() statements to the end of body. **/
    public void makePopStatements(JBlock body, int n) {
	// always put them in a loop exceeds the unroll count
	SIRPopExpression popExpr = new SIRPopExpression(CStdType.Float, n);
	// wrap the pop expression so it is a statement.
	JExpressionStatement popWrapper = new JExpressionStatement(null, popExpr, null);
	body.addStatement(popWrapper);
    }

    /**
     * makes an array push statement of the following form:
     * push(this.arr1[indexVar] + this.arr2[indexVar]);
     **/
    public JStatement makeArrayAddAndPushStatement(JLocalVariable arr1, JLocalVariable arr2,
						   JVariableDefinition indexVar) {

	JExpression index = makeLocalVarExpression(indexVar);
	    
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
       
    

    /* makes an array push statement of the following form: push(this.arr[indexVar]) */ 
    public JStatement makeArrayPushStatement(JLocalVariable arr, JVariableDefinition indexVar) {
	JExpression index = makeLocalVarExpression(indexVar);
	/* first make the array access expression this.arr[index]. **/
	JExpression arrAccessExpr = makeArrayFieldAccessExpr(arr, index);
	/* now make the push expression */
	SIRPushExpression pushExpr = new SIRPushExpression(arrAccessExpr, CStdType.Float);
	/* and return an expression statement */
	return new JExpressionStatement(null, pushExpr, null);
    }
	
    /**
     * Returns an array of floating point numbers that correspond
     * to the real part of the FIR filter's weights. Size is the size
     * of the array to allocate. This might need to be bigger than the
     * actual number of elements in the FIR response because we will
     * be taking and FFT of the data.<br>
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
     * Generates the a for loop of the form:<br>
     * <pre>
     * for(loopVar = initVal; loopVar&lt;maxVal; loopVar++) {loopBody}
     * </pre>
     **/
    public JStatement makeConstantForLoop(JVariableDefinition loopVar,
					  int initVal, int maxVal,
					  JStatement forBody) {
       JStatement  forInit = makeAssignmentStatement(makeLocalVarExpression(loopVar),
						     new JIntLiteral(initVal));  //i=initVal
	JExpression forCond = makeLessThanExpression(makeLocalVarExpression(loopVar),
						     new JIntLiteral(maxVal));  //i<maxVal
	JStatement  forIncr = makeIncrementStatement(makeLocalVarExpression(loopVar));  //i++
    	return new JForStatement(null, forInit, forCond, forIncr, forBody, null);
    }

    

    /**
     * calculates the appropriate size FFT to perform. It is passed x, the length
     * of the impuse response of the filter, and it returns the actual N, the
     * number of output points that will be produced by one execution of the
     * filter.<br>
     *
     * This implementation works by calculating a targetN that is a
     * multiple of x, and then scaling up to the next power of two.
     **/
    public static int calculateN(int x) {
	int targetNumberOfOutputs = fftSizeFactor * x; 
	LinearPrinter.println("  target output size: " + targetNumberOfOutputs);
	
	// we know N + 2(x-1) = 2^r
	// so we calculate r as floor(lg(N+2(x-1))) +1 where lg is log base 2
	
	// and then N = 2^r - 2(x-1)
	int arg = targetNumberOfOutputs + 2*(x-1);
	int r = (int)Math.floor(Math.log(arg)/Math.log(2)) + 1;
	// now, calculate N
	int N = (int)Math.pow(2,r) - 2*(x-1);
	return N;
    }
}
