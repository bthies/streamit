/**
 * modified the CoarseSerializedBeamFormer that was output by Bill's new makefile
 * so that the linear filters are wrapped in a pipeline.
 **/





import streamit.*;

public class CoarseSerializedBeamFormer extends StreamIt
{
    public static void main(String[] args) {
	CoarseSerializedBeamFormer b = new CoarseSerializedBeamFormer();
	b.run(args);
    }

    public void init()
    {
	final int numChannels           = 12; 
	final int numSamples            = 64; 
	final int numBeams              = 4; 
	final int numCoarseFilterTaps   = 64; 
	final int numFineFilterTaps     = 64; 
	 
	 
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
				    add(new Pipeline() {
					    public void init() {
						add(new Decimator(coarseDecimationRatio));
						add(new CoarseBeamFirFilter(numCoarseFilterTaps,
									    numPostDec1,
									    coarseDecimationRatio));
												
						add(new Decimator(fineDecimationRatio));
						add(new CoarseBeamFirFilter(numFineFilterTaps,
									    numPostDec2,
									    fineDecimationRatio));
					    }
					});


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
				    add (new Pipeline() {
					    public void init() {
						add(new Beamform(i, 
								 numChannels));
										     
						add(new CoarseBeamFirFilter(mfSize, 
									    numPostDec2,
									    1));
					    }
					});
				    add(new Magnitude());
				    				     
				    add(new Detector(i,
						     numPostDec2,
						     targetBeam,
						     targetSamplePostDec,
						     cfarThreshold));
				}
			    });
		    }
		    
		    setJoiner(ROUND_ROBIN());
                    
                    
		}
	    });
        
	add(new FloatPrinter());
        
    }
}

class InputGenerate extends Filter
{  

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
    {  

	curSample = 0;
	numberOfSamples = nSamples;
	holdsTarget = (tarBeam == i);
	targetSample = tarSample;
	myChannel = i;

	thresh = cfarThresh;
	 
	output = new Channel(Float.TYPE, 2);
    }

    public void work() {
	if( holdsTarget && (curSample == targetSample) ) {

	     
	    output.pushFloat((float)Math.sqrt(curSample*myChannel));
	    output.pushFloat((float)Math.sqrt(curSample*myChannel)+1);


	} else {

	     
	    output.pushFloat(-((float)Math.sqrt(curSample*myChannel)));
	    output.pushFloat(-((float)Math.sqrt(curSample*myChannel)+1));


	}

	 
	curSample++;

	if( curSample >= numberOfSamples )
	    {
		curSample = 0;
	    }
    }
}

 



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
{  

    float[] real_weight;
    float[] imag_weight;
    int numTaps;
    int inputLength;
    int decimationRatio;
    float[] realBuffer;
    float[] imagBuffer;
     
    int count;
     
    int pos;

    public BeamFirFilter(int nt, int inLength, int decRatio) {
	super(nt, inLength, decRatio);
    }

    public void init(int nt, int inLength, int decRatio)
    {  
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

	 




















	 
	 
	for (int j=0; j<nt; j++) {
	    int idx = j + 1;
	     
	    real_weight[j] = (float)(Math.sin(idx) / (idx));
	    imag_weight[j] = (float)(Math.cos(idx) / (idx));
	}


    }

    public void work()
    {  
	float real_curr = 0;
	float imag_curr = 0;
	int i;
	int modPos;

	 
	realBuffer[pos] = input.popFloat();
	 




	imagBuffer[pos] = input.popFloat();

	 
	modPos = pos;
	for (i = 0; i < numTaps; i++) {
	    real_curr += 
		realBuffer[modPos] * real_weight[i] + imagBuffer[modPos] * imag_weight[i];
	    imag_curr +=
		imagBuffer[modPos] * real_weight[i] + realBuffer[modPos] * imag_weight[i];
	     
	    if (modPos==0) { 
		modPos = numTaps;
	    }
	    modPos--;
	}
	
	 
	pos++;
	if (pos==numTaps) { 
	    pos = 0;
	}

	 
	output.pushFloat(real_curr);
	output.pushFloat(imag_curr);
	 





	 
	for (i = 2; i < 2*decimationRatio; i++) {
	    input.popFloat();
	}

	 
	count+=decimationRatio;

	 
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

class Decimator extends Filter {
    int decimationFactor;

    public Decimator(int decimationFactor) {
	super(decimationFactor);
    }

    public void init(int decimationFactor) {
	input = new Channel(Float.TYPE, 2*decimationFactor);
	output = new Channel(Float.TYPE, 2);
	this.decimationFactor = decimationFactor;
    }

    public void work() {
	output.pushFloat(input.popFloat());
	output.pushFloat(input.popFloat());
	for (int i=1; i<decimationFactor; i++) {
	    input.popFloat();
	    input.popFloat();
	}
    }
}

class CoarseBeamFirFilter extends Filter
{  

    float[] real_weight;
    float[] imag_weight;
    int numTaps;
    int inputLength;
    int decimationRatio;

    public CoarseBeamFirFilter(int nt, int inLength, int decRatio) {
	super(nt, inLength, decRatio);
    }

    public void init(int nt, int inLength, int decRatio)
    {  
	int i;
	numTaps = nt;
	inputLength = inLength;
	decimationRatio = decRatio;

	input = new Channel(Float.TYPE, 2*inLength);
	output = new Channel(Float.TYPE, 2*inLength);
	real_weight = new float[numTaps];
	imag_weight = new float[numTaps];


	 
	 
	for (int j=0; j<nt; j++) {
	    int idx = j + 1;
	     
	    real_weight[j] = (float)(Math.sin(idx) / (idx));
	    imag_weight[j] = (float)(Math.cos(idx) / (idx));
	}


	 
	 
    }

    public void work() {
	 
	int min;
	if (numTaps < inputLength) {
	    min = numTaps;
	} else {
	    min = inputLength;
	}
	for (int i=1; i<=min; i++) {
	    float real_curr = 0;
	    float imag_curr = 0;
	    for (int j=0; j<i; j++) {
		int realIndex = 2*(i-j-1);
		int imagIndex = realIndex+1;
		real_curr += real_weight[j] * input.peekFloat(realIndex) + imag_weight[j] * input.peekFloat(imagIndex);
		imag_curr += real_weight[j] * input.peekFloat(imagIndex) + imag_weight[j] * input.peekFloat(realIndex);
		 
		 
	    }
	     
	     
	    output.pushFloat(real_curr);
	    output.pushFloat(imag_curr);
	}

	 
	for (int i=0; i<inputLength-numTaps; i++) {
	     
	    input.popFloat();
	    input.popFloat();
	    float real_curr = 0;
	    float imag_curr = 0;
	    for (int j=0; j<numTaps; j++) {
		int realIndex = 2*(numTaps-j-1);
		int imagIndex = realIndex+1;
		 
		 
		real_curr += real_weight[j] * input.peekFloat(realIndex) + imag_weight[j] * input.peekFloat(imagIndex);
		imag_curr += real_weight[j] * input.peekFloat(imagIndex) + imag_weight[j] * input.peekFloat(realIndex);
	    }
	     
	     
	     
	     
	    output.pushFloat(real_curr);
	    output.pushFloat(imag_curr);
	}

	 
	 
	for (int i=0; i<min; i++) {
	    input.popFloat();
	    input.popFloat();
	}
    }
}

class Beamform extends Filter
{  

    float[] real_weight;
    float[] imag_weight;
    int numChannels;
    int myBeamId;

    public Beamform(int myBeam, int nc) {
	super(myBeam, nc);
    }

    public void init(int myBeam, int nc)
    {  
	numChannels = nc;
	myBeamId = myBeam;

	input = new Channel(Float.TYPE, 2*nc);
	output = new Channel(Float.TYPE, 2);
	real_weight = new float[numChannels];
	imag_weight = new float[numChannels];


	 
	 
	for (int j=0; j<nc; j++) {
	    int idx = j + 1;
	     
	    real_weight[j] = (float)(Math.sin(idx) / (myBeam+idx));
	    imag_weight[j] = (float)(Math.cos(idx) / (myBeam+idx));
	}


    }

    public void work()
    {  
	float real_curr = 0;
	float imag_curr = 0;
	int i;
	for(i=0; i<numChannels; i++) {
	    float real_pop = input.popFloat();
	     




	    float imag_pop = input.popFloat();
	     




	     
	    real_curr += 
		real_weight[i] * real_pop - imag_weight[i] * imag_pop;
	    imag_curr +=
		real_weight[i] * imag_pop + imag_weight[i] * real_pop;
	}
	output.pushFloat(real_curr);
	 




	output.pushFloat(imag_curr);
    }
}

class Magnitude extends Filter
{  

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

     


    private float mag(float real, float imag) {
	return (float)Math.sqrt(real*real + imag*imag);
    }
}

class Detector extends Filter
{  

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

	output = new Channel(Float.TYPE, 1);

    }

    public void work()
    {
	float inputVal = input.popFloat();
	float outputVal;
	if(holdsTarget && targetSample == curSample) {
		if( !(inputVal >= thresh) ) {
		    outputVal = 0;
		} else {
		    outputVal = myBeam;
		}
	} else {
		if( !(inputVal >= thresh) ) {
		    outputVal = 0;
		} else {
		    outputVal = -myBeam;
		}
	}


	outputVal = inputVal;


	output.pushFloat(outputVal);



	curSample++;

	if( curSample >= numSamples )
	    curSample = 0;
    }
}
