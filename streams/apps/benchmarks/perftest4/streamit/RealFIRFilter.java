import streamit.library.*;
import java.lang.Math.*;

/**
 * Class RealFIRFilter
 *
 * Implements an FIR Filter
 */

public class RealFIRFilter extends Filter {
    int inSampFreq;
    int numTaps;
    float taps[];
    float cutoff, gain;
    int decimation;
    
    public RealFIRFilter (int sampleFreq, int dec, float c, int t, float g)
    {
        super (sampleFreq, dec, c, t, g);
    }

    public void init(int sampleFreq, final int dec, float c, final int t, float g) {
        input = new Channel (Float.TYPE, dec, t);
        output = new Channel (Float.TYPE, 1);
	
	inSampFreq = sampleFreq;
	numTaps = t;
	cutoff = c;
	gain = g;
	decimation = dec;
	taps = new float[numTaps];
	buildFilter_real();
    }

    public void buildFilter_real() {
	float arg;
	float N = (float)numTaps;
	float M = N-1;
	float C_PI = (float)java.lang.Math.PI;
	int index;
	
	if (cutoff == 0.0) {
	    for (index=0;index < numTaps; index++) {
		taps[index] =  ((float)(gain*(0.54-0.46*((float)java.lang.Math.cos
						(2*C_PI*((float)index)/(M))))));
	    }
	}
	else {
	    arg = 2*C_PI*cutoff/((float)inSampFreq);
	    for (index=0;index < numTaps;index++) {
		if (((float)index)-(M/2.0) != 0){     
		    taps[index] =  ((float)
			(gain*(((float)java.lang.Math.sin(arg*(index-(M/2.0))))/
			      C_PI/(((float)index)-(M/2.0))*
			      (0.54-0.46*((float)java.lang.Math.cos(2.0*C_PI*
								    ((float)index)/M))))));
		}
	    }
	    if ( (((int)M)/2)*2 == (int)M ){ 
		taps[(int)M/2] =  gain*arg/C_PI;
	    }
	}
    }
        
    public void work() {
	int i;
  
	float sum = 0;
        for (i=0; i < decimation; i++) {
            sum += taps[i] * input.popFloat();
        }
	
	for (i = 0; i < numTaps - decimation; i++)
	    sum += taps[i + decimation] + input.peekFloat(i);

        output.pushFloat(sum);
    }
}
