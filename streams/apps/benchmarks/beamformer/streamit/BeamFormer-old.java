/*
 * Early attempt at a beam former -- DARPA PCA DEMO 2
 *
 * Note that this app has no initialization, so it puts identity
 * values in for all operations. That is why the # of beams == # of channels.
 * This is a quick and dirty way to do auto-verification.
 *
 * For now, all complex's are passed over channels as a real followed
 * by an imaginary component.
 *
 *
 */

import streamit.*;

public class BeamFormer extends StreamIt
{
    static public void main(String[] t)
    {
	BeamFormer test = new BeamFormer();
	test.run(t);
    }

    public void init()
    {
	final int numChannels           = 4;
	final int numSamples            = 4096;
	final int numBeams              = 4;
	final int numCoarseFilterTaps   = 64;
	final int numFineFilterTaps     = 16;
	final int coarseDecimationRatio = 1;
	final int fineDecimationRatio   = 1;
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
	final float cfarThreshold       = 0.95f
	    *dOverLambda*numChannels*(0.5f*pulseSize);

	add(new SplitJoin() {
		public void init() {
		    int i;
		    setSplitter(NULL());
		    for(i=0; i<numChannels; i++) {
			final int final_i = i;
			this.add (new GeneratePipe(i,
						   numSamples,
						   targetBeam,
						   targetSample,
						   numCoarseFilterTaps,
						   coarseDecimationRatio,
						   numFineFilterTaps,
						   numPostDec1,
						   fineDecimationRatio,
						   cfarThreshold));
		    }
		    setJoiner(ROUND_ROBIN(2));
		}
	    });

	add(new SplitJoin() {
		public void init() {
		    int i;
		    setSplitter(DUPLICATE());
		    for(i=0; i<numBeams; i++) {
			this.add (new DetectPipe(i,
						 numChannels,
						 mfSize,
						 numPostDec2,
						 targetBeam,
						 targetSamplePostDec,
						 cfarThreshold));
		    }
		    setJoiner(NULL());
		}
	    });
    }
}

class GeneratePipe extends Pipeline {

    public GeneratePipe(int i,
			int numSamples,
			int targetBeam,
			int targetSample,
			int numCoarseFilterTaps,
			int coarseDecimationRatio,
			int numFineFilterTaps,
			int numPostDec1,
			int fineDecimationRatio,
			float cfarThreshold) {
	super(i, 
	      numSamples, 
	      targetBeam, 
	      targetSample,
	      numCoarseFilterTaps,
	      coarseDecimationRatio,
	      numFineFilterTaps,
	      numPostDec1,
	      fineDecimationRatio,
	      cfarThreshold);
    }

    public void init(int i,
		     int numSamples,
		     int targetBeam,
		     int targetSample,
		     int numCoarseFilterTaps,
		     int coarseDecimationRatio,
		     int numFineFilterTaps,
		     int numPostDec1,
		     int fineDecimationRatio,
		     float cfarThreshold) {
	this.add(new InputGenerate(i,
				   numSamples,
				   targetBeam,
				   targetSample,
				   cfarThreshold));
	this.add(new 
		 BeamFirFilter(numCoarseFilterTaps,
			       numSamples,
			       coarseDecimationRatio));
	this.add(new 
		 BeamFirFilter(numFineFilterTaps,
			       numPostDec1,
			       fineDecimationRatio));
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

	curSample++;

	if( curSample >= numberOfSamples )
	    {
		curSample = 0;
	    }
    }
}

class DetectPipe extends Pipeline {
    public DetectPipe(int i,
		      int numChannels,
		      int mfSize,
		      int numPostDec2,
		      int targetBeam,
		      int targetSamplePostDec,
		      float cfarThreshold) {
	super(i, numChannels, mfSize, numPostDec2, 
	      targetBeam, targetSamplePostDec, cfarThreshold);
    }

    public void init(int i, 
		     int numChannels,
		     int mfSize,
		     int numPostDec2,
		     int targetBeam,
		     int targetSamplePostDec,
		     float cfarThreshold) {
	this.add(new Beamform(i, 
			      numChannels));
	// Need to replace this fir with fft -> elWiseMult -> ifft
	this.add(new 
		 BeamFirFilter(mfSize, 
			       numPostDec2,
			       1));
	this.add(new Magnitude());
	// with a more sophisticated detector, we need someplace to store
	// the data until we can find the targets...
	this.add(new Detector(i, 
			      numPostDec2,
			      targetBeam,
			      targetSamplePostDec,
			      cfarThreshold));
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
/*
class BeamFirFilter extends Pipeline {

    public BeamFirFilter(int nt, int inLength, int decRatio) {
	super(nt, inLength, decRatio);
    }

    public void init(int nt, int inLength, int decRatio) {
	add(new BeamFirZeros(nt, inLength, decRatio));
	add(new BaseFirFilter(nt, inLength, decRatio));
	add(new BeamFirSink(nt, inLength, decRatio));
    }

}

class BeamFirZeros extends SplitJoin {
    
    public BeamFirZeros(int nt, int inLength, int decRatio) {
	super(nt, inLength, decRatio);
    }
    
    public void init(int nt, int inLength, int decRatio) {
	setSplitter(WEIGHTED_ROUND_ROBIN(0,1));
	add(new ZeroSource());
	add(new OneToOne());
	setJoiner(WEIGHTED_ROUND_ROBIN(2*(nt-1), 2*(inLength)));
    }
}

class BeamFirSink extends SplitJoin {

    public BeamFirSink(int nt, int inLength, int decRatio) {
	super(nt, inLength, decRatio);
    }
    
    public void init(int nt, int inLength, int decRatio) {
	setSplitter(WEIGHTED_ROUND_ROBIN(2*(inLength), 2*(nt-1)));
	add(new OneToOne());
	add(new DummySink());
	setJoiner(WEIGHTED_ROUND_ROBIN(1,0));
    }
}
*/
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
		realBuffer[modPos]*real_weight[i] + imagBuffer[modPos] * imag_weight[i];
	    imag_curr +=
		imagBuffer[modPos] * real_weight[i] + realBuffer[modPos] * imag_weight[i];
	    // increment position in this round of summing
	    modPos++;
	    if (modPos==numTaps) { modPos = 0; }
	}
	
	// increment sum
	pos = (pos+1)%numTaps;

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
	} else if (count>inputLength) {
	    System.out.println("ERROR:  dont expect count to exceed inputLength");
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
	
	thresh = 0.1f;
	input = new Channel(Float.TYPE, 1);
    }

    public void work()
    {
	float val = input.popFloat();
	if (val>=thresh) {
	    /*
	    System.err.println("something over threshold in detect: mybeam = " + myBeam + " curSample=" +curSample + " targetSample=" + targetSample + "holdstarget=" + holdsTarget);
	    */
	}
	if(holdsTarget && targetSample == curSample)
	    {
		if( !(val >= thresh) ) {
		    System.out.println("ERROR: Target not found in proper location on beam ");
		    System.out.println(myBeam);
		    System.out.println("; the value was ");
		    System.out.println(val);
		    System.out.println(" which is less than thresh ");
		    System.out.println(thresh);
		} else {
		    System.out.println("Found target.");
		}
	    }
	else
	    {
		if( val >= thresh ) {
		    System.out.println("ERROR: Target found in wrong location on beam " + myBeam);
		} else {
		    //System.out.println("OK not found on beam " + myBeam);
		}
	    }

	curSample++;

	if( curSample >= numSamples )
	    curSample = 0;
    }
}
