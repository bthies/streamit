package at.dms.kjc.sir.linear.frequency;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;


/**
 * This class is the interface to all of the varous types of frequency replacers
 * that we have made. Depending on the option number, we run various different types of frequency
 * replacement:<p>
 *
 * 0 = stupid replacement<br>
 * 1 = smart replacement (reuse partial results) <br>
 * 2 = fftw replacement (use FFTW to calculate FFT, take advantage of real input/output symmetries)<br>
 * 3 = leet multidimensional fftw replacement (FFTW and converts any filter with pop=1)<br>
 **/
public abstract class FrequencyReplacer extends LinearReplacer implements Constants{
    public static final int UNKNOWN = -1;
    public static final int STUPID  =  0;
    public static final int SMARTER =  1;
    public static final int FFTW    =  2;
    public static final int LEET    =  3;
    // maintain this field to point to the best out of the above
    // options.  This will be used by default in some cases if no
    // frequency level is specified.
    public static final int BEST_AVAILABLE = 3;
    
    /**
     * start the process of replacement on str using the Linearity information in lfa.
     * targetSize is the targeted number of outputs to produce per steady state iteration
     * for each filter that is transformed using the frequency conversion. The actual number of
     * outputs produced will always be targetSize or greater (because the FFT we are doing only
     * operates on inputs that are powers of two long.<p>
     *
     * Note that the new frequency replacer determines the target size based on an empirical heuristic,
     * so that the target size option is no longer actually required.<p>
     *
     * (STUPID) 0 = stupid replacement<br>
     * (SMARTER)1 = smart replacement (reuse partial results) <br>
     * (FFTW)   2 = fftw replacement (use FFTW to calculate FFT, take advantage of real input/output symmetries<br>
     **/
    public static void doReplace(LinearAnalyzer lfa, SIRStream str, int replacementType) {
	LinearPrinter.println("Beginning frequency replacement(" + getName(replacementType) + ")...");

	// target size is always going to be 100
	int targetSize = 100;
	// make a new replacer based on replacementType;
	FrequencyReplacer replacer;
	if (replacementType == STUPID) {
	    replacer = new StupidFrequencyReplacer(lfa, targetSize);
	} else if (replacementType == SMARTER) {
	    replacer = new SmarterFrequencyReplacer(lfa, targetSize);
	} else if (replacementType == FFTW) {
	    replacer = new FFTWFrequencyReplacer(lfa);	    
	} else if (replacementType == LEET) {
	    replacer = new LEETFrequencyReplacer(lfa);	    
	} else {
	    throw new RuntimeException("Error -- invalid frequency replacement type: " +
				       replacementType + "valid range is 0->3");
	}
	
	// pump the replacer through the stream graph.
	IterFactory.createIter(str).accept(replacer);
    }

    /* Converts number to replacement name */
    public static String getName(int replacementType) {
	if (replacementType == STUPID) {
	    return "stupid";
	} else if (replacementType == SMARTER) {
	    return "smarter";
	} else if (replacementType == FFTW) {
	    return "fftw";
	} else if (replacementType == LEET) {
	    return "leet";
	} else {
	    return "unknown";
	}
    }	
    

}
