/**
 * This is a template for the beamformer.  It can be configured either
 * to have serial output (with serialized outputs from all parallel
 * printers) or not.
 */

import streamit.*;

public class TemplateBeamFormer extends StreamIt
{
    public static void main(String[] args) {
	TemplateBeamFormer b = new TemplateBeamFormer();
	b.run(args);
    }

    public void init()
    {
	final int numChannels           = 12;//48;
	final int numSamples            = 64;//4096;
	final int numBeams              = 4;//16;
	final int numCoarseFilterTaps   = 64;//
	final int numFineFilterTaps     = 64;//
	// under current implementation, decimation ratios must divide
	// numSamples!  (otherwise we pop off the end of a column.) --bft
	final int coarseDecimationRatio = 1;
	final int fineDecimationRatio   = 2;
	final int numSegments           = 1;
	final int numPostDec1           = numSamples/coarseDecimationRatio;
	final int numPostDec2           = numPostDec1/fineDecimationRatio;
	final int mfSize                = numSegments*numPostDec2;
	final int pulseSize             = numPostDec2/2;
	final int predecPulseSize       = pulseSize*
	    coarseDecimationRatio*fineDecimationRatio;
	final int targetBeam            = numBeams/4;
	final int targetSample          = numSamples/4;
	// targetSamplePostDec used to have a 1 added to it, but that
	// seemed to do the wrong thing --bft
	final int targetSamplePostDec   = targetSample/coarseDecimationRatio/fineDecimationRatio;
	final float dOverLambda         = 0.5f;
	final float cfarThreshold       = 0.95f * dOverLambda*numChannels 
	    * (0.5f*pulseSize);

	add(new SplitJoin() {
		public void init() {
		    int _i;
		    setSplitter(NULL());
		    for(_i=0; _i<numChannels; _i+=1) {
			final int i = _i;
			add(new Pipeline() {
				public void init() {
				    add(new InputGenerate(i,
							  numSamples,
							  targetBeam,
							  targetSample,
							  cfarThreshold));
#ifdef COARSE
				    add(new CoarseBeamFirFilter(numCoarseFilterTaps,
								numSamples,
								coarseDecimationRatio));
#endif
#ifndef COARSE
				    add(new BeamFirFilter(numCoarseFilterTaps,
							  numSamples,
							  coarseDecimationRatio));
#endif

#ifdef COARSE
				    add(new CoarseBeamFirFilter(numFineFilterTaps,
								numPostDec1,
								fineDecimationRatio));
#endif
#ifndef COARSE
				    add(new BeamFirFilter(numFineFilterTaps,
							  numPostDec1,
							  fineDecimationRatio));
#endif
				}
			    });
		    }
		    setJoiner(ROUND_ROBIN(2));
		}
	    });

	add(new SplitJoin() {
		public void init() {
		    int _i;
		    setSplitter(DUPLICATE());
		    for(_i=0; _i<numBeams; _i+=1) {
			final int i = _i;
			add (new Pipeline() {
				public void init() {
				    add(new Beamform(i, 
						     numChannels));
						    // Need to replace this fir with 
						    //fft -> elWiseMult -> ifft
				    add(new BeamFirFilter(mfSize, 
							  numPostDec2,
							  1));
				    add(new Magnitude());
				    // with a more sophisticated detector, we need
				    // someplace to store the data until we can find
				    // the targets...
				    add(new Detector(i,
						     numPostDec2,
						     targetBeam,
						     targetSamplePostDec,
						     cfarThreshold));
				}
			    });
		    }
		    #ifdef SERIALIZED
		    setJoiner(ROUND_ROBIN());
                    #endif
                    #ifndef SERIALIZED
		    setJoiner(NULL());
                    #endif
		}
	    });
        #ifdef SERIALIZED
	add(new FloatPrinter());
        #endif
    }
}

class InputGenerate extends Filter
{ // class InputGenerate

    int curSample;
    int numberOfSamples;
    boolean holdsTarget;
    int targetSample;
    int myChannel;
    float thresh;
    //    int i2;

    public InputGenerate(int i, int n, int t1, int t2, float c) {
	super(i, n, t1, t2, c);
    }

    public void init(int i,
		     int nSamples,
		     int tarBeam,
		     int tarSample,
		     float cfarThresh)
    { // InputGenerate::init()

	curSample = 0;
	numberOfSamples = nSamples;
	holdsTarget = (tarBeam == i);
	targetSample = tarSample;
	myChannel = i;

	thresh = cfarThresh;
	//	i2 = 0;
	output = new Channel(Float.TYPE, 2);
    }

    public void work()
    { // InputGenerate::work()
	if( holdsTarget && (curSample == targetSample) )
	    {
		// real
		output.pushFloat((float)Math.sqrt(thresh));
		// imag
		output.pushFloat(0);
	    }
	else
	    {
		// real
		output.pushFloat(0);
		// imag
		output.pushFloat(0);
	    }

	//	System.out.println(i2++);
	curSample++;

	if( curSample >= numberOfSamples )
	    {
		curSample = 0;
	    }
    }
}

/**
 * Just a temp. substitute for the fir filter until we get a
 * functional one.
 */
class OneToOne extends Filter {

    public OneToOne() {
	super();
    }

    public void init() {
	input = new Channel(Float.TYPE, 1);
	output = new Channel(Float.TYPE, 1);
    }

    public void work() {
	output.pushFloat(input.popFloat());
    }
}

class FloatPrinter extends Filter {
    public FloatPrinter() {
	super();
    }
    public void init() {
	input = new Channel(Float.TYPE, 1);
    }

    public void work() {
	System.out.println(input.popFloat());
    }
}

/**
 * This filter just outputs a stream of zeros.
 */
class ZeroSource extends Filter {

    public ZeroSource() {
	super();
    }

    public void init() {
	output = new Channel(Float.TYPE, 1);
    }

    public void work() {
	output.pushFloat(0);
    }
}

class DummySink extends Filter {
    public DummySink() {
	super();
    }

    public void init() {
	input = new Channel(Float.TYPE, 1);
    }

    public void work() {
	input.popFloat();
    }
}

class BeamFirFilter extends Filter
{ // class FirFilter...

    float[] real_weight;
    float[] imag_weight;
    int numTaps;
    int inputLength;
    int decimationRatio;
    float[] realBuffer;
    float[] imagBuffer;
    // number of items we've seen in relation to inputLength
    int count;
    // our current writing position into the buffers
    int pos;

    public BeamFirFilter(int nt, int inLength, int decRatio) {
	super(nt, inLength, decRatio);
    }

    public void init(int nt, int inLength, int decRatio)
    { // BeamFirFilter::init()
	int i;
	numTaps = nt;
	inputLength = inLength;
	decimationRatio = decRatio;

	input = new Channel(Float.TYPE, 2*decRatio);
	output = new Channel(Float.TYPE, 2);
	real_weight = new float[numTaps];
	imag_weight = new float[numTaps];
	realBuffer = new float[numTaps];
	imagBuffer = new float[numTaps];
	pos = 0;

	// Use identity weights for now...
	//  Later should become an input parameter to the FIR
	
	/* NOTE that these weights are (intentionally) backwards
	 * compared to some formulations of an FIR.  That is, if we
	 * could disregard the initial conditions and there were
	 * complex values on the the tape, then filter would compute
	 * something equivalent to:
	 *
	 *   peek(0) * weight[numTaps] + ... + peek(numTaps) * weight[0]
	 * 
	 *  this formulation makes sense when you visualize the FIR operation:
	 *
	 *   input stream                               weights
         *      in[4]
	 *      in[3]                                     /|\   <- the weights move
	 *      in[2]                                      |       UP during the 
	 *      in[1]                                    w[0]      course of execution
	 *      in[0]                                    w[1]
	 *        0    <- initial conditions             w[2]
	 *        0                                      w[3]
	 */
	real_weight[0] = 1.0f;
	imag_weight[0] = 0.0f;
	for(i = 1; i < numTaps; i ++) {
	    real_weight[i] = 0;
	    imag_weight[i] = 0;
	    realBuffer[i] = 0;
	    imagBuffer[i] = 0;
	}
    }

    public void work()
    { // BeamFirFilter::work()
	float real_curr = 0;
	float imag_curr = 0;
	int i;
	int modPos;

	// pop a new item into the buffer
	realBuffer[pos] = input.popFloat();
	/*
	  if (realBuffer[pos]>0) {
	  System.err.println("popping >0 with pos=" + pos + " in " + this);
	  }
	*/
	imagBuffer[pos] = input.popFloat();

	// calculate sum
	modPos = pos;
	for (i = 0; i < numTaps; i++) {
	    real_curr += 
		realBuffer[modPos] * real_weight[i] + imagBuffer[modPos] * imag_weight[i];
	    imag_curr +=
		imagBuffer[modPos] * real_weight[i] + realBuffer[modPos] * imag_weight[i];
	    // adjust position in this round of summing.
	    if (modPos==0) { 
		modPos = numTaps;
	    }
	    modPos--;
	}
	
	// increment sum
	pos++;
	if (pos==numTaps) { 
	    pos = 0;
	}

	// push output
	output.pushFloat(real_curr);
	output.pushFloat(imag_curr);
	/*
	  if (real_curr>0) {
	  System.err.println("pushing >0 with pos=" + pos + " in " + this);
	  }
	*/

	// decimate
	for (i = 2; i < 2*decimationRatio; i++) {
	    input.popFloat();
	}

	// increment count
	count+=decimationRatio;

	// when we reach inLength, reset
	if (count==inputLength) {
	    count = 0;
	    pos = 0;
	    for (i=0; i<numTaps; i++) {
		realBuffer[i] = 0;
		imagBuffer[i] = 0;
	    }
	}
    }
}

class CoarseBeamFirFilter extends Filter
{ // class FirFilter...

    float[] real_weight;
    float[] imag_weight;
    int numTaps;
    int inputLength;
    int decimationRatio;

    public CoarseBeamFirFilter(int nt, int inLength, int decRatio) {
	super(nt, inLength, decRatio);
    }

    public void init(int nt, int inLength, int decRatio)
    { // BeamFirFilter::init()
	int i;
	numTaps = nt;
	inputLength = inLength;
	decimationRatio = decRatio;

	input = new Channel(Float.TYPE, 2*inLength*decRatio);
	output = new Channel(Float.TYPE, 2*inLength);
	real_weight = new float[numTaps];
	imag_weight = new float[numTaps];

	// Use identity weights for now...
	//  Later should become an input parameter to the FIR

	// NOTE:  weights are as "backwards" as explained in beamfirfilter
	real_weight[0] = 1.0f;
	imag_weight[0] = 0.0f;
	for(i = 1; i < numTaps; i++) {
	    real_weight[i] = 0;
	    imag_weight[i] = 0;
	}
    }

    public void work() {
	// for first <numTaps>, only look at beginning items
	for (int i=1; i<numTaps; i++) {
	    float real_curr = 0;
	    float imag_curr = 0;
	    for (int j=0; j<i; j++) {
		int realIndex = 2*(i-j-1);
		int imagIndex = realIndex+1;
		real_curr += real_weight[j] * input.peekFloat(realIndex) + imag_weight[j] * input.peekFloat(imagIndex);
		imag_curr += real_weight[j] * input.peekFloat(imagIndex) + imag_weight[j] * input.peekFloat(realIndex);
		//System.err.println("  real += wr[" + j + "] * peek("+realIndex+") + wi["+j+"] * peek("+imagIndex+")");
		//System.err.println("  imag += wr[" + j + "] * peek("+imagIndex+") + wi["+j+"] * peek("+realIndex+")");
	    }
	    //System.err.println("PUSH real");
	    //System.err.println("PUSH imag");
	    output.pushFloat(real_curr);
	    output.pushFloat(imag_curr);
	    // do decimation
	    for (int k=1; k<decimationRatio; k++) {
		input.popFloat();
		input.popFloat();
	    }
	}

	// then look at <numTaps> items
	for (int i=0; i<inputLength-numTaps+1; i++) {
	    float real_curr = 0;
	    float imag_curr = 0;
	    for (int j=0; j<numTaps; j++) {
		int realIndex = 2*(numTaps-j-1);
		int imagIndex = realIndex+1;
		//System.err.println("  real += wr[" + j + "] * peek("+realIndex+") + wi["+j+"] * peek("+imagIndex+")");
		//System.err.println("  imag += wr[" + j + "] * peek("+imagIndex+") + wi["+j+"] * peek("+realIndex+")");
		real_curr += real_weight[j] * input.peekFloat(realIndex) + imag_weight[j] * input.peekFloat(imagIndex);
		imag_curr += real_weight[j] * input.peekFloat(imagIndex) + imag_weight[j] * input.peekFloat(realIndex);
	    }
	    //System.err.println("PUSH real");
	    //System.err.println("PUSH imag");
	    //System.err.println("POP");
	    //System.err.println("POP");
	    output.pushFloat(real_curr);
	    output.pushFloat(imag_curr);
	    // do popping and decimation
	    for (int k=0; k<decimationRatio; k++) {
		input.popFloat();
		input.popFloat();
	    }
	}

	// pop extra <numTaps-1> items (that are still on the tape
	// from our last filtering)
	for (int i=0; i<(numTaps-1); i++) {
	    input.popFloat();
	    input.popFloat();
	}
    }
}

class Beamform extends Filter
{ // class Beamform...

    float[] real_weight;
    float[] imag_weight;
    int numChannels;
    int myBeamId;

    public Beamform(int myBeam, int nc) {
	super(myBeam, nc);
    }

    public void init(int myBeam, int nc)
    { // BeamCalc::init()
	int i;
	numChannels = nc;
	myBeamId = myBeam;

	input = new Channel(Float.TYPE, 2*nc);
	output = new Channel(Float.TYPE, 2);
	real_weight = new float[numChannels];
	imag_weight = new float[numChannels];

	// For now, use identity weights.
	for(i = 0; i < numChannels; i++)
	    {
		real_weight[i] = 0; 
		imag_weight[i] = 0; 
		if( i == myBeamId )
		    {
			real_weight[i] = 1;
			imag_weight[i] = 0;
		    }
	    }
    }

    public void work()
    { // BeamCalc::work()
	float real_curr = 0;
	float imag_curr = 0;
	int i;
	for(i=0; i<numChannels; i++) {
	    float real_pop = input.popFloat();
	    /*
	      if(real_pop>0) {
	      System.err.println("popping > 0 in " + this);
	      }
	    */
	    float imag_pop = input.popFloat();
	    /*
	      if(imag_pop>0) {
	      System.err.println("popping imag > 0 in " + this);
	      }
	    */
	    // Need to check this boundary cond
	    real_curr += 
		real_weight[i] * real_pop - imag_weight[i] * imag_pop;
	    imag_curr +=
		real_weight[i] * imag_pop + imag_weight[i] * real_pop;
	}
	output.pushFloat(real_curr);
	/*
	  if (real_curr>0) {
	  System.err.println("pushing >0 in " + this);
	  }
	*/
	output.pushFloat(imag_curr);
    }
}

class Magnitude extends Filter
{ // class Magnitude...

    public void init()
    {
	input = new Channel(Float.TYPE, 2);
	output = new Channel(Float.TYPE, 1);
    }

    public void work()
    { 
	float f1 = input.popFloat();
	float f2 = input.popFloat();
	output.pushFloat(mag(f1, f2));
    }

    /**
     * Return magnitude of (<real>, <imag>)
     */
    private float mag(float real, float imag) {
	return (float)Math.sqrt(real*real + imag*imag);
    }
}

class Detector extends Filter
{ // class Detector...

    int curSample;
    int myBeam;
    int numSamples;
    float thresh;
    int targetSample;
    boolean holdsTarget;

    public Detector(int i,
		    int nSamples,
		    int targetBeam,
		    int tarSample,
		    float cfarThreshold) {
	super(i, nSamples, targetBeam, tarSample, cfarThreshold);
    }

    public void init(int i,
		     int nSamples,
		     int targetBeam,
		     int tarSample,
		     float cfarThreshold)
    {
	curSample = 0;
	myBeam = i;
	numSamples = nSamples;
	holdsTarget = (myBeam == targetBeam);
	targetSample = tarSample;
	myBeam++;

	thresh = 0.1f;
	input = new Channel(Float.TYPE, 1);
#ifdef SERIALIZED
	output = new Channel(Float.TYPE, 1);
#endif
    }

    public void work()
    {
	float val = input.popFloat();
	if(holdsTarget && targetSample == curSample)
	    {
		if( !(val >= thresh) ) {
#ifdef SERIALIZED
		    output.pushFloat(0);
#endif
#ifndef SERIALIZED
		    System.out.println(0);
#endif
		} else {
#ifdef SERIALIZED
		    output.pushFloat(myBeam);
#endif
#ifndef SERIALIZED
		    System.out.println(myBeam);
#endif
		}
	    }
	else
	    {
		if( val >= thresh ) {
#ifdef SERIALIZED
		    output.pushFloat(0);
#endif
#ifndef SERIALIZED
		    System.out.println(0);
#endif
		} else {
#ifdef SERIALIZED
		    output.pushFloat(-myBeam);
#endif
#ifndef SERIALIZED
		    System.out.println(-myBeam);
#endif
		}
	    }

	curSample++;

	if( curSample >= numSamples )
	    curSample = 0;
    }
}
