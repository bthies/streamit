import streamit.library.*;
class TestSource extends Filter{
    public void init() {
	output = new Channel(Float.TYPE, 1650);
    }
    public void work() {
	int i;
	for (i = 0; i < 1650; i++) 
	    output.pushFloat(0);
    }
}
	    
class NullSink extends Filter {
    public void init() {
	input = new Channel(Short.TYPE, 1);
    }
    public void work() {
	//System.out.println(input.popShort());
	input.popShort();
    }
}

class PerftestPipeline extends Pipeline {
    public PerftestPipeline(final float center_freq) {
	super(center_freq);
    }
    
    public void init(final float center_freq) {
	add(new ComplexFIRFilter(33000000, 825, 400, center_freq, 2));
	add(new QuadratureDemod(5, 0));
	add(new RealFIRFilter(8000, 5, 4000, 20, 1));
    }
}


class PerftestSplitJoin extends Pipeline {
    public PerftestSplitJoin()
    {
        super();
    }

    public void init() {
	add(new ComplexFIRFilterFuse(33000000, 825, 400, 10370400, 10355400, 10340400,10960500,2));
	   
	add(new QuadratureDemodFuse(5, 0));
		
	add(new SplitJoin() {
		public void init() {
		    setSplitter(ROUND_ROBIN());
		    add(new RealFIRFilter(8000, 5, 4000, 20, 1));
		    add(new RealFIRFilter(8000, 5, 4000, 20, 1));
		    add(new RealFIRFilter(8000, 5, 4000, 20, 1));
		    add(new RealFIRFilter(8000, 5, 4000, 20, 1));
		    setJoiner(ROUND_ROBIN());
		}
	    });
    }
}

public class perftest4_fuse extends StreamIt
{
    
    static public void main(String[] t)
    {
        perftest4_inout test = new perftest4_inout();
        test.run(t);
    }
    
    public void init() {
	
	add(new TestSource());
	add(new PerftestSplitJoin());
	add(new NullSink());
    }
}	


class ComplexFIRFilterFuse extends Filter {
    int inSampFreq;
    int numtaps;
    int decimation;
    float phase_correctionReal1, phase_correctionImag1;
    float phase_correctionReal2, phase_correctionImag2;
    float phase_correctionReal3, phase_correctionImag3;
    float phase_correctionReal4, phase_correctionImag4;
    float phase_corr_incrReal1, phase_corr_incrImag1;
    float phase_corr_incrReal2, phase_corr_incrImag2;
    float phase_corr_incrReal3, phase_corr_incrImag3;
    float phase_corr_incrReal4, phase_corr_incrImag4;
    float gain;
    float[] tapsReal1;
    float[] tapsImag1;
    float[] tapsReal2;
    float[] tapsImag2;
    float[] tapsReal3;
    float[] tapsImag3;
    float[] tapsReal4;
    float[] tapsImag4;
    float[] inputArray;
    float center_freq1, center_freq2, center_freq3, center_freq4;
    
    public ComplexFIRFilterFuse (int sampFreq, int dec, int t,
			     float freq1, float freq2, float freq3, float freq4, float g)
    {
        super ();
	
    }

    public void init(int samplFreq, final int dec, int t,
		     float freq1, float freq2, float freq3, float freq4, float g) {
	
        input = new Channel (Float.TYPE, 4*dec, 4*dec);
        output = new Channel (Float.TYPE,8);
	
	inSampFreq = samplFreq;
	numtaps = t;
	gain = g;
	center_freq1 = freq1;
	center_freq2 = freq2;
	center_freq3 = freq3;
	center_freq4 = freq4;
	decimation = dec;
	
	tapsReal1 = new float[numtaps];
	tapsImag1 = new float[numtaps];
	tapsReal2 = new float[numtaps];
	tapsImag2 = new float[numtaps];
	tapsReal3 = new float[numtaps];
	tapsImag3 = new float[numtaps];
	tapsReal4 = new float[numtaps];
	tapsImag4 = new float[numtaps];

	inputArray = new float[numtaps];

	phase_corr_incrReal1 = 1;
	phase_corr_incrImag1 = 0;
	phase_corr_incrReal2 = 1;
	phase_corr_incrImag2 = 0;
	phase_corr_incrReal3 = 1;
	phase_corr_incrImag3 = 0;
	phase_corr_incrReal4 = 1;
	phase_corr_incrImag4 = 0;
	
	phase_correctionReal1 = 1;
	phase_correctionImag1 = 0;
	phase_correctionReal2 = 1;
	phase_correctionImag2 = 0;
	phase_correctionReal3 = 1;
	phase_correctionImag3 = 0;
	phase_correctionReal4 = 1;
	phase_correctionImag4 = 0;
	
	buildFilter_complex();
    }

    public void buildFilter_complex() {
	int index;
	float N = (float)numtaps;
	float M = N - 1;
	float C_PI = (float)Math.PI;

	///1
	if (center_freq1 == 0.0) {
	    for (index = 0; index < numtaps; index++) {
		tapsReal1[index] = (float)
		    (gain * (0.54-0.46*Math.cos(2*C_PI*index/(M))));
		tapsImag1[index] = 0;
	    }
	}
	else {
	    float arg = 2*C_PI*center_freq1/(float)inSampFreq;
	    for (index = 0; index < numtaps; index++) {
		tapsReal1[index] = (float)
		    (gain*Math.cos(arg*((float)index)*
				   (0.54-0.46*Math.cos(2*C_PI*index/(M)))));
		tapsImag1[index] = (float)(gain*(-1)*Math.sin(arg*index)*
					  (0.54-0.46*Math.cos(2*C_PI*index/(M))));
	    }
	    phase_corr_incrReal1 = (float)(Math.cos(arg*((float)decimation)));
	    phase_corr_incrImag1 = (float)((-1)*Math.sin(arg*(float)decimation));
	}

	////2
	if (center_freq2 == 0.0) {
	    for (index = 0; index < numtaps; index++) {
		tapsReal2[index] = (float)
		    (gain * (0.54-0.46*Math.cos(2*C_PI*index/(M))));
		tapsImag2[index] = 0;
	    }
	}
	else {
	    float arg = 2*C_PI*center_freq2/(float)inSampFreq;
	    for (index = 0; index < numtaps; index++) {
		tapsReal2[index] = (float)
		    (gain*Math.cos(arg*((float)index)*
				   (0.54-0.46*Math.cos(2*C_PI*index/(M)))));
		tapsImag2[index] = (float)(gain*(-1)*Math.sin(arg*index)*
					  (0.54-0.46*Math.cos(2*C_PI*index/(M))));
	    }
	    phase_corr_incrReal2 = (float)(Math.cos(arg*((float)decimation)));
	    phase_corr_incrImag2 = (float)((-1)*Math.sin(arg*(float)decimation));
	}
	///3
	if (center_freq3 == 0.0) {
	    for (index = 0; index < numtaps; index++) {
		tapsReal3[index] = (float)
		    (gain * (0.54-0.46*Math.cos(2*C_PI*index/(M))));
		tapsImag3[index] = 0;
	    }
	}
	else {
	    float arg = 2*C_PI*center_freq3/(float)inSampFreq;
	    for (index = 0; index < numtaps; index++) {
		tapsReal3[index] = (float)
		    (gain*Math.cos(arg*((float)index)*
				   (0.54-0.46*Math.cos(2*C_PI*index/(M)))));
		tapsImag3[index] = (float)(gain*(-1)*Math.sin(arg*index)*
					  (0.54-0.46*Math.cos(2*C_PI*index/(M))));
	    }
	    phase_corr_incrReal3 = (float)(Math.cos(arg*((float)decimation)));
	    phase_corr_incrImag3 = (float)((-1)*Math.sin(arg*(float)decimation));
	}
	///4
	if (center_freq4 == 0.0) {
	    for (index = 0; index < numtaps; index++) {
		tapsReal4[index] = (float)
		    (gain * (0.54-0.46*Math.cos(2*C_PI*index/(M))));
		tapsImag4[index] = 0;
	    }
	}
	else {
	    float arg = 2*C_PI*center_freq4/(float)inSampFreq;
	    for (index = 0; index < numtaps; index++) {
		tapsReal4[index] = (float)
		    (gain*Math.cos(arg*((float)index)*
				   (0.54-0.46*Math.cos(2*C_PI*index/(M)))));
		tapsImag4[index] = (float)(gain*(-1)*Math.sin(arg*index)*
					  (0.54-0.46*Math.cos(2*C_PI*index/(M))));
	    }
	    phase_corr_incrReal4 = (float)(Math.cos(arg*((float)decimation)));
	    phase_corr_incrImag4 = (float)((-1)*Math.sin(arg*(float)decimation));
	}


    }

    public void work() {
	float resultReal = 0;
	float resultImag = 0;
	int i, t;
 	///1
	for (t = 0; t < numtaps; t++) {
	    inputArray[t] = input.popFloat();
	}
	
	for (t = 0; t < (decimation - numtaps); t++) {
	  input.popFloat();
	}
	
	for(t = 0; t < numtaps; t++) {
		resultReal += tapsReal1[t] * inputArray[t];
		resultImag += tapsImag1[t];
	}
	
	if (center_freq1 != 0.0){
	   phase_correctionReal1 = (phase_correctionReal1 *  phase_corr_incrReal1) -
		(phase_correctionImag1 * phase_corr_incrImag1);
	    phase_correctionImag1 = (phase_correctionReal1 * phase_corr_incrImag1) -
		(phase_correctionImag1 * phase_corr_incrReal1);
	    
	    resultReal =  (resultReal *phase_correctionReal1) -
		(resultImag *				       
		phase_correctionImag1);
	    
	    resultImag =  (resultReal * phase_correctionImag1) -
		(resultImag * 
		 phase_correctionReal1);
	}
	
	output.pushFloat(resultReal);
	output.pushFloat(resultImag);


     	///2
	for (t = 0; t < numtaps; t++) {
	    inputArray[t] = input.popFloat();
	}
	
	for (t = 0; t < (decimation - numtaps); t++) {
	  input.popFloat();
	}
	
	for(t = 0; t < numtaps; t++) {
		resultReal += tapsReal2[t] * inputArray[t];
		resultImag += tapsImag2[t];
	}
	
	if (center_freq2 != 0.0){
	   phase_correctionReal2 = (phase_correctionReal2 *  phase_corr_incrReal2) -
		(phase_correctionImag2 * phase_corr_incrImag2);
	    phase_correctionImag2 = (phase_correctionReal2 * phase_corr_incrImag2) -
		(phase_correctionImag2 * phase_corr_incrReal2);
	    
	    resultReal =  (resultReal * phase_correctionReal2) -
		(resultImag *				       
		phase_correctionImag2);
	    
	    resultImag =  (resultReal * phase_correctionImag2) -
		(resultImag * 
		 phase_correctionReal2);
	}
	
	output.pushFloat(resultReal);
	output.pushFloat(resultImag);
  
 	///3
	for (t = 0; t < numtaps; t++) {
	    inputArray[t] = input.popFloat();
	}
	
	for (t = 0; t < (decimation - numtaps); t++) {
	  input.popFloat();
	}
	
	for(t = 0; t < numtaps; t++) {
		resultReal += tapsReal3[t] * inputArray[t];
		resultImag += tapsImag3[t];
	}
	
	if (center_freq3 != 0.0){
	   phase_correctionReal3 = (phase_correctionReal3 *  phase_corr_incrReal3) -
		(phase_correctionImag3 * phase_corr_incrImag3);
	    phase_correctionImag3 = (phase_correctionReal3 * phase_corr_incrImag3) -
		(phase_correctionImag3 * phase_corr_incrReal3);
	    
	    resultReal =  (resultReal *phase_correctionReal3) -
		(resultImag *				       
		phase_correctionImag3);
	    
	    resultImag =  (resultReal * phase_correctionImag3) -
		(resultImag * 
		 phase_correctionReal3);
	}
	
	output.pushFloat(resultReal);
	output.pushFloat(resultImag);  

 	///4
	for (t = 0; t < numtaps; t++) {
	    inputArray[t] = input.popFloat();
	}
	
	for (t = 0; t < (decimation - numtaps); t++) {
	  input.popFloat();
	}
	
	for(t = 0; t < numtaps; t++) {
		resultReal += tapsReal4[t] * inputArray[t];
		resultImag += tapsImag4[t];
	}
	
	if (center_freq4 != 0.0){
	   phase_correctionReal4 = (phase_correctionReal4 *  phase_corr_incrReal4) -
		(phase_correctionImag4 * phase_corr_incrImag4);
	    phase_correctionImag4 = (phase_correctionReal4 * phase_corr_incrImag4) -
		(phase_correctionImag4 * phase_corr_incrReal4);
	    
	    resultReal =  (resultReal * phase_correctionReal4) -
		(resultImag *				       
		phase_correctionImag4);
	    
	    resultImag =  (resultReal * phase_correctionImag4) -
		(resultImag * 
		 phase_correctionReal4);
	}
	
	output.pushFloat(resultReal);
	output.pushFloat(resultImag);
    }


}

class QuadratureDemodFuse extends Filter {
    int firing;
    float gain;

  
    public QuadratureDemodFuse (int firingRate, float g)
    {
        super ();
    }

    public void init(final int firingRate, float g) {
	final int peekAmount;
	if (2*firingRate > 4) peekAmount =2*firingRate;
	else peekAmount = 4;

        input = new Channel (Float.TYPE, 2*firingRate * 4, 2 * firingRate * 4/*peekAmount*/);
        output = new Channel (Float.TYPE, firingRate * 4);

	firing = firingRate;
	gain = g;
    }

    public void work() {
	float lastValReal, productReal, valReal;
	float lastValImag, productImag, valImag;
	int i;

	lastValReal = input.peekFloat(3);
	lastValImag = input.peekFloat(2);

	for (i = firing; i > 0; i--) {
	    valImag = input.popFloat();
	    valReal = input.popFloat();
	    
	    productReal = (valReal *lastValReal) - (valImag * lastValImag);
	    productImag = (valReal * (-lastValImag)) - (valImag *lastValReal);

	    lastValReal = valReal;
	    lastValImag = valImag;
	    
	    output.pushFloat
		(gain * (float)(Math.asin(productImag/
					  (Math.sqrt(Math.pow(productReal,2) + 
						     Math.pow(productImag,2))))));
	}


	lastValReal = input.peekFloat(3);
	lastValImag = input.peekFloat(2);

	for (i = firing; i > 0; i--) {
	    valImag = input.popFloat();
	    valReal = input.popFloat();
	    
	    productReal = (valReal *lastValReal) - (valImag * lastValImag);
	    productImag = (valReal * (-lastValImag)) - (valImag *lastValReal);

	    lastValReal = valReal;
	    lastValImag = valImag;
	    
	    output.pushFloat
		(gain * (float)(Math.asin(productImag/
					  (Math.sqrt(Math.pow(productReal,2) + 
						     Math.pow(productImag,2))))));
	}

	lastValReal = input.peekFloat(3);
	lastValImag = input.peekFloat(2);

	for (i = firing; i > 0; i--) {
	    valImag = input.popFloat();
	    valReal = input.popFloat();
	    
	    productReal = (valReal *lastValReal) - (valImag * lastValImag);
	    productImag = (valReal * (-lastValImag)) - (valImag *lastValReal);

	    lastValReal = valReal;
	    lastValImag = valImag;
	    
	    output.pushFloat
		(gain * (float)(Math.asin(productImag/
					  (Math.sqrt(Math.pow(productReal,2) + 
						     Math.pow(productImag,2))))));
	}

	lastValReal = input.peekFloat(3);
	lastValImag = input.peekFloat(2);

	for (i = firing; i > 0; i--) {
	    valImag = input.popFloat();
	    valReal = input.popFloat();
	    
	    productReal = (valReal *lastValReal) - (valImag * lastValImag);
	    productImag = (valReal * (-lastValImag)) - (valImag *lastValReal);

	    lastValReal = valReal;
	    lastValImag = valImag;
	    
	    output.pushFloat
		(gain * (float)(Math.asin(productImag/
					  (Math.sqrt(Math.pow(productReal,2) + 
						     Math.pow(productImag,2))))));
	}

    }
}
